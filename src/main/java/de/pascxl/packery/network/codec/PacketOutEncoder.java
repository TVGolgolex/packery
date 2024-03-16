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

import de.pascxl.packery.Packery;
import de.pascxl.packery.buffer.ByteBuffer;
import de.pascxl.packery.packet.PacketBase;
import de.pascxl.packery.packet.PacketManager;
import io.netty5.buffer.Buffer;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.handler.codec.MessageToByteEncoder;
import lombok.AllArgsConstructor;

import java.util.logging.Level;

@AllArgsConstructor
public class PacketOutEncoder extends MessageToByteEncoder<PacketBase> {

    private final PacketManager packetManager;

    @Override
    protected Buffer allocateBuffer(ChannelHandlerContext ctx, PacketBase msg) throws Exception {
        return ctx.bufferAllocator().allocate(0);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, PacketBase msg, Buffer out) throws Exception {
        var byteBuffer = new ByteBuffer(out);

        if (!this.packetManager.isPacketAllow(msg)) {
            Packery.log(Level.SEVERE, this.getClass(), "The channel {0} tries to send a packet which is not allowed: PacketId: {1}", ctx.channel().remoteAddress(), msg.packetId());
            return;
        }

        byteBuffer.writeString(msg.getClass().getName());
        Packery.debug(Level.INFO, this.getClass(), "encode: writeString: className " + msg.getClass().getSimpleName());
        byteBuffer.writeLong(msg.packetId());
        Packery.debug(Level.INFO, this.getClass(), "encode: writeLong: packetId " + msg.getClass().getSimpleName());
        byteBuffer.writeUUID(msg.uniqueId() == null ? Packery.SYSTEM_UUID : msg.uniqueId());
        Packery.debug(Level.INFO, this.getClass(), "encode: writeUUID: uniqueId " + msg.getClass().getSimpleName());
        byteBuffer.writeLong(msg.seasonId());
        Packery.debug(Level.INFO, this.getClass(), "encode: writeLong: seasonId " + msg.getClass().getSimpleName());
        msg.write(byteBuffer);
        Packery.debug(Level.INFO, this.getClass(), "encode: write " + msg.getClass().getSimpleName());
    }
}
