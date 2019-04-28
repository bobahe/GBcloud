package ru.bobahe.gbcloud.common;

import io.netty.channel.ChannelHandlerContext;
import ru.bobahe.gbcloud.common.command.Command;

public interface Invokable {
    void invoke(Command command, ChannelHandlerContext ctx);
}
