package de.pascxl.test.fun.test;

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

import com.google.gson.JsonElement;
import de.pascxl.packery.packet.listener.PacketReceiveListener;
import de.pascxl.packery.packet.sender.PacketSender;
import io.netty5.channel.ChannelHandlerContext;

import java.util.Map;

public class TestPacketListener extends PacketReceiveListener<TestNettyPacket> {
    @Override
    public void call(TestNettyPacket packet, PacketSender packetSender, ChannelHandlerContext channelHandlerContext) {
        for (Map.Entry<String, JsonElement> stringJsonElementEntry : packet.jsonDocument().jsonObject().entrySet()) {
            System.out.println(stringJsonElementEntry.getKey() + ": " + stringJsonElementEntry.getValue().toString());
        }
    }
}
