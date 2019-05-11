package ru.bobahe.gbcloud.client.controller;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import ru.bobahe.gbcloud.client.viewmodel.Filec;
import ru.bobahe.gbcloud.client.viewmodel.GlobalViewModel;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    private GlobalViewModel model = GlobalViewModel.getInstance();
    private ObservableList<Filec> clientFilesList = model.getClientFilesList();
    private ObservableList<Filec> serverFilesList = model.getServerFilesList();
    private StringProperty serverPath = model.getServerPath();
    private StringProperty clientPath = model.getClientPath();

    @FXML
    private StackPane root;

    @FXML
    private AnchorPane anchorPane;

    @FXML
    private GridPane clientGridPane;

    @FXML
    private TableView<Filec> clientFilesTable, serverFilesTable;

    @FXML
    private Label lblClientPath, lblServerPath;

    @FXML
    private Circle connected;

    @FXML
    private ContextMenu serverFilesMenu;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        onCloseRequest();

        clientPath.setValue(File.separator);
        lblClientPath.textProperty().bind(clientPath);
        serverPath.setValue(File.separator);
        lblServerPath.textProperty().bind(serverPath);

        prepareTableViews();

        model.getClientFileList();

        serverFilesMenu.getItems().forEach(mi -> mi.setDisable(!model.getIsConnected().get()));

        model.getMessageFromServer().addListener(this::messageFromServer);
        model.getIsAuthenticated().addListener(this::getAuthCommand);
    }

    private void onCloseRequest() {
        Platform.runLater(() -> {
            root.getScene().getWindow().setOnCloseRequest(event -> {
                if (event.getEventType() == WindowEvent.WINDOW_CLOSE_REQUEST) {
                    while(!GlobalViewModel.getInstance().getClient().close()) {}
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
        Platform.runLater(() -> {
            Alert.AlertType type = Alert.AlertType.ERROR;
            String headerText = "Ошибка";

            if (model.getMessageFromServerType().get() == 0) {
                type = Alert.AlertType.INFORMATION;
                headerText = "Успех";
            }

            Alert alert = new Alert(type, ((StringProperty) observable).get(), ButtonType.OK);
            alert.setTitle("Собщение от сервера");
            alert.setHeaderText(headerText);
            alert.showAndWait();
        });
    }

    @SuppressWarnings("unchecked")
    private void prepareTableViews() {
        clientFilesTable.getColumns().addAll(
                getNewColumn("Имя", "name"),
                getNewColumn("Тип", "isFolder")
        );
        clientFilesTable.setItems(clientFilesList);

        serverFilesTable.getColumns().addAll(
                getNewColumn("Имя", "name"),
                getNewColumn("Тип", "isFolder")
        );
        serverFilesTable.setItems(serverFilesList);

        setTableViewsColumnWidth();
    }

    private void setTableViewsColumnWidth() {
        Platform.runLater(() -> {
            clientFilesTable.getColumns().get(0).setPrefWidth(clientFilesTable.getWidth() * 85 / 100);
            clientFilesTable.getColumns().get(1).setPrefWidth(clientFilesTable.getWidth() * 14 / 100);
            serverFilesTable.getColumns().get(0).setPrefWidth(clientFilesTable.getWidth() * 85 / 100);
            serverFilesTable.getColumns().get(1).setPrefWidth(clientFilesTable.getWidth() * 14 / 100);
        });
    }

    private TableColumn<Filec, String> getNewColumn(String name, String propertyName) {
        TableColumn<Filec, String> newColumn = new TableColumn<>(name);
        newColumn.setCellValueFactory(new PropertyValueFactory<>(propertyName));
        return newColumn;
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
//        if (isClient) {
//            Filec dir = Filec.builder().name(String.valueOf(clientFilesTable.getItems().size())).isFolder("папка").build();
//            clientFilesTable.getItems().add(dir);
//            clientFilesTable.getSelectionModel().select(dir);
//            //clientFilesTable.setEditable(true);
//            clientFilesTable.layout();
//            clientFilesTable.edit(clientFilesTable.getItems().size() - 1, clientFilesTable.getColumns().get(1));
//        }
        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/NewDirectory.fxml"));
            Parent root = loader.load();
            NewDirectoryController controller = loader.getController();
            controller.setPath(isClient ? clientPath.get() : serverPath.get());
            controller.setClient(isClient);

            stage.setTitle("Новая папка");
            stage.setScene(new Scene(root, 270, 100));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isClientContextMenu(ActionEvent actionEvent) {
        return ((MenuItem) actionEvent.getSource()).getParentPopup().getId().startsWith("client");
    }

    public void newDirectoryMenuItemClicked(ActionEvent actionEvent) {
        showNewDirectoryModal(isClientContextMenu(actionEvent));
    }

    public void deleteMenuItemClicked(ActionEvent actionEvent) {
        try {
            TableView<Filec> tw = serverFilesTable;
            if (isClientContextMenu(actionEvent)) {
                tw = clientFilesTable;
            }
            model.delete(isClientContextMenu(actionEvent), tw);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK);
            alert.setTitle("Удаление");
            alert.setHeaderText("Ошибка");
            alert.showAndWait();
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
