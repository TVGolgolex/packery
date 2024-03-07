package de.pascxl.packery.netty.server;

import de.pascxl.packery.NettyAddress;
import de.pascxl.packery.Packery;
import de.pascxl.packery.netty.NettyUtils;
import de.pascxl.packery.netty.packet.PacketManager;
import de.pascxl.packery.netty.transmitter.NettyTransmitter;
import de.pascxl.packery.netty.transmitter.NettyTransmitterAuth;
import io.netty5.bootstrap.ServerBootstrap;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelOption;
import io.netty5.channel.MultithreadEventLoopGroup;
import io.netty5.channel.epoll.Epoll;
import io.netty5.handler.ssl.SslContext;
import io.netty5.handler.ssl.util.SelfSignedCertificate;
import io.netty5.util.concurrent.Future;
import lombok.Getter;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/*
 * MIT License
 *
 * Copyright (c) 2024 Mario Kurz
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
@Getter
public class NettyServer implements AutoCloseable {

    private final MultithreadEventLoopGroup bossGroup = new MultithreadEventLoopGroup(1, NettyUtils.createIoHandlerFactory());
    private final MultithreadEventLoopGroup workerGroup = new MultithreadEventLoopGroup(1, NettyUtils.createIoHandlerFactory());
    private final PacketManager packetManager;
    private final NettyAddress nettyAddress;
    private ServerBootstrap serverBootstrap;
    private SslContext sslContext;
    private boolean connected = false;

    private final List<NettyTransmitter> authenticatedTransmitters = new ArrayList<>();
    private final List<NettyTransmitterAuth> waitingForAuthentication = new ArrayList<>();

    public NettyServer(NettyAddress nettyAddress) {
        this.nettyAddress = nettyAddress;
        this.packetManager = new PacketManager();
    }

    public boolean tryConnect(boolean ssl, Runnable failRunnable) {
        if (ssl) {
            Packery.LOGGER.info("Enabling SSL Context for service requests");
            SelfSignedCertificate ssc;
            try {
                ssc = new SelfSignedCertificate();
            } catch (CertificateException e) {
                throw new RuntimeException(e);
            }
            try {
                sslContext = SslContext.newServerContext(ssc.certificate(), ssc.privateKey());
            } catch (SSLException e) {
                throw new RuntimeException(e);
            }
        }

        serverBootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .option(ChannelOption.AUTO_READ, true)
                .channelFactory(NettyUtils.createServerChannelFactory())
                .childOption(ChannelOption.IP_TOS, 24)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.AUTO_READ, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new NettyServerChannelInitializer(this));

        Packery.LOGGER.info("Using " + (Epoll.isAvailable() ? "Epoll native transport" : "NIO transport"));
        Packery.LOGGER.info("Try to bind to " + nettyAddress.hostName() + ':' + nettyAddress.port() + "...");

        Future<Channel> channelFuture = serverBootstrap
                .bind(
                        nettyAddress.hostName(),
                        nettyAddress.port())
                .addListener(future -> {
                    if (future.isSuccess()) {
                        Packery.LOGGER.info(Packery.BRANDING + " is listening @" + nettyAddress.hostName() + ':' + nettyAddress.port());
                    } else {
                        Packery.LOGGER.info("Failed to bind @" + nettyAddress.hostName() + ':' + nettyAddress.port());
                        if (failRunnable != null) {
                            failRunnable.run();
                        }
                    }
                });

        new Thread(() -> {
            try {
                channelFuture.asStage().get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });

        return false;
    }

    @Override
    public void close() throws Exception {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
