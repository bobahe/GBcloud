package ru.bobahe.gbcloud.server.net.handlers;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import ru.bobahe.gbcloud.common.FileChunk;
import ru.bobahe.gbcloud.common.fs.FileWorker;
import ru.bobahe.gbcloud.server.AuthenticatedClients;
import ru.bobahe.gbcloud.server.properties.ApplicationProperties;

import java.io.File;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentMap;

public class FileChunkHandler extends ChannelInboundHandlerAdapter {
    private ConcurrentMap<String, Channel> clients = AuthenticatedClients.getInstance().clients;
    private static final FileWorker fileWorker = new FileWorker();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FileChunk) {
            FileChunk fileChunk = (FileChunk) msg;

            while (fileChunk.getLength() != -1) {
                fileWorker.writeFileChunk(
                        Paths.get(buildLocalPath(ctx.channel()) + fileChunk.getFilePath()),
                        fileChunk.getData(),
                        fileChunk.getOffset(),
                        fileChunk.getLength()
                );
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

    private String buildLocalPath(Channel channel) {
        StringBuilder localPath = new StringBuilder();

        localPath
                .append(ApplicationProperties.getInstance().getProperty("root.directory"))
                .append(File.separator)
                .append(findUserFolderByChannel(channel))
                .append(File.separator);

        return localPath.toString();
    }

    private String findUserFolderByChannel(Channel channel) {
        for (String key : clients.keySet()) {
            if (clients.get(key) == channel) {
                return key;
            }
        }

        return null;
    }
}
