package ru.bobahe.gbcloud.client.viewmodel;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;
import lombok.Getter;
import ru.bobahe.gbcloud.client.net.Client;
import ru.bobahe.gbcloud.client.properties.ApplicationProperties;
import ru.bobahe.gbcloud.common.command.Action;
import ru.bobahe.gbcloud.common.command.Command;
import ru.bobahe.gbcloud.common.command.parameters.CredentialParameters;
import ru.bobahe.gbcloud.common.command.parameters.FileParameters;
import ru.bobahe.gbcloud.common.command.parameters.ListParameters;
import ru.bobahe.gbcloud.common.fs.FileWorker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class GlobalViewModel {
    // region Singleton
    private static GlobalViewModel ourInstance = new GlobalViewModel();

    private GlobalViewModel() {}

    public static GlobalViewModel getInstance() {
        return ourInstance;
    }
    // endregion

    @Getter
    private Client client = new Client();
    private Command responseCommand;
    private FileWorker fileWorker = new FileWorker();

    @Getter
    private BooleanProperty isConnected = new SimpleBooleanProperty(false);

    @Getter
    private ObservableList<Filec> clientFilesList = FXCollections.observableArrayList();

    @Getter
    private ObservableList<Filec> serverFilesList = FXCollections.observableArrayList();

    @Getter
    private StringProperty serverPath = new SimpleStringProperty(File.separator);

    @Getter
    private StringProperty clientPath = new SimpleStringProperty(File.separator);

    @Getter
    private StringProperty messageFromServer = new SimpleStringProperty();

    @Getter
    private IntegerProperty messageFromServerType = new SimpleIntegerProperty();

    @Getter
    private BooleanProperty isAuthenticated = new SimpleBooleanProperty(false);

    public void authenticate(String username, String password) {
        if (client.getChannel() == null) {
            return;
        }

        responseCommand = Command.builder()
                .action(Action.AUTH)
                .parameters(new CredentialParameters(username, password))
                .build();
        client.getChannel().writeAndFlush(responseCommand);
    }

    public void getClientFileList() {
        try {
            if (!clientPath.get().equals(File.separator)) {
                clientFilesList.add(Filec.builder().name("..").isFolder("папка").build());
            }
            Map<String, Boolean> fileList = new FileWorker().getFileList(
                    ApplicationProperties.getInstance().getProperty("root.directory") +
                            clientPath.get());
            fileList.forEach((n, f) -> clientFilesList.add(Filec.builder().name(n).isFolder(f ? "папка" : "").build()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getServerFileList() {
        responseCommand = Command.builder()
                .action(Action.LIST)
                .parameters(new ListParameters(serverPath.get(), null))
                .build();
        client.getChannel().writeAndFlush(responseCommand);
    }

    public void changeDir(TableView<Filec> tw, StringProperty path, boolean isClientPath) {
        Filec selectedItem = tw.getSelectionModel().getSelectedItem();

        if (selectedItem == null) {
            return;
        }

        // todo ОБЯЗАТЕЛЬНО переделать представление файла так, чтобы проверять boolean, а не строку!!!!!
        if (selectedItem.getIsFolder().equals("папка")) {
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
                getServerFileList();
            }
        }
    }

    public void copyToServer(TableView<Filec> tw, StringProperty from, StringProperty to) {
        Filec selectedItem = tw.getSelectionModel().getSelectedItem();

        if (selectedItem == null || selectedItem.getName().equals("..")) {
            return;
        }

        if (client.getChannel() == null) {
            return;
        }

        String sourcePath = ApplicationProperties.getInstance().getProperty("root.directory") + from.get() + selectedItem.getName();

        try {
            Files.walk(Paths.get(sourcePath)).forEach(p -> {
                if (!Files.isDirectory(p)) {
                    String dstPath = to.get();

                    if (selectedItem.getIsFolder().equals("папка")) {
                        dstPath = to.get() + p.toString().substring(
                                p.toString().indexOf(selectedItem.getName()),
                                p.toString().lastIndexOf(File.separator) + 1
                        );
                    }

                    responseCommand = Command.builder()
                            .action(Action.UPLOAD)
                            .parameters(new FileParameters(File.separator + p.subpath(1, p.getNameCount()).toString(), dstPath))
                            .build();
                    client.getChannel().writeAndFlush(responseCommand);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void copyFromServer(TableView<Filec> serverFilesTable, StringProperty clientPath, StringProperty serverPath) {
        Filec selectedItem = serverFilesTable.getSelectionModel().getSelectedItem();

        if (selectedItem == null || selectedItem.getName().equals("..")) {
            return;
        }

        if (client.getChannel() == null) {
            return;
        }

        responseCommand = Command.builder()
                .action(Action.DOWNLOAD)
                .parameters(new FileParameters(serverPath.get() + selectedItem.getName(), clientPath.get()))
                .build();
        client.getChannel().writeAndFlush(responseCommand);
    }

    public void delete(boolean isClient, TableView<Filec> tw) throws Exception {
        Filec selecteItem = tw.getSelectionModel().getSelectedItem();

        if (selecteItem == null || selecteItem.getName().equals("..")) {
            return;
        }

        if (isClient) {
            Path pathToDelete = Paths.get(
                    ApplicationProperties.getInstance().getProperty("root.directory") +
                            clientPath.get() +
                            selecteItem.getName()
            );
            fileWorker.delete(pathToDelete);
            clientFilesList.clear();
            getClientFileList();
        } else {
            if (client.getChannel() == null) {
                return;
            }

            responseCommand = Command.builder()
                    .action(Action.DELETE)
                    .parameters(new FileParameters(serverPath.get() + selecteItem.getName(), null))
                    .build();
            client.getChannel().writeAndFlush(responseCommand);
        }
    }

    public void createDirectory(boolean isClient, String path) throws Exception {
        if (isClient) {
            fileWorker.createDirectory(Paths.get(
                    ApplicationProperties.getInstance().getProperty("root.directory") + path));
            clientFilesList.clear();
            getClientFileList();
        } else {
            if (client.getChannel() == null) {
                return;
            }

            responseCommand = Command.builder()
                    .action(Action.CREATE)
                    .parameters(new FileParameters(path, null))
                    .build();
            client.getChannel().writeAndFlush(responseCommand);
        }
    }

    public void register(String username, String password) {
        if (client.getChannel() == null) {
            return;
        }

        responseCommand = Command.builder()
                .action(Action.REGISTER)
                .parameters(new CredentialParameters(username, password))
                .build();
        client.getChannel().writeAndFlush(responseCommand);
    }
}
