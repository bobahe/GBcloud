package ru.bobahe.gbcloud.server.net.handlers;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import ru.bobahe.gbcloud.common.Command;
import ru.bobahe.gbcloud.common.FileChunk;
import ru.bobahe.gbcloud.common.fs.FileWorker;
import ru.bobahe.gbcloud.server.AuthenticatedClients;
import ru.bobahe.gbcloud.server.auth.AuthService;
import ru.bobahe.gbcloud.server.auth.SQLAuthService;
import ru.bobahe.gbcloud.server.properties.ApplicationProperties;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MessageHandler extends ChannelInboundHandlerAdapter {
    private static final AuthService authService = new SQLAuthService();
    private static final Command command = new Command();
    private static final FileChunk fileChunk = new FileChunk(command);

    private static final Path rootFolder = Paths.get(ApplicationProperties.getInstance().getProperty("root.directory"));

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Command) {
            Command response = (Command) msg;

            if (!isAuthenticatedClient(ctx.channel())) {
                if (response.getAction() != Command.Action.AUTH) {
                    command.setAction(Command.Action.ERROR).setDescription("Вы не авторизовались.");
                    ctx.write(command);
                    return;
                }

                String folder = authService.getFolderByUsernameAndPassword(
                        response.getUsername(),
                        response.getPassword()
                );

                if (folder == null) {
                    command.setAction(Command.Action.ERROR).setDescription("Неверные логин и/или пароль.");
                    ctx.writeAndFlush(command);
                    return;
                }

                if (!new FileWorker().checkFolders(Paths.get(rootFolder + File.separator + folder))) {
                    command.setAction(Command.Action.ERROR).setDescription("На сервере отсутствует Ваша папка." +
                            " Обратитесь к системному администратору");
                    ctx.writeAndFlush(command);
                    ctx.close();
                    return;
                }

                AuthenticatedClients.getInstance().clients.put(folder, ctx.channel());

                command.setAction(Command.Action.LIST);
                ctx.writeAndFlush(command);
            } else {
                switch (response.getAction()) {
                    case DOWNLOAD:
                        fileChunk.setFilePath(
                                ApplicationProperties.getInstance().getProperty("root.directory") +
                                        File.separator +
                                        findUserFolderByChannel(ctx) +
                                        File.separator +
                                        response.getFilename()
                        );
                        while (fileChunk.getNextChunk()) {
                            ctx.writeAndFlush(fileChunk);
                        }
                        break;
                }
            }
        } else {
            if (isAuthenticatedClient(ctx.channel())) {
                ctx.fireChannelRead(msg);
            }
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
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
