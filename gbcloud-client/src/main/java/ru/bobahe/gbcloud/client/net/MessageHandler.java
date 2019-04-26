package ru.bobahe.gbcloud.client.net;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import ru.bobahe.gbcloud.client.properties.ApplicationProperties;
import ru.bobahe.gbcloud.client.viewmodel.Filec;
import ru.bobahe.gbcloud.client.viewmodel.globalViewModel;
import ru.bobahe.gbcloud.common.Command;
import ru.bobahe.gbcloud.common.FileChunk;

import java.io.File;
import java.io.IOException;

public class MessageHandler extends ChannelInboundHandlerAdapter {
    private Command responseCommand;
    private static final FileChunk fileChunk = new FileChunk();

    private globalViewModel model = globalViewModel.getInstance();

    public MessageHandler() {

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws IOException {
//        responseCommand = Command.builder()
//                .action(Command.Action.REGISTER)
//                .username("user")
//                .password("password")
//                .build();
//        ctx.writeAndFlush(responseCommand);

        responseCommand = Command.builder()
                .action(Command.Action.AUTH)
                .username("user")
                .password("password")
                .build();
        ctx.writeAndFlush(responseCommand);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Command) {
            Command receivedCommand = (Command) msg;

            switch (receivedCommand.getAction()) {
                case ERROR:
                    model.getMessageFromServerType().set(1);
                    model.getMessageFromServer().set(receivedCommand.getDescription());
                    break;
                case SUCCESS:
                    model.getMessageFromServerType().set(0);
                    model.getMessageFromServer().set(receivedCommand.getDescription());
                    break;
                case LIST:
                    model.getServerFilesList().clear();

                    if (!model.getServerPath().get().equals(File.separator)) {
                        model.getServerFilesList().add(
                                Filec.builder().name("..").isFolder("папка").build()
                        );
                    }
                    receivedCommand.getChildFiles().forEach((n, f) ->
                            model.getServerFilesList().add(
                                    Filec.builder().name(n).isFolder(f ? "папка" : "").build()
                            )
                    );
                    break;
                case UPLOAD:
                    fileChunk.setFilePath(
                            ApplicationProperties.getInstance().getProperty("root.directory") +
                                    receivedCommand.getPath()
                    );
                    fileChunk.setDestinationFilePath(receivedCommand.getDestinationPath());

                    try {
                        while (fileChunk.getNextChunk()) {
                            ctx.writeAndFlush(fileChunk);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
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
