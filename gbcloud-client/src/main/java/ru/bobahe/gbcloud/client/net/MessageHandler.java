package ru.bobahe.gbcloud.client.net;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import ru.bobahe.gbcloud.client.properties.ApplicationProperties;
import ru.bobahe.gbcloud.client.viewmodel.Filec;
import ru.bobahe.gbcloud.client.viewmodel.MainWindowModel;
import ru.bobahe.gbcloud.common.Command;
import ru.bobahe.gbcloud.common.FileChunk;

import java.io.File;
import java.io.IOException;

public class MessageHandler extends ChannelInboundHandlerAdapter {
    private Command responseCommand;
    private static final FileChunk fileChunk = new FileChunk();

    private MainWindowModel model = MainWindowModel.getInstance();

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
            Command response = (Command) msg;

            if (response.getAction() == Command.Action.ERROR) {
                System.err.println(response.getDescription());
                return;
            }

            if (response.getAction() == Command.Action.SUCCESS) {
                System.out.println(response.getDescription());
                return;
            }

            // LIST
            if (response.getAction() == Command.Action.LIST) {
                model.getServerFilesList().clear();

                if (!model.getServerPath().get().equals(File.separator)) {
                    model.getServerFilesList().add(
                            Filec.builder().name("..").isFolder("папка").build()
                    );
                }
                response.getChildFiles().forEach((n, f) ->
                        model.getServerFilesList().add(
                                Filec.builder().name(n).isFolder(f ? "папка" : "").build()
                        )
                );
            }

            // UPLOAD
            if (response.getAction() == Command.Action.UPLOAD) {
                fileChunk.setFilePath(
                        ApplicationProperties.getInstance().getProperty("root.directory") +
                                response.getPath()
                );
                fileChunk.setDestinationFilePath(response.getDestinationPath());

                try {
                    while (fileChunk.getNextChunk()) {
                        ctx.writeAndFlush(fileChunk);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
