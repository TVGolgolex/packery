package de.pascxl.packery.test;

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

import de.pascxl.packery.NettyAddress;
import de.pascxl.packery.Packery;
import de.pascxl.packery.netty.client.InactiveAction;
import de.pascxl.packery.netty.client.NettyClient;
import de.pascxl.packery.netty.document.PDocument;
import de.pascxl.packery.netty.packet.auth.Authentication;
import de.pascxl.packery.netty.packet.document.DocumentPacket;

import java.util.UUID;

public class Client {

    public static void main(String[] args) {

        Packery.DEV_MODE = true;
        NettyClient nettyClient = new NettyClient(
                new Authentication("test", UUID.randomUUID()),
                new NettyAddress("0.0.0.0", 8558),
                InactiveAction.SHUTDOWN
        );

        nettyClient.connect(false, () -> {

            DocumentPacket documentPacket = new DocumentPacket(2, new PDocument());

            for (int i = 0; i < 100; i++) {
                documentPacket.data().append(UUID.randomUUID().toString(), UUID.randomUUID());
                documentPacket.data().append(UUID.randomUUID().toString(), "" + UUID.randomUUID() + "xxxx");
                documentPacket.data().append(UUID.randomUUID().toString(), i);
            }

            nettyClient.nettyTransmitter().send(documentPacket);

        });

    }

}
