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

import de.golgolex.quala.ConsoleColor;
import de.golgolex.quala.scheduler.Scheduler;
import de.golgolex.quala.utils.string.StringUtils;
import de.pascxl.packery.Packery;
import de.pascxl.packery.internal.NettyPacketOutChannelStayActive;
import de.pascxl.packery.network.InactiveAction;
import de.pascxl.packery.network.ChannelIdentity;
import de.pascxl.packery.network.NettyTransmitter;
import de.pascxl.packery.packet.PacketManager;
import de.pascxl.packery.utils.NettyUtils;
import io.netty5.bootstrap.Bootstrap;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelOption;
import io.netty5.channel.EventLoopGroup;
import io.netty5.channel.MultithreadEventLoopGroup;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

@Getter
public class NettyClient implements AutoCloseable {

    protected final EventLoopGroup eventLoopGroup = new MultithreadEventLoopGroup(2, NettyUtils.createIoHandlerFactory());
    protected final List<ChannelIdentity> channelIdentities = new ArrayList<>();
    protected final ChannelIdentity channelIdentity;
    protected final InactiveAction inactiveAction;
    protected final PacketManager packetManager;
    @Setter
    protected String name;
    protected NettyTransmitter nettyTransmitter;
    protected Bootstrap bootstrap;
    protected boolean connected = false;
    protected boolean stayActive = false;

    public NettyClient(@NonNull ChannelIdentity channelIdentity, @NonNull InactiveAction inactiveAction, @NonNull PacketManager packetManager) {
        this.inactiveAction = inactiveAction;
        this.packetManager = packetManager;

        ChannelIdentity finalIdentity = channelIdentity;
        if (channelIdentity.namespace() == null || channelIdentity.namespace().isEmpty()) {
            finalIdentity = new ChannelIdentity(StringUtils.generateRandomString(8), channelIdentity.uniqueId());
        }
        if (channelIdentity.uniqueId() == null) {
            finalIdentity = new ChannelIdentity(finalIdentity.namespace(), UUID.randomUUID());
        }
        this.channelIdentity = finalIdentity;
    }

    public NettyClient(@NonNull ChannelIdentity channelIdentity, @NonNull InactiveAction inactiveAction) {
        this.inactiveAction = inactiveAction;
        this.packetManager = new PacketManager();

        ChannelIdentity finalIdentity = channelIdentity;
        if (channelIdentity.namespace() == null || channelIdentity.namespace().isEmpty()) {
            finalIdentity = new ChannelIdentity(StringUtils.generateRandomString(8), channelIdentity.uniqueId());
        }
        if (channelIdentity.uniqueId() == null) {
            finalIdentity = new ChannelIdentity(finalIdentity.namespace(), UUID.randomUUID());
        }
        this.channelIdentity = finalIdentity;
    }

    public boolean connect(@NonNull String hostName, int port, boolean ssl) {
        this.bootstrap = new Bootstrap()
                .group(eventLoopGroup)
                .channelFactory(NettyUtils.createChannelFactory())
                .option(ChannelOption.AUTO_READ, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new NettyClientChannelInitializer(this));

        if (name == null) {
            name = StringUtils.generateRandomString(8);
            Packery.log(Level.INFO, this.getClass(), ConsoleColor.YELLOW.ansiCode() + "NettyClient (" + hostName + ":" + port + ") has no specific name. (Generated: " + this.name + ")");
        }

        var initThread = new InitThread(this.bootstrap, hostName, port);
        var thread = new Thread(initThread);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            Packery.debug(Level.SEVERE, this.getClass(), e.getMessage());
            return false;
        }

        if (initThread.channel() == null) {
            Packery.debug(Level.SEVERE, this.getClass(), ConsoleColor.RED.ansiCode() + "Channel is null");
            return false;
        }

        connected = true;
        return true;
    }

    public void stayActive() {
        if (stayActive) {
            Packery.log(Level.WARNING, "StayActive is already enabled");
            return;
        }
        Scheduler.runtimeScheduler().schedule(() -> nettyTransmitter.sendPacket(new NettyPacketOutChannelStayActive()), 60000, 60000);
    }

    @Override
    public void close() throws Exception {
        this.eventLoopGroup.shutdownGracefully();
    }

    @Getter
    public class InitThread implements Runnable {

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
                                Packery.log(Level.INFO, ConsoleColor.GREEN.ansiCode() + "Successfully connecting to " + NettyClient.this.name + " @" + host + ":" + port);
                            } else {
                                Packery.log(Level.INFO, ConsoleColor.RED.ansiCode() + "Failed while connecting to " + NettyClient.this.name + " @" + host + ":" + port);
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
