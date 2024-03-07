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
import io.netty5.buffer.Buffer;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.handler.codec.ByteToMessageDecoder;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class PacketDecoder extends ByteToMessageDecoder {

    private final PacketManager packetManager;

    @Override
    protected void decode(ChannelHandlerContext ctx, Buffer in) throws Exception {

        if (!ctx.channel().isActive() || in.readableBytes() <= 0) {
            return;
        }

        String logId = ctx.channel().remoteAddress().toString() + ": ";

/*        if (in.readableBytes() < 4) {
            Packery.debug(this.getClass(), "[" + logId + "]" + "Decode not enough readableBytes(" + in.readableBytes() + ";channel=" + ctx.channel().remoteAddress().toString() + ")");
            return;
        }*/

        System.out.println(in.readableBytes());

        ByteBuffer byteBuffer = new ByteBuffer(in);

        int packetTypeId = byteBuffer.readInt();

        this.packetManager.packetTypes()
                .values()
                .stream()
                .filter(packetType -> packetType.typeId() == packetTypeId)
                .findFirst()
                .ifPresentOrElse(packetType -> {
                    long packetID = byteBuffer.readLong();
                    System.out.println(packetID);
                    DefaultPacket packetObject = this.packetManager.constructPacket(packetID);
                    packetObject.read(byteBuffer);
                    Packery.debug(this.getClass(), "Read Packet " + packetObject.packetId() + "/" + packetObject.uniqueId() + "/" + packetObject.seasonId());
                    ctx.fireChannelRead(packetObject);
                }, () -> {
                    Packery.debug(this.getClass(), logId + ": Cannot decode ");
                });
    }
}
