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
import de.pascxl.packery.packet.PacketBase;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.channel.SimpleChannelInboundHandler;
import lombok.AllArgsConstructor;

import java.io.IOException;
import java.util.logging.Level;

@AllArgsConstructor
public class NettyClientHandler extends SimpleChannelInboundHandler<PacketBase> {

    private final NettyClient client;

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, PacketBase msg) throws Exception {
        Packery.debug(Level.INFO, this.getClass(), "messageReceived: " + msg.getClass().getSimpleName());
        this.client.packetManager.call(msg, this.client.nettyTransmitter(), ctx, this.client.account());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.client.nettyTransmitter().channel(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if ((!ctx.channel().isActive() || !ctx.channel().isOpen() || !ctx.channel().isWritable())) {
            ctx.channel().close();
            switch (client.inactiveAction()) {
                case SHUTDOWN -> System.exit(0);
                case RETRY -> {

                }
            }
        }
    }

    @Override
    public void channelExceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (!(cause instanceof IOException)) {
            Packery.debug(Level.SEVERE, this.getClass(), cause.getMessage());
        }
    }

}
