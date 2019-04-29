package ru.bobahe.gbcloud.client.net;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import ru.bobahe.gbcloud.client.properties.ApplicationProperties;
import ru.bobahe.gbcloud.client.viewmodel.Filec;
import ru.bobahe.gbcloud.client.viewmodel.GlobalViewModel;
import ru.bobahe.gbcloud.common.FileChunk;
import ru.bobahe.gbcloud.common.command.Action;
import ru.bobahe.gbcloud.common.command.Command;
import ru.bobahe.gbcloud.common.command.parameters.DescriptionParameters;
import ru.bobahe.gbcloud.common.command.parameters.FileParameters;
import ru.bobahe.gbcloud.common.command.parameters.ListParameters;

import java.io.File;
import java.io.IOException;

public class MessageHandler extends ChannelInboundHandlerAdapter {
    private static final FileChunk fileChunk = new FileChunk();
    private GlobalViewModel model = GlobalViewModel.getInstance();

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        model.getIsConnected().set(true);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Command) {
            Command receivedCommand = (Command) msg;

            DescriptionParameters description = null;
            if (receivedCommand.getParameters() instanceof DescriptionParameters) {
                description = ((DescriptionParameters) receivedCommand.getParameters());
            }

            switch (receivedCommand.getAction()) {
                case ERROR:
                case SUCCESS:
                    if (description == null) {
                        return;
                    }

                    model.getMessageFromServerType().set(0);
                    if (receivedCommand.getAction() == Action.ERROR) {
                        model.getMessageFromServerType().set(1);
                    }
                    model.getMessageFromServer().set(description.getDescription());
                    break;
                case AUTH:
                    if (description == null) {
                        return;
                    }
                    if (description.getDescription().equals("OK")) {
                        model.getIsAuthenticated().set(true);
                    }

                    model.getMessageFromServerType().set(1);
                    model.getMessageFromServer().set(description.getDescription());
                    break;
                case REGISTER:
                    if (description == null) {
                        return;
                    }
                    if (description.getDescription().startsWith("OK")) {
                        model.getMessageFromServerType().set(0);
                    }
                    model.getMessageFromServer().set(description.getDescription());
                    break;
                case LIST:
                    if (receivedCommand.getParameters() instanceof ListParameters) {
                        ListParameters params = ((ListParameters) receivedCommand.getParameters());
                        model.getServerFilesList().clear();

                        if (!model.getServerPath().get().equals(File.separator)) {
                            model.getServerFilesList().add(
                                    Filec.builder().name("..").isFolder("папка").build()
                            );
                        }
                        params.getFileList().forEach((n, f) ->
                                model.getServerFilesList().add(
                                        Filec.builder().name(n).isFolder(f ? "папка" : "").build()
                                )
                        );
                    }
                    break;
                case UPLOAD:
                    if (receivedCommand.getParameters() instanceof FileParameters) {
                        FileParameters params = ((FileParameters) receivedCommand.getParameters());
                        fileChunk.setFilePath(
                                ApplicationProperties.getInstance().getProperty("root.directory") +
                                        params.getPath()
                        );
                        fileChunk.setDestinationFilePath(params.getDestinationPath());

                        try {
                            while (fileChunk.getNextChunk()) {
                                ctx.writeAndFlush(fileChunk);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
            }
        } else {
            ctx.fireChannelRead(msg);
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
}
