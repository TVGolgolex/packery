package de.pascxl.packery.network;

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

import de.pascxl.packery.packet.PacketBase;
import de.pascxl.packery.packet.sender.PacketSender;
import de.pascxl.packery.utils.ExecutionUtils;
import io.netty5.channel.Channel;
import lombok.Getter;
import lombok.Setter;

@Getter
public class NettyTransmitter extends PacketSender {

    private final NettyIdentity identity;
    @Setter
    private Channel channel;

    public NettyTransmitter(NettyIdentity identity, Channel channel) {
        this.identity = identity;
        this.channel = channel;
    }

    @Override
    public <P extends PacketBase> void sendPacketAsync(P packet) {
        if (!(channel != null && channel.isOpen())) {
            return;
        }
        ExecutionUtils.ASYNC_EXECUTOR.execute(() -> channel.writeAndFlush(packet));
    }

    @Override
    public <P extends PacketBase> void sendPacketSync(P packet) {
        if (!(channel != null && channel.isOpen())) {
            return;
        }
        ExecutionUtils.DIRECT_EXECUTOR.execute(() -> channel.writeAndFlush(packet));
    }
}