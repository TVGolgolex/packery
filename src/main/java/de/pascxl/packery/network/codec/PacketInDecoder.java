package de.pascxl.packery.network.codec;

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
import de.pascxl.packery.packet.PacketBase;
import de.pascxl.packery.packet.PacketManager;
import io.netty5.buffer.Buffer;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.handler.codec.ByteToMessageDecoder;
import lombok.AllArgsConstructor;

import java.util.logging.Level;

@AllArgsConstructor
public class PacketInDecoder extends ByteToMessageDecoder {

    private final PacketManager packetManager;

    @Override
    protected void decode(ChannelHandlerContext ctx, Buffer in) throws Exception {
        var byteBuffer = new ByteBuffer(in);
        Packery.debug(Level.INFO, this.getClass(), "Before read, length: {0}", in.readableBytes());

        try {
            var packetClassName = byteBuffer.readString();
            var packetClass = Class.forName(packetClassName);
            var packetInstance = (PacketBase) Allocator.unsafeAllocation(packetClass);

            if (packetInstance == null) {
                Packery.log(Level.SEVERE, this.getClass(), "PacketInstance is null");
                return;
            }

            var packetId = byteBuffer.readLong();
            var packetUUID = byteBuffer.readUUID();
            var seasonId = byteBuffer.readLong();

            Packery.debug(Level.INFO, this.getClass(), "decode: readLong: packetId " + packetClass.getSimpleName());
            packetInstance.packetId(packetId);

            if (!this.packetManager.isPacketAllow(packetInstance)) {
                Packery.log(Level.SEVERE, this.getClass(), "The channel {0} tries to send a packet which is not allowed: PacketId: {1}", ctx.channel().remoteAddress(), packetId);
                return;
            }

            Packery.debug(Level.INFO, this.getClass(), "decode: readUUID: uniqueId " + packetClass.getSimpleName());
            packetInstance.uniqueId((packetUUID.equals(Packery.SYSTEM_UUID) ? null : packetUUID));
            Packery.debug(Level.INFO, this.getClass(), "decode: readLong: seasonId " + packetClass.getSimpleName());
            packetInstance.seasonId(seasonId);
            Packery.debug(Level.INFO, this.getClass(), "decode: read: byteBuffer " + packetClass.getSimpleName());
            packetInstance.read(byteBuffer);
            ctx.fireChannelRead(packetInstance);
        } catch (Exception exception) {
            Packery.log(Level.SEVERE, this.getClass(), exception.getMessage());
        }

        Packery.debug(Level.INFO, this.getClass(), "After read, length: {0}", in.readableBytes());
    }
}
