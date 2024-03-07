package de.pascxl.packery.netty.codec;

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
import de.pascxl.packery.netty.buffer.ByteBuffer;
import de.pascxl.packery.netty.packet.DefaultPacket;
import de.pascxl.packery.netty.packet.PacketManager;
import de.pascxl.packery.netty.packet.PacketType;
import de.pascxl.packery.netty.packet.unknown.EncodingPacket;
import io.netty5.buffer.Buffer;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.handler.codec.MessageToByteEncoder;
import lombok.AllArgsConstructor;

import java.util.logging.Level;

@AllArgsConstructor
public class PacketEncoder extends MessageToByteEncoder {

    private final PacketManager packetManager;

    @Override
    protected Buffer allocateBuffer(ChannelHandlerContext ctx, Object msg) throws Exception {
        return ctx.bufferAllocator().allocate(0);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, Buffer out) throws Exception {
        ByteBuffer byteBuffer = new ByteBuffer(out);

        if (msg instanceof EncodingPacket encodingPacket) {

            PacketType<?> packetType = this.packetManager.getType(encodingPacket.typeId());
            if (packetType == null) {
                Packery.debug(this.getClass(), "PacketType is not registered");
                return;
            }

            DefaultPacket defaultPacket = (DefaultPacket) packetType.build(encodingPacket.packetObject());
            if (defaultPacket == null) {
                Packery.debug(this.getClass(), "Packet cannot build by object: " + encodingPacket.packetObject().getClass().getSimpleName());
                return;
            }

            if (defaultPacket.packetId() < 0) {
                Packery.LOGGER.log(Level.SEVERE, "PacketID is under 0");
                return;
            }

            byteBuffer.writeInt(packetType.typeId());
            defaultPacket.write(byteBuffer);
            Packery.debug(this.getClass(), "Write Packet " + encodingPacket.packetObject().getClass().getSimpleName());
            return;
        }

        if (!(msg instanceof DefaultPacket unknown)) {
            Packery.debug(this.getClass(), "Cannot encode " + msg.getClass().getSimpleName() + " it's not a packet");
            return;
        }

        this.packetManager.packetTypes()
                .values()
                .stream()
                .filter(packetType -> packetType.isPacket(msg))
                .findFirst()
                .ifPresentOrElse(packetType -> {
                    DefaultPacket defaultPacket = (DefaultPacket) packetType.build(msg);
                    byteBuffer.writeInt(packetType.typeId());
                    byteBuffer.writeLong(defaultPacket.packetId());
                    defaultPacket.write(byteBuffer);
                    Packery.debug(this.getClass(), "Write Packet " + msg.getClass().getSimpleName());
                }, () -> {
                    Packery.debug(this.getClass(), "Cannot encode " + msg.getClass().getSimpleName());
                });

    }
}
