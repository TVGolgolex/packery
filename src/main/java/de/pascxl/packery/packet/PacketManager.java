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

import com.github.golgolex.eventum.EventManager;
import de.pascxl.packery.Packery;
import de.pascxl.packery.events.PacketIdAllowEvent;
import de.pascxl.packery.events.PacketIdDisallowEvent;
import de.pascxl.packery.network.NettyIdentity;
import de.pascxl.packery.packet.listener.PacketReceiveListener;
import de.pascxl.packery.packet.request.PacketRequester;
import de.pascxl.packery.packet.sender.PacketSender;
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

    private final Map<Long, Collection<Class<? extends PacketReceiveListener<?>>>> packetHandlers = new ConcurrentHashMap<>(0);
    private final Collection<Long> allowedPacketIds = new ArrayList<>();
    private final PacketRequester packetRequester;

    public PacketManager() {
        this.packetRequester = new PacketRequester(this);
    }

    public <P extends PacketBase> boolean registerPacketHandler(long packetId, Class<? extends PacketReceiveListener<P>> handler) {
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
        int calledCount = 0;
        for (PacketReceiveListener<P> listener : this.collectHandlers(packet)) {
            calledCount++;
            if (packet.uniqueId() != null) {
                listener.uniqueId(packet.uniqueId());
            }
            listener.packetId(packet.packetId());
            listener.seasonId(packet.seasonId());
            listener.call(packet, packetSender, channelHandlerContext);
        }
        return calledCount;
    }

    public boolean isPacketAllow(PacketBase packetBase) {
        long packetId = packetBase.packetId();

        Packery.log(Level.INFO, this.getClass(), "Checking PacketId: {0}", packetId);

        if (packetId < 1 && packetId != -400 && packetId != -410) {
            Packery.log(Level.SEVERE, this.getClass(), "No packet IDs less than 1 are permitted: Requested: {0}", packetId);
            return false;
        }

        if (packetBase.uniqueId() != null && this.packetRequester.waiting().containsKey(packetBase.uniqueId())) {
            return true;
        }

        return packetId == -400 || packetId == -410 || this.allowedPacketIds.contains(packetId);
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

    public <P extends PacketBase> void call(P packet, PacketSender packetSender, ChannelHandlerContext channelHandlerContext, NettyIdentity authentication) {
        Packery.debug(Level.INFO, this.getClass(), "Received Packet [id=" + packet.packetId() + ";uuid=" + packet.uniqueId() + ";seasonId=" + packet.seasonId() + "] from " + authentication.namespace() + "#" + authentication.uniqueId());
        if (packet.uniqueId() != null && this.packetRequester.waiting().containsKey(packet.uniqueId())) {
            this.packetRequester.dispatch(packet);
        }
        callHandlers(packet, packetSender, channelHandlerContext);
    }

}
