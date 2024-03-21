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

import de.golgolex.quala.scheduler.Scheduler;
import de.pascxl.packery.Packery;
import de.pascxl.packery.internal.PacketOutAuthentication;
import de.pascxl.packery.internal.PacketOutIdentityActive;
import de.pascxl.packery.internal.PacketOutIdentityInactive;
import de.pascxl.packery.network.NettyTransmitter;
import de.pascxl.packery.packet.PacketBase;
import de.pascxl.packery.packet.defaults.relay.RoutingPacket;
import de.pascxl.packery.packet.defaults.relay.RoutingResultReplyPacket;
import de.pascxl.packery.packet.router.RoutingResult;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.channel.SimpleChannelInboundHandler;
import lombok.Getter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

        if (msg instanceof PacketOutAuthentication authPacket) {
            if (unauthenticated.stream().anyMatch(channel -> channel.remoteAddress().equals(ctx.channel().remoteAddress()))) {
                unauthenticated.removeIf(channel -> channel.remoteAddress().equals(ctx.channel().remoteAddress()));
                var authenticatedTransmitter = new NettyTransmitter(authPacket.channelIdentity(), ctx.channel());

                var otherIdentities = this.transmitters.stream().map(NettyTransmitter::channelIdentity).toList();
                Packery.debug(Level.INFO, this.getClass(), "Sending " + otherIdentities.size() + " to " + authPacket.channelIdentity());
                ctx.channel().writeAndFlush(new PacketOutIdentityActive(authPacket.channelIdentity(), new ArrayList<>(otherIdentities)));

                for (var transmitter : this.transmitters) {
                    transmitter.sendPacketSync(new PacketOutIdentityActive(authPacket.channelIdentity()));
                    Packery.debug(Level.INFO, this.getClass(), "Sending new Id to " + transmitter.channelIdentity());
                }

                transmitters.add(authenticatedTransmitter);
                Packery.debug(Level.INFO, this.getClass(), "Created new Transmitter for " + authPacket.channelIdentity() + ctx.channel().remoteAddress());
            }
            return;
        }

        if (msg instanceof RoutingPacket routingPacket) {
            Packery.debug(Level.INFO, this.getClass(), "Received RelayPacket: " + routingPacket.getClass().getSimpleName() + " to: " + routingPacket.to() + " Transmitters: " + this.transmitters.size());

            Scheduler.runtimeScheduler().schedule(() -> {
                transmitters.stream()
                        .filter(transmitter -> transmitter.channelIdentity().equals(routingPacket.to()))
                        .findFirst()
                        .ifPresentOrElse(transmitter -> {
                            Packery.debug(Level.INFO, this.getClass(), "Send Packet RelayPacket: " + routingPacket.getClass().getSimpleName());
                            transmitter.sendPacketSync(routingPacket.packet());
                            ctx.channel().writeAndFlush(
                                    new RoutingResultReplyPacket(routingPacket.packetId(), routingPacket.uniqueId(), RoutingResult.SUCCESS));
                        }, () -> {
                            Packery.debug(Level.INFO, this.getClass(), "No Channel with Id: " + routingPacket.to() + " found.");
                            ctx.channel().writeAndFlush(
                                    new RoutingResultReplyPacket(routingPacket.packetId(), routingPacket.uniqueId(), RoutingResult.FAILED_NO_CLIENT));
                        });
            }, 100);

            return;
        }

        this.transmitters
                .stream()
                .filter(transmitters -> transmitters
                        .channel()
                        .remoteAddress()
                        .equals(ctx
                                .channel()
                                .remoteAddress()))
                .findFirst()
                .ifPresentOrElse(
                        transmitter -> this.server.packetManager.call(msg, transmitter, ctx, transmitter.channelIdentity()),
                        () -> Packery.debug(Level.SEVERE, this.getClass(), "No PacketSender found. Packet: " + msg.getClass().getSimpleName())
                );
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if ((!ctx.channel().isActive() || !ctx.channel().isOpen() || !ctx.channel().isWritable())) {
            Packery.log(Level.INFO, "Channel inactive: " + ctx.channel().remoteAddress());
            unauthenticated.removeIf(channel -> channel.remoteAddress().equals(ctx.channel().remoteAddress()));
            for (var transmitter : transmitters) {
                if (!transmitter.channel().remoteAddress().equals(ctx.channel().remoteAddress())) {
                    transmitter.sendPacketSync(new PacketOutIdentityInactive(transmitter.channelIdentity()));
                    Packery.debug(Level.INFO, this.getClass(), "Send PacketOutIdentityInit for " + ctx.channel().remoteAddress() + " to " + transmitter.channelIdentity().namespace() + "#" + transmitter.channelIdentity().uniqueId());
                }
            }
            transmitters.removeIf(nettyTransmitter -> nettyTransmitter.channel().remoteAddress().equals(ctx.channel().remoteAddress()));
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
            Packery.debug(Level.SEVERE, this.getClass(), "Channel: " + ctx.channel().remoteAddress() + " exception caught: " + cause.fillInStackTrace().getMessage());
        }
    }

}
