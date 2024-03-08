package de.pascxl.packery.client;

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
import de.pascxl.packery.network.codec.PacketInDecoder;
import de.pascxl.packery.network.codec.PacketOutEncoder;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.channel.ChannelInitializer;
import lombok.AllArgsConstructor;

import java.io.IOException;
import java.util.logging.Level;

@AllArgsConstructor
public class NettyClientChannelInitializer extends ChannelInitializer<Channel> {

    private final NettyClient client;

    @Override
    protected void initChannel(Channel ch) throws Exception {
        if (ch == null) {
            Packery.log(Level.SEVERE, this.getClass(), "Channel is null");
            return;
        }
        ch.pipeline().addLast(new PacketInDecoder(this.client.packetManager()), new PacketOutEncoder(this.client.packetManager()), new NettyClientHandler(client));
    }
}
