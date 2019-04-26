package ru.bobahe.gbcloud.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.extern.java.Log;
import ru.bobahe.gbcloud.common.Command;
import ru.bobahe.gbcloud.common.FileChunk;
import ru.bobahe.gbcloud.common.Invokable;
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
    private static CommandRunner ourInstance = new CommandRunner();

    private Command responseCommand;
    private static final AuthService authService = new SQLAuthService();
    private static final FileChunk fileChunk = new FileChunk();
    private static FileWorker fileWorker = new FileWorker();

    @Getter
    private String lastRequestedPathForListing;

    public static CommandRunner getInstance() {
        return ourInstance;
    }

    private CommandRunner() {

    }

    @Override
    public void invoke(Command command, ChannelHandlerContext ctx) {
        if (!isAuthenticatedClient(ctx.channel()) &&
                command.getAction() != Command.Action.AUTH &&
                command.getAction() != Command.Action.REGISTER) {
            responseCommand = Command.builder().action(Command.Action.ERROR).description("Вы не авторизовались.").build();
            ctx.write(responseCommand);
            return;
        }

        log.info("Client has sent command " + command.getAction());

        switch (command.getAction()) {
            case AUTH:
                authenticate(command.getUsername(), command.getPassword(), ctx);
                break;
            case DOWNLOAD:
                sendFile(command, ctx);
                break;
            case LIST:
                sendList(command.getPath(), ctx);
                break;
            case DELETE:
                delete(command.getPath(), ctx);
                break;
            case CREATE:
                createDirectory(command, ctx);
                break;
            case REGISTER:
                registerClient(command.getUsername(), command.getPassword(), ctx);
                break;
            case UPLOAD:
                sendUploadEcho(command, ctx);
                break;
            default:
                sendMessage(Command.Action.ERROR, "Я еще не умею обрабатывать команды " + command.getAction(), ctx);
                break;
        }
    }

    private void createDirectory(Command command, ChannelHandlerContext ctx) {
        try {
            fileWorker.createDirectory(Paths.get(
                    ApplicationProperties.getInstance().getProperty("root.directory") +
                            File.separator +
                            findUserFolderByChannel(ctx) +
                            command.getPath()
            ));
            sendList(lastRequestedPathForListing, ctx);
        } catch (IOException e) {
            sendMessage(Command.Action.ERROR, "Не удалось создать папку.", ctx);
        }
    }

    private void sendUploadEcho(Command command, ChannelHandlerContext ctx) {
        responseCommand = command;
        ctx.writeAndFlush(responseCommand);
    }

    private void registerClient(String username, String password, ChannelHandlerContext ctx) {
        try {
            String hashedPassword = new String(MessageDigest.getInstance("MD5").digest());
            String uuidFolderName = UUID.randomUUID().toString();
            authService.insertNewUser(username, hashedPassword, uuidFolderName);

            sendMessage(Command.Action.SUCCESS, "Вы успешно зарегистрированы.", ctx);

            fileWorker.createDirectory(
                    Paths.get(
                            ApplicationProperties.getInstance().getProperty("root.directory") +
                                    File.separator +
                                    uuidFolderName)
            );
        } catch (Exception e) {
            sendMessage(Command.Action.ERROR, e.getMessage(), ctx);
        }
    }

    private void delete(String path, ChannelHandlerContext ctx) {
        try {
            fileWorker.delete(Paths.get(
                    ApplicationProperties.getInstance().getProperty("root.directory") +
                            File.separator +
                            findUserFolderByChannel(ctx) +
                            File.separator +
                            path
            ));

            if (!fileWorker.isDeleteFalse()) {
                return;
            }

            sendMessage(Command.Action.ERROR, "Возникла ошибка при удалении файла/ов.", ctx);
        } catch (IOException e) {
            sendMessage(Command.Action.ERROR, e.getClass().getSimpleName() + ": " + e.getMessage(), ctx);
        } finally {
            sendList(lastRequestedPathForListing, ctx);
        }
    }

    public void sendList(String path, ChannelHandlerContext ctx) {
        lastRequestedPathForListing = path;

        try {
            Map<String, Boolean> list = fileWorker.getFileList(
                    ApplicationProperties.getInstance().getProperty("root.directory") +
                            File.separator +
                            findUserFolderByChannel(ctx) +
                            File.separator +
                            path
            );

            responseCommand = Command.builder()
                    .action(Command.Action.LIST)
                    .path(path)
                    .childFiles(list)
                    .build();
            ctx.writeAndFlush(responseCommand);
        } catch (IOException e) {
            sendMessage(Command.Action.ERROR, e.getMessage(), ctx);
        }
    }

    private void sendFile(Command command, ChannelHandlerContext ctx) {
        String pathFromCopy = ApplicationProperties.getInstance().getProperty("root.directory") +
                File.separator +
                findUserFolderByChannel(ctx) +
                command.getPath();
        String fileName = command.getPath().substring(command.getPath().lastIndexOf(File.separator));

        try {
            Files.walk(Paths.get(pathFromCopy)).forEach(p -> {
                if (!Files.isDirectory(p)) {
                    String dstPath = command.getDestinationPath() +
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

    private void sendMessage(Command.Action action, String s, ChannelHandlerContext ctx) {
        responseCommand = Command.builder().action(action).description(s).build();
        ctx.writeAndFlush(responseCommand);
    }

    private void authenticate(String username, String password, ChannelHandlerContext ctx) {
        String folder = null;

        try {
            folder = authService.getFolderByUsernameAndPassword(
                    username,
                    new String(MessageDigest.getInstance("MD5").digest())
            );
        } catch (Exception e) {
            sendMessage(Command.Action.ERROR, e.getMessage(), ctx);
        }

        if (folder == null) {
            sendMessage(Command.Action.ERROR, "Неверные логин и/или пароль.", ctx);
            return;
        }

        if (!new FileWorker().checkFolders(Paths.get(
                ApplicationProperties.getInstance().getProperty("root.directory") + File.separator + folder))) {
            sendMessage(Command.Action.ERROR, "На сервере отсутствует Ваша папка. Обратитесь к системному администратору.", ctx);
            ctx.close();
            return;
        }

        AuthenticatedClients.getInstance().clients.put(folder, ctx.channel());

        sendMessage(Command.Action.SUCCESS, "Вы успешно авторизованы.", ctx);

        sendList(".", ctx);
    }

    private boolean isAuthenticatedClient(Channel channel) {
        return AuthenticatedClients.getInstance().clients.containsValue(channel);
    }

    private String findUserFolderByChannel(ChannelHandlerContext ctx) {
        for (String key : AuthenticatedClients.getInstance().clients.keySet()) {
            if (AuthenticatedClients.getInstance().clients.get(key) == ctx.channel()) {
                return key;
            }
        }

        return null;
    }
}
