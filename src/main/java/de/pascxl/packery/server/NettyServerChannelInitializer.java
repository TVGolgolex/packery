package de.pascxl.packery.server;

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

import com.github.golgolex.eventum.EventManager;
import de.pascxl.packery.events.ChannelInitEvent;
import de.pascxl.packery.network.codec.PacketClassDecoder;
import de.pascxl.packery.network.codec.PacketClassEncoder;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelInitializer;

public class NettyServerChannelInitializer extends ChannelInitializer<Channel> {

    private final NettyServer server;
    private final NettyServerHandler nettyServerHandler;

    public NettyServerChannelInitializer(NettyServer server, NettyServerHandler nettyServerHandler) {
        this.server = server;
        this.nettyServerHandler = nettyServerHandler;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        this.nettyServerHandler.unauthenticated().add(ch);

        ch.pipeline().addLast(
                new PacketClassDecoder(this.server.packetManager, this.server.name),
                new PacketClassEncoder(this.server.packetManager, this.server.name),
                nettyServerHandler
        );

        EventManager.call(new ChannelInitEvent(ch));
    }
}
