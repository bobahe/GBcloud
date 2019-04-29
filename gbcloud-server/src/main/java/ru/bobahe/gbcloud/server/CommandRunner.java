package ru.bobahe.gbcloud.server;

import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.extern.java.Log;
import ru.bobahe.gbcloud.common.FileChunk;
import ru.bobahe.gbcloud.common.Invokable;
import ru.bobahe.gbcloud.common.command.Action;
import ru.bobahe.gbcloud.common.command.Command;
import ru.bobahe.gbcloud.common.command.parameters.CredentialParameters;
import ru.bobahe.gbcloud.common.command.parameters.DescriptionParameters;
import ru.bobahe.gbcloud.common.command.parameters.FileParameters;
import ru.bobahe.gbcloud.common.command.parameters.ListParameters;
import ru.bobahe.gbcloud.common.fs.FileWorker;
import ru.bobahe.gbcloud.server.auth.AuthService;
import ru.bobahe.gbcloud.server.auth.SQLAuthService;
import ru.bobahe.gbcloud.server.properties.ApplicationProperties;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;

@Log
public class CommandRunner implements Invokable {
    private Command responseCommand;
    private static final AuthService authService = new SQLAuthService();
    private static final FileChunk fileChunk = new FileChunk();
    private static FileWorker fileWorker = new FileWorker();

    @Getter
    private String lastRequestedPathForListing;

    @Getter
    private boolean isAuthenticated;

    @Getter
    private String clientFolder;

    @Override
    public void invoke(Command command, ChannelHandlerContext ctx) {
        if (!isAuthenticated && command.getAction() != Action.AUTH && command.getAction() != Action.REGISTER) {
            sendMessage(Action.ERROR, "Вы не авторизовались.", ctx);
            return;
        }

        log.info("Client has sent command " + command.getAction());

        switch (command.getAction()) {
            case AUTH:
                if (command.getParameters() instanceof CredentialParameters) {
                    CredentialParameters params = ((CredentialParameters) command.getParameters());
                    authenticate(params.getUsername(), params.getPassword(), ctx);
                }
                break;
            case DOWNLOAD:
                if (command.getParameters() instanceof FileParameters) {
                    FileParameters params = ((FileParameters) command.getParameters());
                    sendFile(params.getPath(), params.getDestinationPath(), ctx);
                }
                break;
            case LIST:
                if (command.getParameters() instanceof ListParameters) {
                    ListParameters params = ((ListParameters) command.getParameters());
                    sendList(params.getPath(), ctx);
                }
                break;
            case DELETE:
                if (command.getParameters() instanceof FileParameters) {
                    FileParameters params = ((FileParameters) command.getParameters());
                    delete(params.getPath(), ctx);
                }
                break;
            case CREATE:
                if (command.getParameters() instanceof FileParameters) {
                    FileParameters params = ((FileParameters) command.getParameters());
                    createDirectory(params.getPath(), ctx);
                }
                break;
            case REGISTER:
                if (command.getParameters() instanceof CredentialParameters) {
                    CredentialParameters params = ((CredentialParameters) command.getParameters());
                    registerClient(params.getUsername(), params.getPassword(), ctx);
                }
                break;
            case UPLOAD:
                sendUploadEcho(command, ctx);
                break;
            default:
                sendMessage(Action.ERROR, "Я еще не умею обрабатывать команды " + command.getAction(), ctx);
                break;
        }
    }

    private void createDirectory(String path, ChannelHandlerContext ctx) {
        try {
            fileWorker.createDirectory(Paths.get(clientFolder + path));
            sendList(lastRequestedPathForListing, ctx);
        } catch (IOException e) {
            sendMessage(Action.ERROR, "Не удалось создать папку.", ctx);
        }
    }

    private void sendUploadEcho(Command command, ChannelHandlerContext ctx) {
        responseCommand = command;
        ctx.writeAndFlush(responseCommand);
    }

    private void registerClient(String username, String password, ChannelHandlerContext ctx) {
        try {
            String hashedPassword = new String(MessageDigest.getInstance("MD5").digest(password.getBytes()));
            String uuidFolderName = UUID.randomUUID().toString();
            authService.insertNewUser(username, hashedPassword, uuidFolderName);


            fileWorker.createDirectory(
                    Paths.get(
                            ApplicationProperties.getInstance().getProperty("root.directory") +
                                    File.separator +
                                    uuidFolderName)
            );

            sendMessage(Action.REGISTER, "OK. Вы успешно зарегистрированы.", ctx);
        } catch (Exception e) {
            sendMessage(Action.REGISTER, e.getMessage(), ctx);
        }
    }

    private void delete(String path, ChannelHandlerContext ctx) {
        try {
            fileWorker.delete(Paths.get(clientFolder + File.separator + path));

            if (!fileWorker.isDeleteFalse()) {
                return;
            }

            sendMessage(Action.ERROR, "Возникла ошибка при удалении файла/ов.", ctx);
        } catch (IOException e) {
            sendMessage(Action.ERROR, e.getClass().getSimpleName() + ": " + e.getMessage(), ctx);
        } finally {
            sendList(lastRequestedPathForListing, ctx);
        }
    }

    public void sendList(String path, ChannelHandlerContext ctx) {
        lastRequestedPathForListing = path;

        try {
            Map<String, Boolean> list = fileWorker.getFileList(clientFolder + File.separator + path);

            responseCommand = Command.builder().action(Action.LIST).parameters(new ListParameters(path, list)).build();
            ctx.writeAndFlush(responseCommand);
        } catch (IOException e) {
            sendMessage(Action.ERROR, e.getMessage(), ctx);
        }
    }

    private void sendFile(String path, String destinationPath, ChannelHandlerContext ctx) {
        String pathFromCopy = clientFolder + path;
        String fileName = path.substring(path.lastIndexOf(File.separator));

        try {
            Files.walk(Paths.get(pathFromCopy)).forEach(p -> {
                if (!Files.isDirectory(p)) {
                    String dstPath = destinationPath +
                            p.toString().substring(p.toString().indexOf(fileName), p.toString().lastIndexOf(File.separator));

                    fileChunk.setFilePath(p.toString());
                    fileChunk.setDestinationFilePath(dstPath);

                    try {
                        while (fileChunk.getNextChunk()) {
                            ctx.writeAndFlush(fileChunk);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void authenticate(String username, String password, ChannelHandlerContext ctx) {
        String folder = null;

        try {
            folder = authService.getFolderByUsernameAndPassword(
                    username,
                    new String(MessageDigest.getInstance("MD5").digest(password.getBytes()))
            );
        } catch (Exception e) {
            sendMessage(Action.AUTH, e.getMessage(), ctx);
        }

        if (folder == null) {
            sendMessage(Action.AUTH, "Неверные логин и/или пароль.", ctx);
            return;
        }

        clientFolder = ApplicationProperties.getInstance().getProperty("root.directory") + File.separator + folder;

        if (!new FileWorker().checkFolders(Paths.get(clientFolder))) {
            sendMessage(Action.AUTH, "На сервере отсутствует Ваша папка. Обратитесь к системному администратору.", ctx);
            ctx.close();
            return;
        }

        isAuthenticated = true;

        sendMessage(Action.AUTH, "OK", ctx);

        sendList(".", ctx);
    }

    private void sendMessage(Action action, String message, ChannelHandlerContext ctx) {
        responseCommand = Command.builder().action(action).parameters(new DescriptionParameters(message)).build();
        ctx.writeAndFlush(responseCommand);
    }
}
