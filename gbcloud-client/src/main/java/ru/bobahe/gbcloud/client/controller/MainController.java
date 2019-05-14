package ru.bobahe.gbcloud.client.controller;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.stage.WindowEvent;
import ru.bobahe.gbcloud.client.guiutils.GuiUtils;
import ru.bobahe.gbcloud.client.viewmodel.FileInfo;
import ru.bobahe.gbcloud.client.viewmodel.GlobalViewModel;

import java.io.File;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    private GlobalViewModel model = GlobalViewModel.getInstance();
    private StringProperty serverPath = model.getServerPath();
    private StringProperty clientPath = model.getClientPath();

    @FXML
    private StackPane root;

    @FXML
    private AnchorPane anchorPane;

    @FXML
    private GridPane clientGridPane;

    @FXML
    private TableView<FileInfo> clientFilesTable, serverFilesTable;

    @FXML
    private Label lblClientPath, lblServerPath;

    @FXML
    private Circle connected;

    @FXML
    private ContextMenu serverFilesMenu;

    @Override
    @SuppressWarnings("unchecked")
    public void initialize(URL location, ResourceBundle resources) {
        onCloseRequest();

        clientPath.setValue(File.separator);
        lblClientPath.textProperty().bind(clientPath);
        serverPath.setValue(File.separator);
        lblServerPath.textProperty().bind(serverPath);

        GuiUtils.prepareTableViews(clientFilesTable, serverFilesTable);

        model.getClientFileList();

        serverFilesMenu.getItems().forEach(mi -> mi.setDisable(!model.getIsConnected().get()));

        model.getMessageFromServer().addListener(this::messageFromServer);
        model.getIsAuthenticated().addListener(this::getAuthCommand);
    }

    private void onCloseRequest() {
        Platform.runLater(() -> {
            root.getScene().getWindow().setOnCloseRequest(event -> {
                if (event.getEventType() == WindowEvent.WINDOW_CLOSE_REQUEST) {
                    while (!GlobalViewModel.getInstance().getClient().close()) {
                    }
                    Platform.exit();
                }
            });
        });
    }

    private void getAuthCommand(Observable observable) {
        boolean isAuthOk = ((BooleanProperty) observable).get();
        serverFilesMenu.getItems().forEach(mi -> mi.setDisable(!isAuthOk));
        if (isAuthOk) {
            connected.setFill(Paint.valueOf("green"));
        }
    }

    private void messageFromServer(Observable observable) {
        if (((StringProperty) observable).get().length() == 0) {
            return;
        }

        Platform.runLater(() -> {
            Alert.AlertType type = Alert.AlertType.ERROR;
            String headerText = "Ошибка";

            if (model.getMessageFromServerType().get() == 0) {
                type = Alert.AlertType.INFORMATION;
                headerText = "Успех";
            }

            GuiUtils.showAlert(type, "Собщение от сервера", headerText, ((StringProperty) observable).get());
            ((StringProperty) observable).set("");
        });
    }

    public void clientFilesTableClick(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() > 1) {
            model.changeDir(clientFilesTable, clientPath, true);
        }
    }

    public void clientFilesTableKeyPressed(KeyEvent keyEvent) {
        switch (keyEvent.getCode()) {
            case ENTER:
                model.changeDir(clientFilesTable, clientPath, true);
                break;
            case F5:
                model.copyToServer(clientFilesTable, clientPath, serverPath);
                break;
            case F7:
                showNewDirectoryModal(true);
                break;
            case F8:
                try {
                    model.delete(true, clientFilesTable);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    public void serverFilesTableClick(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() > 1) {
            model.changeDir(serverFilesTable, serverPath, false);
        }
    }

    public void serverFilesTableKeyPressed(KeyEvent keyEvent) {
        switch (keyEvent.getCode()) {
            case ENTER:
                model.changeDir(serverFilesTable, serverPath, false);
                break;
            case F5:
                model.copyFromServer(serverFilesTable, clientPath, serverPath);
                break;
            case F7:
                showNewDirectoryModal(false);
                break;
            case F8:
                try {
                    model.delete(false, serverFilesTable);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    private void showNewDirectoryModal(boolean isClient) {
        // todo Разобраться с редактированием TableView
        Optional<String> result = GuiUtils.showNewFolderDialog();

        result.ifPresent(name -> {
            try {
                model.createDirectory(isClient, (isClient ? clientPath.get() : serverPath.get()) + result.get());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private boolean isClientContextMenu(ActionEvent actionEvent) {
        return ((MenuItem) actionEvent.getSource()).getParentPopup().getId().startsWith("client");
    }

    public void newDirectoryMenuItemClicked(ActionEvent actionEvent) {
        showNewDirectoryModal(isClientContextMenu(actionEvent));
    }

    public void deleteMenuItemClicked(ActionEvent actionEvent) {
        try {
            TableView<FileInfo> tw = serverFilesTable;
            if (isClientContextMenu(actionEvent)) {
                tw = clientFilesTable;
            }
            model.delete(isClientContextMenu(actionEvent), tw);
        } catch (Exception e) {
            GuiUtils.showAlert(Alert.AlertType.ERROR, "Удаление", "Ошибка", e.getMessage());
        }
    }

    public void copyMenuItemClicked(ActionEvent actionEvent) {
        if (isClientContextMenu(actionEvent)) {
            model.copyToServer(clientFilesTable, clientPath, serverPath);
        } else {
            model.copyFromServer(serverFilesTable, clientPath, serverPath);
        }
    }
}
