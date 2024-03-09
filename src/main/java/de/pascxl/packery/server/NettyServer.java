package de.pascxl.packery.server;

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

import de.pascxl.packery.Packery;
import de.pascxl.packery.packet.PacketManager;
import de.pascxl.packery.utils.NettyUtils;
import io.netty5.bootstrap.ServerBootstrap;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelOption;
import io.netty5.channel.EventLoopGroup;
import io.netty5.channel.MultithreadEventLoopGroup;
import io.netty5.channel.epoll.Epoll;
import io.netty5.handler.ssl.SslContext;
import io.netty5.handler.ssl.SslContextBuilder;
import io.netty5.handler.ssl.util.SelfSignedCertificate;
import io.netty5.util.concurrent.Future;
import lombok.Getter;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

@Getter
public class NettyServer implements AutoCloseable{

    protected final EventLoopGroup bossEventLoopGroup = new MultithreadEventLoopGroup(1, NettyUtils.createIoHandlerFactory());
    protected final EventLoopGroup workerEventLoopGroup = new MultithreadEventLoopGroup(1, NettyUtils.createIoHandlerFactory());
    protected final PacketManager packetManager;
    protected final NettyServerHandler nettyServerHandler;
    protected ServerBootstrap serverBootstrap;
    protected SslContext ssl;
    protected boolean connected = false;

    public NettyServer(PacketManager packetManager) {
        this.packetManager = packetManager;
        this.nettyServerHandler = new NettyServerHandler(this);
    }

    public NettyServer() {
        this.packetManager = new PacketManager();
        this.nettyServerHandler = new NettyServerHandler(this);
    }

    public boolean connect(String hostName, int port, boolean ssl) {
        if (ssl) {
            Packery.debug(Level.INFO, this.getClass(), "Enabling SSL Context for service requests");
            SelfSignedCertificate ssc;
            try {
                ssc = new SelfSignedCertificate();
            } catch (CertificateException e) {
                throw new RuntimeException(e);
            }
            try {
                this.ssl = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
            } catch (SSLException e) {
                throw new RuntimeException(e);
            }
        }

        this.serverBootstrap = new ServerBootstrap()
                .group(bossEventLoopGroup, workerEventLoopGroup)
                .channelFactory(NettyUtils.createServerChannelFactory())
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.AUTO_READ, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new NettyServerChannelInitializer(this, nettyServerHandler));

        Packery.debug(Level.INFO, this.getClass(), "Using " + (Epoll.isAvailable() ? "Epoll native transport" : "NIO transport"));

        var channelFuture = serverBootstrap
                .bind(
                        hostName,
                        port)
                .addListener(future -> {
                    if (future.isSuccess()) {
                        Packery.LOGGER.log(Level.INFO, Packery.BRANDING + " is listening @" + hostName + ":" + port);
                    } else {
                        Packery.LOGGER.log(Level.INFO, Packery.BRANDING + " failed while bind @" + hostName + ":" + port);
                    }
                });

        if (channelFuture.isFailed()) {
            return false;
        }

        new Thread(() -> {
            try {
                channelFuture.asStage().get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
        connected = true;
        return true;
    }

    @Override
    public void close() throws Exception {
        this.bossEventLoopGroup.shutdownGracefully();
        this.workerEventLoopGroup.shutdownGracefully();
    }
}