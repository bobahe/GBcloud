package ru.bobahe.gbcloud.server.net.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.java.Log;
import ru.bobahe.gbcloud.common.Command;
import ru.bobahe.gbcloud.server.CommandRunner;

@Log
public class MessageHandler extends ChannelInboundHandlerAdapter {
    private CommandRunner commandRunner = new CommandRunner();

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("New client connected [" + ctx.channel().remoteAddress() + "]");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Command) {
            commandRunner.invoke((Command) msg, ctx);
        } else {
            if (commandRunner.isAuthenticated()) {
                ctx.pipeline().get(FileChunkHandler.class).setCommandRunner(commandRunner);
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
}
