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

import de.pascxl.packery.Packery;
import de.pascxl.packery.buffer.ByteBuffer;
import de.pascxl.packery.network.ChannelIdentity;
import lombok.Getter;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.logging.Level;

@Getter
public class PacketOutIdentityActive extends AbstractIdentityPacket {

    private Collection<ChannelIdentity> other;

    public PacketOutIdentityActive(@NonNull ChannelIdentity channelIdentity, @NonNull Collection<ChannelIdentity> other) {
        super(-410, channelIdentity);
        this.other = other;
    }

    public PacketOutIdentityActive(@NonNull ChannelIdentity channelIdentity) {
        super(-410, channelIdentity);
    }

    @Override
    public void writeCustom(ByteBuffer byteBuffer) {
        Packery.debug(Level.INFO, this.getClass(), "Other is empty: " + (other == null));
        byteBuffer.writeBoolean(other != null);
        if (other != null) {
            byteBuffer.writeCollectionString(other
                            .stream()
                            .map(ChannelIdentity::toString)
                            .toList()
            );
        }
    }

    @Override
    public void readCustom(ByteBuffer byteBuffer) {
        var state = byteBuffer.readBoolean();
        Packery.debug(Level.INFO, this.getClass(), "Other is empty: " + state);
        if (state) {
            this.other = byteBuffer.readCollectionString()
                    .stream()
                    .map(s -> {
                        String[] split = s.split("#");
                        return new ChannelIdentity(split[0], UUID.fromString(split[1]));
                    })
                    .toList();
        }
    }
}
