package de.pascxl.packery.netty.transmitter;

import com.github.golgolex.eventum.EventManager;
import de.pascxl.packery.Packery;
import de.pascxl.packery.events.TransmitterInitEvent;
import de.pascxl.packery.netty.packet.DefaultPacket;
import de.pascxl.packery.netty.packet.PacketManager;
import de.pascxl.packery.netty.packet.PacketSender;
import de.pascxl.packery.netty.packet.PacketType;
import de.pascxl.packery.netty.packet.auth.Authentication;
import de.pascxl.packery.netty.packet.unknown.EncodingPacket;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.channel.SimpleChannelInboundHandler;
import lombok.Getter;
import lombok.Setter;

import java.util.logging.Level;

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
@Getter
@Setter
public class NettyTransmitter extends SimpleChannelInboundHandler implements PacketSender {

    private final Authentication authentication;
    private final PacketManager packetManager;
    private final long connectionTIme;
    private Channel channel;

    public NettyTransmitter(Authentication authentication, PacketManager packetManager, long connectionTIme, Channel channel) {
        this.authentication = authentication;
        this.packetManager = packetManager;
        this.connectionTIme = connectionTIme;
        this.channel = channel;
        EventManager.call(new TransmitterInitEvent(this));
        Packery.LOGGER.log(Level.INFO, "Transmitter initialized: " + channel.remoteAddress() + "/" + authentication.namespace() + "#" + authentication.uniqueId());
    }

    @Override
    public void send(Object object) {
        Packery.debug(this.getClass(), "send object (" + object.getClass().getSimpleName() + ")");
        channel.writeAndFlush(object);
    }

    @Override
    public void sendSynchronized(Object object) {
        Packery.debug(this.getClass(), "sendSynchronized object (" + object.getClass().getSimpleName() + ")");
        Packery.DIRECT_EXECUTOR.execute(() -> channel.writeAndFlush(object));
    }

    @Override
    public void sendASynchronized(Object object) {
        Packery.debug(this.getClass(), "sendASynchronized object (" + object.getClass().getSimpleName() + ")");
        Packery.ASYNC_EXECUTOR.execute(() -> channel.writeAndFlush(object));
    }

    @Override
    public void send(PacketType<?> packetType, Object element) {
        this.send(new EncodingPacket(packetType.typeId(), element));
    }

    @Override
    public void send(int typeId, Object element) {
        this.send(new EncodingPacket(typeId, element));
    }

    @Override
    public void sendASynchronized(int typeId, Object element) {
        this.sendASynchronized(new EncodingPacket(typeId, element));
    }

    @Override
    public void sendASynchronized(PacketType<?> packetType, Object element) {
        this.sendASynchronized(new EncodingPacket(packetType.typeId(), element));
    }

    @Override
    public void sendSynchronized(int typeId, Object element) {
        this.sendSynchronized(new EncodingPacket(typeId, element));
    }

    @Override
    public void sendSynchronized(PacketType<?> packetType, Object element) {
        this.sendSynchronized(new EncodingPacket(packetType.typeId(), element));
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.channel = ctx.channel();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if ((!channel.isActive() || !channel.isOpen() || !channel.isWritable())) {
            channel = null;
            ctx.close();
        }
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
        Packery.debug(this.getClass(), "Executed messageReceived");

        if (msg instanceof DefaultPacket defaultPacket) {
            Packery.debug(this.getClass(), "Dispatched " + msg.getClass().getSimpleName());
            this.packetManager.call(defaultPacket, this, ctx, this.authentication);
        }
    }
}
