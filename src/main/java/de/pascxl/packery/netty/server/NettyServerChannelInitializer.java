package de.pascxl.packery.netty.server;

import de.pascxl.packery.Packery;
import de.pascxl.packery.netty.codec.PacketDecoder;
import de.pascxl.packery.netty.codec.PacketEncoder;
import de.pascxl.packery.netty.transmitter.NettyTransmitterAuth;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelInitializer;
import lombok.AllArgsConstructor;

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
@AllArgsConstructor
public class NettyServerChannelInitializer extends ChannelInitializer<Channel> {

    private final NettyServer nettyServer;

    @Override
    protected void initChannel(Channel ch) throws Exception {
        Packery.LOGGER.info("Channel [" + ch.remoteAddress().toString() + "] is connected.");

        if (nettyServer.sslContext() != null) {
            ch.pipeline().addLast(nettyServer.sslContext().newHandler(ch.bufferAllocator()));
        }

        NettyTransmitterAuth nettyTransmitterAuth = new NettyTransmitterAuth(
                this.nettyServer,
                System.currentTimeMillis(),
                this.nettyServer.packetManager(),
                ch);

        ch.pipeline().addLast(
                        new PacketDecoder(this.nettyServer.packetManager()),
                        new PacketEncoder(this.nettyServer.packetManager()))
//                        new NettyServerHandler(this.nettyServer))
                .addLast("auth=" + ch.remoteAddress().toString(), nettyTransmitterAuth);
    }
}
