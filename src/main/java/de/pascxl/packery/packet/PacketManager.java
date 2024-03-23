package de.pascxl.packery.packet;

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
import de.pascxl.packery.internal.NettyPacketOutAuthentication;
import de.pascxl.packery.internal.NettyPacketOutChannelStayActive;
import de.pascxl.packery.internal.NettyPacketOutIdentityActive;
import de.pascxl.packery.internal.NettyPacketOutIdentityInactive;
import de.pascxl.packery.network.ChannelIdentity;
import de.pascxl.packery.packet.defaults.relay.RoutingResultReplyPacket;
import de.pascxl.packery.packet.defaults.request.RespondNettyPacket;
import de.pascxl.packery.packet.listener.PacketReceiveListener;
import de.pascxl.packery.packet.query.PacketQuery;
import de.pascxl.packery.packet.router.PacketRouter;
import de.pascxl.packery.packet.sender.PacketSender;
import de.pascxl.packery.utils.BypassCheck;
import io.netty5.channel.ChannelHandlerContext;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

@Getter
public class PacketManager {

    //    private final Map<Long, Collection<Class<? extends PacketReceiveListener<?>>>> packetHandlers = new ConcurrentHashMap<>(0);
    private final Map<String, Collection<Class<? extends PacketReceiveListener<?>>>> packetHandlers = new ConcurrentHashMap<>(0);
    //    private final Collection<Long> allowedPacketIds = new ArrayList<>();
    private final Collection<String> allowedPackets = new ArrayList<>();
    private final PacketQuery packetQuery;
    private final PacketRouter packetRouter;

    public PacketManager() {
        this.packetQuery = new PacketQuery(this);
        this.packetRouter = new PacketRouter();
        this.allowPacket(NettyPacketOutAuthentication.class);
        this.allowPacket(NettyPacketOutChannelStayActive.class);
        this.allowPacket(NettyPacketOutIdentityActive.class);
        this.allowPacket(NettyPacketOutIdentityInactive.class);

        this.allowPacket(RespondNettyPacket.class);
    }

    public <P extends NettyPacket> boolean registerPacketHandler(String packetId, Class<? extends PacketReceiveListener<P>> handler) {
        if (!this.packetHandlers.containsKey(packetId)) {
            this.packetHandlers.put(packetId, new ArrayList<>());
        }
        this.packetHandlers.get(packetId).add(handler);
        return true;
    }

    public <P extends NettyPacket> boolean unregisterPacketHandler(String packetId, Class<? extends PacketReceiveListener<P>> handler) {
        if (!this.packetHandlers.containsKey(packetId)) {
            return false;
        }
        Collection<Class<? extends PacketReceiveListener<?>>> handlers = this.packetHandlers.get(packetId);
        handlers.remove(handler);
        if (handlers.isEmpty()) {
            this.packetHandlers.remove(packetId);
        }
        return true;
    }

    public <P extends NettyPacket> Collection<PacketReceiveListener<P>> collectHandlers(P packet) {
        Collection<PacketReceiveListener<P>> handlers = new LinkedList<>();
        if (packetHandlers.containsKey(packet.getClass().getName())) {
            for (Class<? extends PacketReceiveListener<?>> aClass : packetHandlers.get(packet.getClass().getName())) {
                try {
                    handlers.add((PacketReceiveListener<P>) aClass.newInstance());
                } catch (InstantiationException | IllegalAccessException | ClassCastException exception) {
                    Packery.log(Level.SEVERE, this.getClass(), exception.getMessage());
                }
            }
        }
        return handlers;
    }

    public <P extends NettyPacket> int callHandlers(P packet, PacketSender packetSender, ChannelHandlerContext channelHandlerContext) {
        var calledCount = 0;
        for (var listener : this.collectHandlers(packet)) {
            calledCount++;
            if (packet.uniqueId() != null) {
                listener.uniqueId(packet.uniqueId());
            }
            listener.packetId(packet.getClass().getName());
            listener.call(packet, packetSender, channelHandlerContext);
        }
        return calledCount;
    }

    public void allowPacket(Class<? extends NettyPacket> clazz) {
        if (!this.allowedPackets.contains(clazz.getName())) {
            this.allowedPackets.add(clazz.getName());
        }
    }

    public void disAllowPacket(Class<? extends NettyPacket> clazz) {
        this.allowedPackets.remove(clazz.getName());
    }

    public <P extends NettyPacket> void call(P packet, PacketSender packetSender, ChannelHandlerContext channelHandlerContext, ChannelIdentity authentication) {
        Packery.debug(Level.INFO, this.getClass(), "Received Packet [Packet=" + packet.getClass().getName() + ";uuid=" +
                packet.uniqueId() + "] from " + authentication.namespace() + "#" + authentication.uniqueId());

        if (packet.uniqueId() != null) {
            if (this.packetQuery.waiting().containsKey(packet.uniqueId())) {
                this.packetQuery.dispatch(packet);
            }
            if (packet instanceof RoutingResultReplyPacket routingResultReplyPacket && this.packetQuery.waiting().containsKey(packet.uniqueId())) {
                this.packetRouter.dispatch(routingResultReplyPacket);
            }
        }

        callHandlers(packet, packetSender, channelHandlerContext);
    }

    public boolean isPacketAllow(NettyPacket packetBase) {
        var packetId = packetBase.getClass().getName();
        Packery.debug(Level.INFO, this.getClass(), "Checking PacketId: {0}", packetId);
        if (packetBase.uniqueId() != null && this.packetQuery.waiting().containsKey(packetBase.uniqueId())) {
            Packery.debug(Level.WARNING, this.getClass(), "Accepted {0} because Packet is in Query", packetId);
            return true;
        }

        var result = this.allowedPackets.contains(packetBase.getClass().getName());
        if (this.allowedPackets.contains(BypassCheck.class.getName())) {
            Packery.debug(Level.WARNING, this.getClass(), "Allowed all: bypass: {0}", packetId);
            result = true;
        }
        if (!result) {
            Packery.debug(Level.SEVERE, this.getClass(), "Packet check for: {0} is marked as not allowed!", packetId);
        } else {
            Packery.debug(Level.WARNING, this.getClass(), "Accepted {0}", packetId);
        }
        return result;
    }

/*    public <P extends PacketBase> boolean registerPacketHandler(long packetId, Class<? extends PacketReceiveListener<P>> handler) {
        if (!this.packetHandlers.containsKey(packetId)) {
            this.packetHandlers.put(packetId, new ArrayList<>());
        }
        this.packetHandlers.get(packetId).add(handler);
        return true;
    }

    public <P extends PacketBase> boolean unregisterPacketHandler(long packetId, Class<? extends PacketReceiveListener<P>> handler) {
        if (!this.packetHandlers.containsKey(packetId)) {
            return false;
        }
        Collection<Class<? extends PacketReceiveListener<?>>> handlers = this.packetHandlers.get(packetId);
        handlers.remove(handler);
        if (handlers.isEmpty()) {
            this.packetHandlers.remove(packetId);
        }
        return true;
    }

    public <P extends PacketBase> Collection<PacketReceiveListener<P>> collectHandlers(P packet) {
        Collection<PacketReceiveListener<P>> handlers = new LinkedList<>();
        if (packetHandlers.containsKey(packet.packetId())) {
            for (Class<? extends PacketReceiveListener<?>> aClass : packetHandlers.get(packet.packetId())) {
                try {
                    handlers.add((PacketReceiveListener<P>) aClass.newInstance());
                } catch (InstantiationException | IllegalAccessException | ClassCastException exception) {
                    Packery.log(Level.SEVERE, this.getClass(), exception.getMessage());
                }
            }
        }
        return handlers;
    }

    public <P extends PacketBase> int callHandlers(P packet, PacketSender packetSender, ChannelHandlerContext channelHandlerContext) {
        var calledCount = 0;
        for (var listener : this.collectHandlers(packet)) {
            calledCount++;
            if (packet.uniqueId() != null) {
                listener.uniqueId(packet.uniqueId());
            }
            listener.packetId(packet.packetId());
            listener.call(packet, packetSender, channelHandlerContext);
        }
        return calledCount;
    }

    public boolean isPacketAllow(PacketBase packetBase) {
        var packetId = packetBase.packetId();

        Packery.debug(Level.INFO, this.getClass(), "Checking PacketId: {0}", packetId);
        if (this.allowedPacketIds.contains(774090777346262697L)) {
            Packery.debug(Level.WARNING, this.getClass(), "Allowed all: bypass: {0}", packetId);
            return true;
        }

        if (packetId < 1
                && packetId != -400
                && packetId != -410
                && packetId != -411
                && packetId != -412) {
            Packery.log(Level.SEVERE, this.getClass(), "No packet IDs less than 1 are permitted: Requested: {0}", packetId);
            return false;
        }

        if (packetBase.uniqueId() != null && this.packetQuery.waiting().containsKey(packetBase.uniqueId())) {
            return true;
        }

        var result =
                packetId == -400
                || packetId == -410
                || packetId == -411
                || packetId == -412
                || this.allowedPacketIds.contains(packetId);

        if (!result) {
            Packery.log(Level.SEVERE, this.getClass(), "Packet check for: {0} is marked as not allowed!", packetId);
        }

        return result;
    }

    public void allowPacket(long id) {
        if (!this.allowedPacketIds.contains(id)) {
            this.allowedPacketIds.add(id);
            EventManager.call(new PacketIdAllowEvent(id));
        }
    }

    public void disAllowPacket(long id) {
        if (this.allowedPacketIds.contains(id)) {
            this.allowedPacketIds.remove(id);
            EventManager.call(new PacketIdDisallowEvent(id));
        }
    }

    public <P extends PacketBase> void call(P packet, PacketSender packetSender, ChannelHandlerContext channelHandlerContext, ChannelIdentity authentication) {
        Packery.debug(Level.INFO, this.getClass(), "Received Packet [id=" + packet.packetId() + ";uuid=" + packet.uniqueId() + ";seasonId=" + packet.seasonId() + "] from " + authentication.namespace() + "#" + authentication.uniqueId());

        if (packet.uniqueId() != null) {
            if (this.packetQuery.waiting().containsKey(packet.uniqueId())) {
                this.packetQuery.dispatch(packet);
            }
            if (packet instanceof RoutingResultReplyPacket routingResultReplyPacket && this.packetQuery.waiting().containsKey(packet.uniqueId())) {
                this.packetRouter.dispatch(routingResultReplyPacket);
            }
        }

        callHandlers(packet, packetSender, channelHandlerContext);
    }*/

}
