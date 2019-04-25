package ru.bobahe.gbcloud.client.controller;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import ru.bobahe.gbcloud.client.net.Client;
import ru.bobahe.gbcloud.client.properties.ApplicationProperties;
import ru.bobahe.gbcloud.client.viewmodel.Filec;
import ru.bobahe.gbcloud.client.viewmodel.MainWindowModel;
import ru.bobahe.gbcloud.common.Command;
import ru.bobahe.gbcloud.common.fs.FileWorker;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    private ObservableList<Filec> clientFilesList = MainWindowModel.getInstance().getClientFilesList();
    private ObservableList<Filec> serverFilesList = MainWindowModel.getInstance().getServerFilesList();
    private StringProperty serverPath = MainWindowModel.getInstance().getServerPath();
    private StringProperty clientPath = new SimpleStringProperty();

    private static final Client client = new Client();
    private static Command responseCommand;

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

        getClientFileList();
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

    private void getClientFileList() {
        try {
            if (!clientPath.get().equals(File.separator)) {
                clientFilesList.add(Filec.builder().name("..").isFolder("Папка").build());
            }
            Map<String, Boolean> fileList = new FileWorker().getFileList(
                    ApplicationProperties.getInstance().getProperty("root.directory") +
                    clientPath.get());
            fileList.forEach((n, f) -> clientFilesList.add(Filec.builder().name(n).isFolder(f ? "Папка" : "").build()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clientFilesTableClick(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() > 1) {
            changeDir(clientFilesTable, clientPath, true);
        }
    }

    public void clientFilesTableKeyPressed(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER) {
            changeDir(clientFilesTable, clientPath, true);
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
            changeDir(serverFilesTable, serverPath, false);
        }
    }


    public void serverFilesTableKeyPressed(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER) {
            changeDir(serverFilesTable, serverPath, false);
        }
    }

    private void changeDir(TableView<Filec> tw, StringProperty path, boolean isClientPath) {
        Filec selectedItem = tw.getSelectionModel().getSelectedItem();

        if (selectedItem == null) {
            return;
        }

        if (selectedItem.getIsFolder().equals("Папка")) {
            if (selectedItem.getName().equals("..")) {
                int lastIndex = path.get().length() - 2;
                path.setValue(
                        path.get().substring(
                                0,
                                path.get().substring(0, lastIndex).lastIndexOf(File.separator) + 1
                        )
                );
            } else {
                path.setValue(path.get() + selectedItem.getName() + File.separator);
            }

            if (isClientPath) {
                clientFilesList.clear();
                getClientFileList();
            } else {
                responseCommand = Command.builder()
                        .action(Command.Action.LIST)
                        .path(serverPath.get())
                        .build();
                client.getChannel().writeAndFlush(responseCommand);
            }
        }
    }
}
