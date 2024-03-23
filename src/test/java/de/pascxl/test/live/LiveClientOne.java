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

import de.golgolex.quala.json.document.JsonDocument;
import de.golgolex.quala.scheduler.Scheduler;
import de.golgolex.quala.utils.string.StringUtils;
import de.pascxl.packery.Packery;
import de.pascxl.packery.buffer.ByteBuffer;
import de.pascxl.packery.client.NettyClient;
import de.pascxl.packery.network.ChannelIdentity;
import de.pascxl.packery.network.InactiveAction;
import de.pascxl.packery.packet.NettyPacket;
import de.pascxl.packery.packet.defaults.relay.RoutingNettyPacket;
import de.pascxl.packery.utils.BypassCheck;
import de.pascxl.test.fun.test.TestNettyPacket;

import java.util.UUID;

public class LiveClientOne {
    public static void main(String[] args) {

        Packery.DEV_MODE = true;

        NettyClient nettyClient = new NettyClient(new ChannelIdentity("client-1", UUID.randomUUID()), InactiveAction.SHUTDOWN);

        boolean state = nettyClient.connect("0.0.0.0", 44488, false);

        if (!state) {
            return;
        }

        nettyClient.packetManager().allowPacket(BypassCheck.class);

/*        nettyClient.nettyTransmitter().sendPacket(new NettyPacket() {
            @Override
            public void write(ByteBuffer out) {

            }

            @Override
            public void read(ByteBuffer in) {

            }
        });*/

        Scheduler.runtimeScheduler().schedule(() -> {
            TestNettyPacket testPacket = new TestNettyPacket(new JsonDocument());
            for (int i = 0; i < 30; i++) {
                testPacket.jsonDocument().write(StringUtils.generateRandomString(7), StringUtils.generateRandomString(25));
            }

            var result = nettyClient
                    .packetManager()
                    .packetRouter()
                    .routeDirect(new RoutingNettyPacket(testPacket, nettyClient
                                    .channelIdentities()
                                    .stream()
                                    .filter(channelIdentity -> channelIdentity.namespace().equalsIgnoreCase("Client-2"))
                                    .findFirst()
                                    .orElseThrow()),
                            nettyClient.nettyTransmitter()
                    );

            System.out.println(result.name());

            var relayResult = nettyClient
                    .packetManager()
                    .packetRouter()
                    .routeFuture(new RoutingNettyPacket(testPacket,
                                    nettyClient
                                            .channelIdentities()
                                            .stream()
                                            .filter(channelIdentity -> channelIdentity.namespace().equalsIgnoreCase("Client-2"))
                                            .findFirst()
                                            .orElseThrow()
                            ),
                            nettyClient.nettyTransmitter()
                    );

//            System.out.println(((Rout)));

            System.out.println(relayResult.whenComplete((routingResult1, throwable) -> {
                System.out.println(routingResult1.name());
            }));
        }, 5000);


    }
}
