package ru.bobahe.gbcloud.server.net.handlers;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import ru.bobahe.gbcloud.server.properties.ApplicationProperties;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

public class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {
    private SslContext sslContext;

    public ServerChannelInitializer() {
        boolean ssl = Boolean.parseBoolean(ApplicationProperties.getInstance().getProperty("ssl"));

        if (ssl) {
            try {
                SelfSignedCertificate certificate = new SelfSignedCertificate();
                sslContext = SslContextBuilder.forServer(
                        certificate.certificate(),
                        certificate.privateKey()
                ).build();
            } catch (CertificateException | SSLException e) {
                e.printStackTrace();
            }
        } else {
            sslContext = null;
        }
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) {
        ChannelPipeline pipeline = socketChannel.pipeline();

        if (sslContext != null) {
            pipeline.addLast(sslContext.newHandler(socketChannel.alloc()));
        }

        pipeline.addLast(
                new ObjectEncoder(),
                new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
                new MessageHandler(),
                new FileChunkHandler()
        );
    }
}
