package ru.bobahe.gbcloud.common;

import io.netty.channel.ChannelHandlerContext;

public interface Invokable {
    void invoke(Command command, ChannelHandlerContext ctx);
}
