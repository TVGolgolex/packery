package de.pascxl.packery.packet.defaults.relay;

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

import de.golgolex.quala.reflections.Allocator;
import de.pascxl.packery.Packery;
import de.pascxl.packery.buffer.ByteBuffer;
import de.pascxl.packery.network.ChannelIdentity;
import de.pascxl.packery.packet.PacketBase;
import lombok.Getter;
import lombok.NonNull;

import java.util.logging.Level;

@Getter
public class RoutingPacket extends PacketBase {

    private PacketBase packet;
    private ChannelIdentity to;

    public RoutingPacket(long packetId, @NonNull PacketBase packet, @NonNull ChannelIdentity to) {
        super(packetId);
        this.packet = packet;
        this.to = to;
    }

    @Override
    public void write(ByteBuffer out) {
        if (packet == null) {
            Packery.log(Level.SEVERE, this.getClass(), "Cannot send RelayPacket because Packet is null");
            return;
        }
        if (to == null) {
            Packery.log(Level.SEVERE, this.getClass(), "Cannot send RelayPacket because ChannelIdentity is null");
            return;
        }
        if (to.namespace() == null) {
            Packery.log(Level.SEVERE, this.getClass(), "Cannot send RelayPacket because ChannelIdentity#namespace is null");
            return;
        }
        if (to.uniqueId() == null) {
            Packery.log(Level.SEVERE, this.getClass(), "Cannot send RelayPacket because ChannelIdentity#uniqueId is null");
            return;
        }

        out.writeString(to.namespace())
                .writeUUID(to.uniqueId());
        out.writeString(packet.getClass().getName());
        packet.write(out);
    }

    @Override
    public void read(ByteBuffer in) {
        this.to = new ChannelIdentity(in.readString(), in.readUUID());
        try {
            var packetClass = Class.forName(in.readString());
            this.packet = (PacketBase) Allocator.unsafeAllocation(packetClass);

            if (packet == null) {
                Packery.log(Level.SEVERE, this.getClass(), "Packet cannot be allocated");
                return;
            }

            this.packet.read(in);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
