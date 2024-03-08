package de.pascxl.packery.packet.request;

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

import de.pascxl.packery.network.NettyTransmitter;
import de.pascxl.packery.packet.PacketBase;
import de.pascxl.packery.packet.PacketManager;
import de.pascxl.packery.utils.Value;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class PacketRequester {

    private final PacketManager packetManager;
    private final Map<UUID, Value<Respond<?>>> waiting = new ConcurrentHashMap<>(0);

    public PacketRequester(PacketManager packetManager) {
        this.packetManager = packetManager;
    }

    public <P extends RequestPacket, PR extends PacketBase> RespondPacket query(P packet, NettyTransmitter nettyTransmitter) {
        UUID queryUUID = UUID.randomUUID();
        packet.uniqueId(queryUUID);

        Value<Respond<?>> handled = new Value<>(null);
        waiting.put(queryUUID, handled);
        nettyTransmitter.sendPacketAsync(packet);

        int i = 0;

        while (waiting.get(queryUUID).entry() == null && i++ < 5000) {
            try {
                Thread.sleep(0, 500000);
            } catch (InterruptedException ignored) {
            }
        }

        if (i >= 4999) {
            waiting.get(queryUUID).entry(new Respond<>(queryUUID, null));
        }

        Value<Respond<?>> resultValue = waiting.get(queryUUID);
        waiting.remove(queryUUID);
        return resultValue != null && resultValue.entry() != null ? (RespondPacket) castResult(resultValue).resultPacket() : null;
    }

    @SuppressWarnings("unchecked")
    private <PR extends PacketBase> Respond<PR> castResult(Value<Respond<?>> resultValue) {
        return (Respond<PR>) resultValue.entry();
    }

    public void dispatch(PacketBase packet) {
        Respond<PacketBase> respond = new Respond<>(packet.uniqueId(), packet);
        Value<Respond<?>> waitingQuery = waiting.get(packet.uniqueId());
        waitingQuery.entry(respond);
    }
}
