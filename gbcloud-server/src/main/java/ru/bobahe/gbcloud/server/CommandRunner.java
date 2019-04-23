package ru.bobahe.gbcloud.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
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
import java.nio.file.Paths;
import java.util.Map;

@Log
public class CommandRunner implements Invokable {
    private static CommandRunner ourInstance = new CommandRunner();

    private Command responseCommand;
    private static final AuthService authService = new SQLAuthService();
    private static final FileChunk fileChunk = new FileChunk();
    private static FileWorker fileWorker = new FileWorker();

    public static CommandRunner getInstance() {
        return ourInstance;
    }

    private CommandRunner() {

    }

    @Override
    public void invoke(Command command, ChannelHandlerContext ctx) {
        log.info("Client has sent command " + command.getAction());

        if (!isAuthenticatedClient(ctx.channel()) && command.getAction() != Command.Action.AUTH) {
            responseCommand = Command.builder().action(Command.Action.ERROR).description("Вы не авторизовались.").build();
            ctx.write(responseCommand);
            return;
        }

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
            default:
                sendMessage(Command.Action.ERROR, "Я еще не умею обрабатывать команды " + command.getAction(), ctx);
                break;
        }
    }

    private void sendList(String path, ChannelHandlerContext ctx) {
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
            responseCommand = Command.builder()
                    .action(Command.Action.ERROR)
                    .description(e.getMessage())
                    .build();
            ctx.writeAndFlush(responseCommand);
        }
    }

    private void sendFile(Command command, ChannelHandlerContext ctx) {
        fileChunk.setFilePath(
                ApplicationProperties.getInstance().getProperty("root.directory") +
                        File.separator +
                        findUserFolderByChannel(ctx) +
                        File.separator +
                        command.getPath() +
                        File.separator +
                        command.getFilename()
        );
        fileChunk.setDestinationFilePath(command.getDestinationPath());
        while (fileChunk.getNextChunk()) {
            ctx.writeAndFlush(fileChunk);
        }

        responseCommand = Command.builder()
                .action(Command.Action.SUCCESS)
                .description("Файл " + command.getFilename() + " успешно отправлен")
                .build();
        ctx.writeAndFlush(responseCommand);
    }

    private void sendMessage(Command.Action action, String s, ChannelHandlerContext ctx) {
        responseCommand = Command.builder().action(action).description(s).build();
        ctx.writeAndFlush(responseCommand);
    }

    private void authenticate(String username, String password, ChannelHandlerContext ctx) {
        String folder = authService.getFolderByUsernameAndPassword(
                username,
                password
        );

        if (folder == null) {
            responseCommand = Command.builder()
                    .action(Command.Action.ERROR)
                    .description("Неверные логин и/или пароль.")
                    .build();
            ctx.writeAndFlush(responseCommand);
            return;
        }

        if (!new FileWorker().checkFolders(Paths.get(
                ApplicationProperties.getInstance().getProperty("root.directory") + File.separator + folder))) {
            responseCommand = Command.builder()
                    .action(Command.Action.ERROR)
                    .description("На сервере отсутствует Ваша папка. Обратитесь к системному администратору.")
                    .build();
            ctx.writeAndFlush(responseCommand);
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
