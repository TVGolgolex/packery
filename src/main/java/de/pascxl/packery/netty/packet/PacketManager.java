package de.pascxl.packery.netty.packet;

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
import de.pascxl.packery.netty.packet.auth.AuthPacketType;
import de.pascxl.packery.netty.packet.auth.Authentication;
import de.pascxl.packery.netty.packet.document.DocumentPacketType;
import de.pascxl.packery.netty.packet.file.FilePacketType;
import de.pascxl.packery.netty.packet.query.QuerySender;
import io.netty5.channel.ChannelHandlerContext;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class PacketManager {

    private final Map<Long, Collection<Class<? extends PacketInHandler<?>>>> packetHandlers = new ConcurrentHashMap<>(0);
    private final Map<Integer, PacketType<?>> packetTypes = new ConcurrentHashMap<>(0);
    private final Collection<Long> allowedPacketIds = new ArrayList<>();
    private final QuerySender querySender;

    public PacketManager() {
        this.querySender = new QuerySender(this);
        this.registerPacketType(new DocumentPacketType(1));
        this.registerPacketType(new FilePacketType(2));
        this.registerPacketType(new AuthPacketType());
    }

    public boolean registerPacketType(PacketType<?> type) {
        if (this.packetTypes.containsKey(type.typeId())) {
            Packery.LOGGER.info("PacketType " + type.typeId() + " is already registered");
            return false;
        }
        this.packetTypes.put(type.typeId(), type);
        return true;
    }

    public boolean unregisterPacketType(int typeId) {
        if (!this.packetTypes.containsKey(typeId)) {
            Packery.LOGGER.info("PacketType " + typeId + " is not registered");
            return false;
        }
        this.packetTypes.remove(typeId);
        return true;
    }

    public <P extends PacketType<P>> P getType(int typeId) {
        try {
            return (P) this.packetTypes.get(typeId);
        } catch (ClassCastException exception) {
            Packery.LOGGER.info("PacketType " + typeId + " cannot be cast");
            return null;
        }
    }

    public boolean isRegisteredType(int typeId) {
        return this.packetTypes.containsKey(typeId);
    }

    public <P extends DefaultPacket> boolean isRegisteredType(P packet) {
        for (PacketType<?> value : this.packetTypes.values()) {
            if (value.validClasses().contains(packet.getClass())) {
                return true;
            }
        }
        return false;
    }

    /* ========================================================== */

    public void enablePacketId(long... ids) {
        for (long id : ids) {
            if (this.allowedPacketIds.contains(id)) {
                continue;
            }
            this.allowedPacketIds.add(id);
        }
    }

    public void removeEnablePacketId(long... ids) {
        for (long id : ids) {
            if (!this.allowedPacketIds.contains(id)) {
                continue;
            }
            this.allowedPacketIds.remove(id);
        }
    }

    public boolean isRegisteredPacketId(long id) {
        return this.allowedPacketIds.contains(id);
    }

    /* ========================================================== */

    public <P extends DefaultPacket> boolean registerPacketHandler(long packetId, Class<? extends PacketInHandler<P>> handler) {
        if (!this.packetHandlers.containsKey(packetId)) {
            this.packetHandlers.put(packetId, new ArrayList<>());
        }
        this.packetHandlers.get(packetId).add(handler);
        return true;
    }

    public <P extends DefaultPacket> boolean unregisterPacketHandler(long packetId, Class<? extends PacketInHandler<P>> handler) {
        if (!this.packetHandlers.containsKey(packetId)) {
            return false;
        }
        Collection<Class<? extends PacketInHandler<?>>> handlers = this.packetHandlers.get(packetId);
        handlers.remove(handler);
        if (handlers.isEmpty()) {
            this.packetHandlers.remove(packetId);
        }
        return true;
    }

    public <P extends DefaultPacket> Collection<PacketInHandler<P>> collectHandlers(P packet) {
        Collection<PacketInHandler<P>> handlers = new LinkedList<>();
        if (packetHandlers.containsKey(packet.packetId())) {
            for (Class<? extends PacketInHandler<?>> aClass : packetHandlers.get(packet.packetId())) {
                try {
                    handlers.add((PacketInHandler<P>) aClass.newInstance());
                } catch (InstantiationException | IllegalAccessException | ClassCastException exception) {
                    Packery.debug(this.getClass(), exception.getMessage());
                }
            }
        }
        return handlers;
    }

    public <P extends DefaultPacket> int callHandlers(P packet, PacketSender packetSender, ChannelHandlerContext channelHandlerContext) {
        int calledCount = 0;
        for (PacketInHandler<P> collectHandler : this.collectHandlers(packet)) {
            calledCount++;
            if (packet.uniqueId != null) {
                collectHandler.uniqueId = packet.uniqueId();
            }
            collectHandler.packetId = packet.packetId();
            collectHandler.seasonId = packet.seasonId();
            collectHandler.in(packet, packetSender, channelHandlerContext);
        }
        return calledCount;
    }

    /* ========================================================== */

    public <P extends DefaultPacket> void call(P packet, PacketSender packetSender, ChannelHandlerContext channelHandlerContext, Authentication authentication) {
        Packery.debug(this.getClass(), "Received Packet [id=" + packet.packetId() + ";uuid=" + packet.uniqueId() + ";seasonId=" + packet.seasonId() + "] from " + authentication.namespace() + "#" + authentication.uniqueId());
        if (packet.uniqueId() != null && querySender.waiting().containsKey(packet.uniqueId())) {
            querySender.dispatch(packet);
        }
        callHandlers(packet, packetSender, channelHandlerContext);
    }

}
