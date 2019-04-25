package ru.bobahe.gbcloud.server.net.handlers;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.java.Log;
import ru.bobahe.gbcloud.common.Command;
import ru.bobahe.gbcloud.common.FileChunk;
import ru.bobahe.gbcloud.common.fs.FileWorker;
import ru.bobahe.gbcloud.server.AuthenticatedClients;
import ru.bobahe.gbcloud.server.properties.ApplicationProperties;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentMap;

@Log
public class FileChunkHandler extends ChannelInboundHandlerAdapter {
    private ConcurrentMap<String, Channel> clients = AuthenticatedClients.getInstance().clients;
    private static final FileWorker fileWorker = new FileWorker();
    private Command responseCommand;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FileChunk) {
            FileChunk fileChunk = (FileChunk) msg;

            Path preparedPath = Paths.get(fileChunk.getFilePath()).getFileName();

            if (fileChunk.getDestinationFilePath() != null) {
                preparedPath = Paths.get(fileChunk.getDestinationFilePath()
                        + fileChunk.getFilePath().substring(fileChunk.getFilePath().lastIndexOf(File.separator)));
            }

            if (fileChunk.getLength() != -1) {
                fileWorker.writeFileChunk(
                        Paths.get(buildLocalPath(ctx) + preparedPath.toString()),
                        fileChunk.getData(),
                        fileChunk.getOffset(),
                        fileChunk.getLength()
                );
                System.out.println(fileChunk.getOffset() / fileChunk.getData().length + " -> " + fileChunk.getLength());
            } else {
                responseCommand = Command.builder()
                        .action(Command.Action.SUCCESS)
                        .description(preparedPath.toString())
                        .build();
                ctx.writeAndFlush(responseCommand);
            }
        } else {
            System.out.println("От тебя пришла какая-то туфта.");
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

    private String buildLocalPath(ChannelHandlerContext ctx) {
        StringBuilder localPath = new StringBuilder();

        localPath
                .append(ApplicationProperties.getInstance().getProperty("root.directory"))
                .append(File.separator)
                .append(findUserFolderByChannel(ctx))
                .append(File.separator);

        return localPath.toString();
    }

    private String findUserFolderByChannel(ChannelHandlerContext ctx) {
        for (String key : clients.keySet()) {
            if (clients.get(key) == ctx.channel()) {
                return key;
            }
        }

        return null;
    }
}
