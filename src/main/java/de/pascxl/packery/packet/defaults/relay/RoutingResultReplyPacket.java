package de.pascxl.packery.packet.defaults.relay;

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

import de.pascxl.packery.buffer.ByteBuffer;
import de.pascxl.packery.packet.NettyPacket;
import de.pascxl.packery.packet.router.RoutingResult;
import lombok.Getter;

import java.util.UUID;

@Getter
public class RoutingResultReplyPacket extends NettyPacket {

    private RoutingResult routingResult;

    public RoutingResultReplyPacket(UUID uniqueId, RoutingResult routingResult) {
        super(uniqueId);
        this.routingResult = routingResult;
    }

    @Override
    public void write(ByteBuffer out) {
        out.writeEnum(routingResult);
    }

    @Override
    public void read(ByteBuffer in) {
        routingResult = in.readEnum(RoutingResult.class);
    }
}
