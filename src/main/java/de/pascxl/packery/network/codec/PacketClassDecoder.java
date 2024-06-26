package de.pascxl.packery.network.codec;

/*
 * MIT License
 *
 * Copyright (c) 2024 00:53 Mario Pascal K.
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
import de.pascxl.packery.packet.NettyPacket;
import de.pascxl.packery.packet.PacketManager;
import io.netty5.buffer.Buffer;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.handler.codec.ByteToMessageDecoder;
import lombok.AllArgsConstructor;

import java.util.logging.Level;

@AllArgsConstructor
public class PacketClassDecoder extends ByteToMessageDecoder {

    private final PacketManager packetManager;
    private final String providerName;

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, Buffer buffer) throws Exception {

        if (buffer.readableBytes() < 55) {
            Packery.debug(Level.SEVERE, this.getClass(), "Packet has not enough readable bytes (" + buffer.readableBytes() + ")");
            return;
        }

        var byteBuffer = new ByteBuffer(buffer);
        var packetClassName = byteBuffer.readString();
        try {
            Packery.debug(Level.INFO, this.getClass(), "Decode: " + packetClassName);
            var packetClass = Class.forName(packetClassName);
            var packet = (NettyPacket) Allocator.unsafeAllocation(packetClass);

            if (packet == null) {
                Packery.log(Level.SEVERE, this.getClass(), providerName + ":" + "PacketInstance is null");
                return;
            }

            var packetUUID = byteBuffer.readUUID();
            packet.uniqueId((packetUUID.equals(Packery.SYSTEM_UUID) ? null : packetUUID));

            if (!this.packetManager.isPacketAllow(packet)) {
                Packery.log(Level.SEVERE, this.getClass(), providerName + ": " + "The channel {0} tries to send a packet which is not allowed: PacketId: {1}", channelHandlerContext.channel().remoteAddress(), packet.getClass().getName());
                return;
            }

            packet.read(byteBuffer);
            Packery.debug(Level.INFO, this.getClass(), "Read Packet: " + packet.getClass().getName());
            channelHandlerContext.fireChannelRead(packet);
        } catch (Exception exception) {
            Packery.log(Level.SEVERE, this.getClass(), exception.getMessage());
        }
        buffer.resetOffsets();

    }
}
