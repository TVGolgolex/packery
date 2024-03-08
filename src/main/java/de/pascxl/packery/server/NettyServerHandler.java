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

import com.google.gson.JsonElement;
import de.pascxl.packery.Packery;
import de.pascxl.packery.network.NettyTransmitter;
import de.pascxl.packery.packet.PacketBase;
import de.pascxl.packery.packet.PacketManager;
import de.pascxl.packery.packet.auth.AuthPacket;
import de.pascxl.packery.packet.document.JsonPacket;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.channel.SimpleChannelInboundHandler;
import lombok.Getter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

@Getter
public class NettyServerHandler extends SimpleChannelInboundHandler<PacketBase> {

    private final NettyServer server;
    private final List<NettyTransmitter> transmitters = new ArrayList<>();
    private final List<Channel> unauthenticated = new ArrayList<>();

    public NettyServerHandler(NettyServer server) {
        this.server = server;
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, PacketBase msg) throws Exception {
        Packery.debug(Level.INFO, this.getClass(), "messageReceived: " + msg.getClass().getSimpleName());

        if (msg instanceof AuthPacket authPacket) {
            if (unauthenticated.stream().anyMatch(channel -> channel.remoteAddress().equals(ctx.channel().remoteAddress()))) {
                unauthenticated.removeIf(channel -> channel.remoteAddress().equals(ctx.channel().remoteAddress()));
                transmitters.add(new NettyTransmitter(authPacket.nettyIdentity(), ctx.channel()));
                Packery.debug(Level.INFO, this.getClass(), "Created new Transmitter for " + authPacket.nettyIdentity().namespace() + "#" + authPacket.nettyIdentity().uniqueId() + "/" + ctx.channel().remoteAddress());
            }
            return;
        }

        this.transmitters
                .stream()
                .filter(nettyTransmitter -> nettyTransmitter.channel().remoteAddress().equals(ctx.channel().remoteAddress()))
                .findFirst()
                .ifPresentOrElse(nettyTransmitter -> {
                    this.server.packetManager.call(msg, nettyTransmitter, ctx, nettyTransmitter.identity());
                }, () -> {
                    Packery.debug(Level.SEVERE, this.getClass(), "No PacketSender found. Packet: " + msg.getClass().getSimpleName());
                });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if ((!ctx.channel().isActive() || !ctx.channel().isOpen() || !ctx.channel().isWritable())) {
            Packery.LOGGER.log(Level.INFO, "Channel inactive: " + ctx.channel().remoteAddress());
            unauthenticated.removeIf(channel -> channel.remoteAddress().equals(ctx.channel().remoteAddress()));
            ctx.close();
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Packery.debug(Level.INFO, this.getClass(), "Recache Channel: " + ctx.channel().remoteAddress());
        unauthenticated.removeIf(channel -> channel.remoteAddress().equals(ctx.channel().remoteAddress()));
        unauthenticated.add(ctx.channel());
    }

    @Override
    public void channelExceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (!(cause instanceof IOException)) {
            Packery.debug(Level.SEVERE, this.getClass(), cause.getMessage());
        }
    }

}
