package de.pascxl.test.live;

/*
 * Copyright 2024 packery contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import de.pascxl.packery.Packery;
import de.pascxl.packery.client.NettyClient;
import de.pascxl.packery.network.ChannelIdentity;
import de.pascxl.packery.network.InactiveAction;
import de.pascxl.packery.utils.BypassCheck;
import de.pascxl.test.live.packet.TestNettyPacket;
import de.pascxl.test.live.packet.TestPacketListener;

import java.util.UUID;

public class LiveClientTwo {
    public static void main(String[] args) {

        Packery.DEV_MODE = true;

        NettyClient nettyClient = new NettyClient(new ChannelIdentity("client-2", UUID.randomUUID()), InactiveAction.SHUTDOWN);

        boolean state = nettyClient.connect("0.0.0.0", 44488, false);

        if (!state) {
            return;
        }

        nettyClient.packetManager().allowPacket(BypassCheck.class);
        nettyClient.packetManager().registerPacketHandler(TestNettyPacket.class.getName(), TestPacketListener.class);

    }
}
