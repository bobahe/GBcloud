package ru.bobahe.gbcloud.server.net.handlers;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.java.Log;
import ru.bobahe.gbcloud.common.Command;
import ru.bobahe.gbcloud.server.AuthenticatedClients;
import ru.bobahe.gbcloud.server.CommandRunner;

@Log
public class MessageHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("New client connected [" + ctx.channel().remoteAddress() + "]");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        log.info(msg.toString());
        if (msg instanceof Command) {
            CommandRunner.getInstance().invoke((Command) msg, ctx);
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
}
