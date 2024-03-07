package de.pascxl.packery.netty.client;

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

import de.pascxl.packery.netty.codec.PacketDecoder;
import de.pascxl.packery.netty.codec.PacketEncoder;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.channel.ChannelInitializer;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class NettyClientChannelInitializer extends ChannelInitializer<Channel> {

    private final NettyClient nettyClient;

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        switch (nettyClient.inactiveAction()) {
            case SHUTDOWN -> {
                System.exit(0);
            }
            case TRY_RECONNECT -> {
                this.nettyClient.nettyTransmitter().channel(null);
            }
        }
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        if (nettyClient.sslContext() != null) {
            ch.pipeline().addLast(nettyClient.sslContext().newHandler(ch.bufferAllocator(), nettyClient.nettyAddress().hostName(), nettyClient.nettyAddress().port()));
        }
        ch.pipeline().addLast(
                new PacketDecoder(this.nettyClient.packetManager()),
                new PacketEncoder(this.nettyClient.packetManager()),
                this.nettyClient.nettyTransmitter());
    }
}
