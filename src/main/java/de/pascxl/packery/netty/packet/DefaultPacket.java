package de.pascxl.packery.netty.packet;

import de.pascxl.packery.Packery;
import de.pascxl.packery.netty.buffer.ByteBuffer;
import de.pascxl.packery.netty.io.Decoder;
import de.pascxl.packery.netty.io.Encoder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

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
@Getter
public abstract class DefaultPacket implements Decoder, Encoder {

    protected long packetId = -500;
    @Setter
    protected UUID uniqueId = null;
    protected long seasonId = System.currentTimeMillis();

    public DefaultPacket() {
    }

    public DefaultPacket(int packetId) {
        this.packetId = packetId;
    }

    public DefaultPacket(int packetId, UUID uniqueId) {
        this.packetId = packetId;
        this.uniqueId = uniqueId;
    }

    @Override
    public void read(ByteBuffer in) {
        this.packetId = in.readLong();
        UUID readUUID = in.readUUID();
        this.uniqueId = (readUUID == Packery.SYSTEM_UUID ? null : readUUID);
        this.seasonId = in.readLong();
        this.readBuffer(in);
    }

    @Override
    public void write(ByteBuffer out) {
        out.writeLong(this.packetId);
        out.writeUUID(this.uniqueId == null ? Packery.SYSTEM_UUID : this.uniqueId);
        out.writeLong(this.seasonId);
        this.writeBuffer(out);
    }

    public abstract void readBuffer(ByteBuffer in);

    public abstract void writeBuffer(ByteBuffer out);
}
