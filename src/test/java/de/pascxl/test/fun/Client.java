package de.pascxl.test.fun;

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
import de.pascxl.packery.client.NettyClient;
import de.pascxl.packery.network.InactiveAction;
import de.pascxl.packery.network.ChannelIdentity;
import de.pascxl.packery.packet.defaults.document.JsonDocument;
import de.pascxl.packery.packet.queue.PacketQueue;
import de.pascxl.test.fun.test.TestPacket;
import de.pascxl.test.fun.test.respond.TestRequestPacket;
import de.pascxl.packery.utils.StringUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Client {
    public static void main(String[] args) {

        Packery.DEV_MODE = true;

        NettyClient nettyClient = new NettyClient(new ChannelIdentity("test", UUID.randomUUID()), InactiveAction.SHUTDOWN);

        nettyClient.connect("0.0.0.0", 27785, false);

        nettyClient.packetManager().allowPacket(4);
        nettyClient.packetManager().allowPacket(3);

        PacketQueue packetQueue = new PacketQueue(nettyClient.nettyTransmitter());

        for (int i = 0; i < 25; i++) {
            TestPacket testPacket = new TestPacket(4, new JsonDocument());
            testPacket.jsonDocument().write(StringUtils.generateRandomString(7), StringUtils.generateRandomString(25));
            packetQueue.addPacket(testPacket);
        }

        packetQueue.sendDelayAsync(10, TimeUnit.SECONDS);

        TestPacket testPacket = new TestPacket(4, new JsonDocument());

        for (int i = 0; i < 30; i++) {
            testPacket.jsonDocument().write(StringUtils.generateRandomString(7), StringUtils.generateRandomString(25));
        }

        nettyClient.nettyTransmitter().sendPacketAsync(testPacket);

        TestRequestPacket testRequestPacket = new TestRequestPacket("test");

        TestPacket result = (TestPacket) nettyClient.packetManager()
                .packetRequester()
                .query(testRequestPacket, nettyClient.nettyTransmitter())
                .packet();

        if (result == null) {
            System.out.println("result is null");
            return;
        }

        System.out.println(result.jsonDocument().readString("test"));

    }
}