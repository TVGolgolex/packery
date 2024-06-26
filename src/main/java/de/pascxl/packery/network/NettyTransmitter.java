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

import de.golgolex.quala.utils.executors.ExecutionUtils;
import de.pascxl.packery.Packery;
import de.pascxl.packery.packet.NettyPacket;
import de.pascxl.packery.packet.sender.PacketSender;
import io.netty5.channel.Channel;
import lombok.Getter;
import lombok.Setter;

import java.util.logging.Level;

@Getter
public class NettyTransmitter extends PacketSender {

    private final ChannelIdentity channelIdentity;
    @Setter
    private Channel channel;

    public NettyTransmitter(ChannelIdentity channelIdentity, Channel channel) {
        this.channelIdentity = channelIdentity;
        this.channel = channel;
    }

    @Override
    public <P extends NettyPacket> void writePacket(P packet)
    {
        if (!(channel != null && channel.isOpen())) {
            Packery.log(Level.SEVERE, this.getClass(), "Channel is as null or as not open marked");
            return;
        }
        channel.write(packet);
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Packery.log(Level.SEVERE, this.getClass(), e.getMessage());
        }
        Packery.debug(Level.INFO, this.getClass(), "writePacket: write: " + packet.getClass().getSimpleName());
    }

    @Override
    public void flush()
    {
        if (!(channel != null && channel.isOpen())) {
            Packery.log(Level.SEVERE, this.getClass(), "Channel is as null or as not open marked");
            return;
        }
        channel.flush();
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Packery.log(Level.SEVERE, this.getClass(), e.getMessage());
        }
        Packery.debug(Level.INFO, this.getClass(), "flush");
    }

    @Override
    public <P extends NettyPacket> void sendPacketAsync(P packet) {
        if (!(channel != null && channel.isOpen())) {
            Packery.log(Level.SEVERE, this.getClass(), "Channel is as null or as not open marked");
            return;
        }
        ExecutionUtils.ASYNC_EXECUTOR.execute(() -> {
            channel.writeAndFlush(packet);
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Packery.log(Level.SEVERE, this.getClass(), e.getMessage());
            }
            Packery.debug(Level.INFO, this.getClass(), "sendPacketAsync: writeAndFlush: " + packet.getClass().getSimpleName());
        });
    }

    @Override
    public <P extends NettyPacket> void sendPacket(P packet) {
        if (!(channel != null && channel.isOpen())) {
            Packery.log(Level.SEVERE, this.getClass(), "Channel is as null or as not open marked");
            return;
        }
        channel.writeAndFlush(packet);
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Packery.log(Level.SEVERE, this.getClass(), e.getMessage());
        }
        Packery.debug(Level.INFO, this.getClass(), "sendPacket: writeAndFlush: " + packet.getClass().getSimpleName());
    }

    @Override
    public <P extends NettyPacket> void sendPacketSync(P packet) {
        if (!(channel != null && channel.isOpen())) {
            Packery.log(Level.SEVERE, this.getClass(), "Channel is as null or as not open marked");
            return;
        }
        ExecutionUtils.DIRECT_EXECUTOR.execute(() -> {
            channel.writeAndFlush(packet);
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Packery.log(Level.SEVERE, this.getClass(), e.getMessage());
            }
            Packery.debug(Level.INFO, this.getClass(), "sendPacketSync: writeAndFlush: " + packet.getClass().getSimpleName());
        });
    }
}
