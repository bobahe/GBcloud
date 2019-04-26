package ru.bobahe.gbcloud.client.controller;

import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ru.bobahe.gbcloud.client.net.Client;
import ru.bobahe.gbcloud.client.viewmodel.Filec;
import ru.bobahe.gbcloud.client.viewmodel.globalViewModel;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    private static final Client client = new Client();
    private globalViewModel model = globalViewModel.getInstance().setClient(client);
    private ObservableList<Filec> clientFilesList = model.getClientFilesList();
    private ObservableList<Filec> serverFilesList = model.getServerFilesList();
    private StringProperty serverPath = model.getServerPath();
    private StringProperty clientPath = model.getClientPath();

    @FXML
    AnchorPane anchorPane;

    @FXML
    GridPane clientGridPane;

    @FXML
    TableView<Filec> clientFilesTable, serverFilesTable;

    @FXML
    Label lblClientPath, lblServerPath;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        clientPath.setValue(File.separator);
        lblClientPath.textProperty().bind(clientPath);
        serverPath.setValue(File.separator);
        lblServerPath.textProperty().bind(serverPath);

        prepareTableViews();

        model.getClientFileList();
    }

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

    public void menuConnectAction(ActionEvent actionEvent) {
        new Thread(() -> {
            try {
                if (client.getChannel() == null) {
                    client.connect();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
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
            NewDirectoryController controller = (NewDirectoryController) loader.getController();
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
}
