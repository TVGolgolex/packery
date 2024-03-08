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
import de.pascxl.packery.Packery;
import de.pascxl.packery.events.ChannelInitEvent;
import de.pascxl.packery.network.codec.PacketInDecoder;
import de.pascxl.packery.network.codec.PacketOutEncoder;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelInitializer;

import java.util.logging.Level;

public class NettyServerChannelInitializer extends ChannelInitializer<Channel> {

    private final NettyServer server;
    private final NettyServerHandler nettyServerHandler;

    public NettyServerChannelInitializer(NettyServer server) {
        this.server = server;
        this.nettyServerHandler = new NettyServerHandler(server);
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        Packery.LOGGER.log(Level.INFO, "Initialize Channel: " + ch.remoteAddress().toString());
        this.nettyServerHandler.unauthenticated().add(ch);
        Packery.debug(Level.INFO, this.getClass(), "Added channel (" + ch.remoteAddress().toString() + ") to unauthenticated");
        ch.pipeline().addLast(new PacketInDecoder(this.server.packetManager()), new PacketOutEncoder(this.server.packetManager()), nettyServerHandler);
        Packery.debug(Level.INFO, this.getClass(), "Added channel (" + ch.remoteAddress().toString() + ") pipelines");
        EventManager.call(new ChannelInitEvent(ch));
        Packery.debug(Level.INFO, this.getClass(), "EventCall ChannelInit: (" + ch.remoteAddress().toString() + ")");
    }

/*    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if ((!ctx.channel().isActive() || !ctx.channel().isOpen() || !ctx.channel().isWritable())) {
            Packery.LOGGER.log(Level.INFO, "Channel inactive: " + ctx.channel().remoteAddress());
            unauthenticated.removeIf(channel -> channel.remoteAddress().equals(ctx.channel().remoteAddress()));
            ctx.close();
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Packery.debug(Level.INFO, this.getClass(), "Recache Channel: " + ctx.channel().remoteAddress());
        unauthenticated.removeIf(channel -> channel.remoteAddress().equals(ctx.channel().remoteAddress()));
        unauthenticated.add(ctx.channel());
    }

    @Override
    public void channelExceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (!(cause instanceof IOException))
        {
            Packery.debug(Level.SEVERE, this.getClass(), cause.getMessage());
        }
    }*/
}
