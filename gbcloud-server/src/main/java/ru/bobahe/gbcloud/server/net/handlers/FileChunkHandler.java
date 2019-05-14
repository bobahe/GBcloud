package ru.bobahe.gbcloud.server.net.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.Setter;
import lombok.extern.java.Log;
import ru.bobahe.gbcloud.common.FileChunk;
import ru.bobahe.gbcloud.common.fs.FSUtils;
import ru.bobahe.gbcloud.server.CommandRunner;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Log
public class FileChunkHandler extends ChannelInboundHandlerAdapter {
    @Setter
    private CommandRunner commandRunner;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FileChunk) {
            FileChunk fileChunk = (FileChunk) msg;

            Path preparedPath = Paths.get(fileChunk.getFilePath()).getFileName();

            if (fileChunk.getDestinationFilePath() != null) {
                preparedPath = Paths.get(fileChunk.getDestinationFilePath()
                        + fileChunk.getFilePath().substring(fileChunk.getFilePath().lastIndexOf(File.separator) + 1));
            }

            if (fileChunk.getLength() != -1) {
                FSUtils.writeFileChunk(
                        Paths.get(commandRunner.getClientFolder() + File.separator + preparedPath.toString()),
                        fileChunk.getData(),
                        fileChunk.getOffset(),
                        fileChunk.getLength()
                );
            } else {
                commandRunner.sendList(commandRunner.getLastRequestedPathForListing(), ctx);
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
}
