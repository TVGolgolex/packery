package de.pascxl.packery.client;

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
import de.pascxl.packery.network.InactiveAction;
import de.pascxl.packery.network.NettyIdentity;
import de.pascxl.packery.network.NettyTransmitter;
import de.pascxl.packery.packet.PacketManager;
import de.pascxl.packery.packet.defaults.auth.AuthPacket;
import de.pascxl.packery.utils.NettyUtils;
import io.netty5.bootstrap.Bootstrap;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelOption;
import io.netty5.channel.EventLoopGroup;
import io.netty5.channel.MultithreadEventLoopGroup;
import lombok.Getter;

import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

@Getter
public class NettyClient implements AutoCloseable {

    protected final EventLoopGroup eventLoopGroup = new MultithreadEventLoopGroup(2, NettyUtils.createIoHandlerFactory());
    protected final NettyIdentity account;
    protected final InactiveAction inactiveAction;
    protected final PacketManager packetManager;
    protected NettyTransmitter nettyTransmitter;
    protected Bootstrap bootstrap;
    protected boolean connected = false;

    public NettyClient(NettyIdentity account, InactiveAction inactiveAction, PacketManager packetManager) {
        this.account = account;
        this.inactiveAction = inactiveAction;
        this.packetManager = packetManager;
    }

    public NettyClient(NettyIdentity account, InactiveAction inactiveAction) {
        this.account = account;
        this.inactiveAction = inactiveAction;
        this.packetManager = new PacketManager();
    }

    public boolean connect(String hostName, int port, boolean ssl) {

        this.bootstrap = new Bootstrap()
                .group(eventLoopGroup)
                .channelFactory(NettyUtils.createChannelFactory())
                .option(ChannelOption.AUTO_READ, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new NettyClientChannelInitializer(this));

/*        this.bootstrap.connect(hostName, port)
                .addListener(future -> {
                    if (future.isSuccess()) {
                        Packery.LOGGER.log(Level.INFO, Packery.BRANDING + " Successfully connected @" + hostName + ":" + port);
                    } else {
                        Packery.LOGGER.log(Level.INFO, Packery.BRANDING + " failed while connecting @" + hostName + ":" + port);
                    }
                });*/

        InitThread initThread = new InitThread(this.bootstrap, hostName, port);
        Thread thread = new Thread(initThread);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            Packery.debug(Level.SEVERE, this.getClass(), e.getMessage());
            return false;
        }

        if (initThread.channel() == null) {
            Packery.debug(Level.SEVERE, this.getClass(), "Channel is null");
            return false;
        }

        this.nettyTransmitter = new NettyTransmitter(this.account, initThread.channel());

        this.nettyTransmitter.channel().writeAndFlush(new AuthPacket(this.account));
        connected = true;
        return true;
    }

    @Override
    public void close() throws Exception {
        this.eventLoopGroup.shutdownGracefully();
    }

    @Getter
    public static class InitThread implements Runnable {

        private final Bootstrap bootstrap;
        private final String host;
        private final int port;

        private Channel channel = null;

        public InitThread(Bootstrap bootstrap, String host, int port) {
            this.bootstrap = bootstrap;
            this.host = host;
            this.port = port;
        }

        @Override
        public void run() {
            try {
                this.channel = bootstrap.connect(host, port)
                        .addListener(future -> {
                            if (future.isSuccess()) {
                                Packery.LOGGER.log(Level.INFO, Packery.BRANDING + " successfully connected @" + host + ":" + port);
                            } else {
                                Packery.LOGGER.log(Level.INFO, Packery.BRANDING + " failed while connecting @" + host + ":" + port);
                            }
                        })
                        .asStage()
                        .get();
            } catch (InterruptedException | ExecutionException exception) {
                Packery.debug(Level.SEVERE, this.getClass(), exception.getMessage());
            }
        }
    }
}
