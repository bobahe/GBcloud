package ru.bobahe.gbcloud.client.net;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import ru.bobahe.gbcloud.client.properties.ApplicationProperties;
import ru.bobahe.gbcloud.client.viewmodel.globalViewModel;
import ru.bobahe.gbcloud.common.FileChunk;
import ru.bobahe.gbcloud.common.fs.FileWorker;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileChunkHandler extends ChannelInboundHandlerAdapter {
    private static final FileWorker fileWorker = new FileWorker();

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
            } else {
                globalViewModel.getInstance().getClientFilesList().clear();
                globalViewModel.getInstance().getClientFileList();
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

    private String buildLocalPath(ChannelHandlerContext ctx) {
        StringBuilder localPath = new StringBuilder();

        localPath
                .append(ApplicationProperties.getInstance().getProperty("root.directory"))
                .append(File.separator);

        return localPath.toString();
    }
}

