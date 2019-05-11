package ru.bobahe.gbcloud.client.net;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.concurrent.Future;
import ru.bobahe.gbcloud.client.properties.ApplicationProperties;

public class Client {
    private static boolean ssl = Boolean.parseBoolean(ApplicationProperties.getInstance().getProperty("ssl"));

    EventLoopGroup group;
    private Bootstrap bootstrap;
    private Channel channel;

    private Future<?> closeFuture;

    public void connect() throws Exception {
        final SslContext sslCtx;
        if (ssl) {
            sslCtx = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } else {
            sslCtx = null;
        }

        group = new NioEventLoopGroup();
        try {
            bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();

                            if (sslCtx != null) {
                                p.addLast(sslCtx.newHandler(ch.alloc(), "127.0.0.1", 8192));
                            }

                            p.addLast(
                                    new ObjectEncoder(),
                                    new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
                                    new MessageHandler(),
                                    new FileChunkHandler()
                            );
                        }
                    });

            channel = bootstrap.connect("127.0.0.1", 8192).sync().channel();
            channel.closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }

    public Channel getChannel() {
        return channel;
    }

    public boolean close() {
        if (group != null && closeFuture == null) {
            closeFuture = group.shutdownGracefully();
        }

        return closeFuture.isDone();
    }
}
