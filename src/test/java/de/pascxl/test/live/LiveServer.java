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
import de.pascxl.packery.server.NettyServer;

public class LiveServer {

    public static void main(String[] args) {

        Packery.DEV_MODE = true;

        NettyServer nettyServer = new NettyServer();

        boolean state = nettyServer.connect("0.0.0.0", 44488, false);

        if (!state) {
            return;
        }

        nettyServer.packetManager().allowPacket(774090777346262697L);

    }

}
