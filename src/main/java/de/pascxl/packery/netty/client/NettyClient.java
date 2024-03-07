package de.pascxl.packery.netty.client;

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

import de.pascxl.packery.NettyAddress;
import de.pascxl.packery.Packery;
import de.pascxl.packery.netty.NettyUtils;
import de.pascxl.packery.netty.packet.PacketManager;
import de.pascxl.packery.netty.packet.auth.AuthPacket;
import de.pascxl.packery.netty.packet.auth.Authentication;
import de.pascxl.packery.netty.transmitter.NettyTransmitter;
import io.netty5.bootstrap.Bootstrap;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelOption;
import io.netty5.channel.MultithreadEventLoopGroup;
import io.netty5.handler.ssl.SslContext;
import io.netty5.handler.ssl.SslContextBuilder;
import io.netty5.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.Getter;

import javax.net.ssl.SSLException;
import java.util.concurrent.ExecutionException;

@Getter
public class NettyClient implements AutoCloseable {

    private final MultithreadEventLoopGroup eventLoopGroup = new MultithreadEventLoopGroup(1, NettyUtils.createIoHandlerFactory());
    private final Authentication authentication;
    private final NettyAddress nettyAddress;
    private final PacketManager packetManager;
    private final InactiveAction inactiveAction;
    private NettyTransmitter nettyTransmitter;
    private Bootstrap bootstrap;
    private SslContext sslContext;

    public NettyClient(Authentication authentication, NettyAddress nettyAddress, InactiveAction inactiveAction) {
        this.authentication = authentication;
        this.nettyAddress = nettyAddress;
        this.inactiveAction = inactiveAction;
        this.packetManager = new PacketManager();
    }

    public boolean connect(boolean ssl, Runnable connectedRunnable) {
        try {
            if (ssl) {
                try {
                    sslContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
                } catch (SSLException e) {
                    Packery.debug(this.getClass(), e.getMessage());
                    return false;
                }
            }

            bootstrap = new Bootstrap()
                    .group(eventLoopGroup)
                    .channelFactory(NettyUtils.createChannelFactory())
                    .handler(new NettyClientChannelInitializer(this))
                    .option(ChannelOption.AUTO_READ, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_KEEPALIVE, true);

            InitThread initThread = new InitThread(bootstrap, this);
            Thread thread = new Thread(initThread);
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                Packery.debug(this.getClass(), e.getMessage());
                return false;
            }

            if (initThread.channel() == null) {
                Packery.debug(this.getClass(), "Channel is null");
                return false;
            }
            this.nettyTransmitter = new NettyTransmitter(authentication, this.packetManager, System.currentTimeMillis(), initThread.channel());
            this.nettyTransmitter.sendSynchronized(new AuthPacket(authentication));

            if (connectedRunnable != null) {
                connectedRunnable.run();
            }
            return true;
        } catch (Exception exception) {
            Packery.debug(this.getClass(), exception.getMessage());
        }
        return false;
    }

    @Override
    public void close() throws Exception {
        eventLoopGroup.shutdownGracefully();
    }

    @Getter
    public static class InitThread implements Runnable {

        private final Bootstrap bootstrap;
        private final NettyClient client;

        private Channel channel = null;

        public InitThread(Bootstrap bootstrap, NettyClient client) {
            this.bootstrap = bootstrap;
            this.client = client;
        }

        @Override
        public void run() {
            try {
                this.channel = bootstrap.connect(client.nettyAddress().hostName(),
                        client.nettyAddress.port()).asStage().get();
            } catch (InterruptedException | ExecutionException exception) {
                Packery.debug(this.getClass(), exception.getMessage());
            }
        }
    }
}
