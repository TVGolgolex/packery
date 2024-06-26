package de.pascxl.packery.internal;

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

import de.pascxl.packery.buffer.ByteBuffer;
import de.pascxl.packery.network.ChannelIdentity;
import de.pascxl.packery.packet.NettyPacket;
import lombok.Getter;
import lombok.NonNull;

@Getter
public abstract class AbstractIdentityNettyPacket extends NettyPacket {

    private ChannelIdentity channelIdentity;

    public AbstractIdentityNettyPacket(@NonNull ChannelIdentity channelIdentity) {
        this.channelIdentity = channelIdentity;
    }

    @Override
    public void write(ByteBuffer out) {
        out.writeString(channelIdentity.namespace()).writeUUID(channelIdentity.uniqueId());
        writeCustom(out);
    }

    @Override
    public void read(ByteBuffer in) {
        this.channelIdentity = new ChannelIdentity(in.readString(), in.readUUID());
        readCustom(in);
    }

    public abstract void writeCustom(ByteBuffer byteBuffer);

    public abstract void readCustom(ByteBuffer byteBuffer);
}
