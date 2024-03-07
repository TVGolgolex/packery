package de.pascxl.packery.netty.packet.query;

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
import de.pascxl.packery.Value;
import de.pascxl.packery.netty.packet.DefaultPacket;
import de.pascxl.packery.netty.packet.PacketManager;
import de.pascxl.packery.netty.packet.PacketSender;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

@Getter
public class QuerySender {

    private final PacketManager packetManager;
    private final Map<UUID, Value<QueryResult<?>>> waiting = new ConcurrentHashMap<>(0);
    private final Queue<?> queue = new ConcurrentLinkedQueue<>();
    private final Collection<Long> manuellRegisteredIds = new ArrayList<>();

    public QuerySender(PacketManager packetManager) {
        this.packetManager = packetManager;
    }

    public <P extends DefaultPacket, PR extends DefaultPacket> QueryResult<PR> query(P packet, PacketSender packetSender) {
        if (!packetManager.isRegisteredType(packet)) {
            Packery.LOGGER.log(Level.SEVERE, "No PacketType registered for the packet [{0}].", packet.getClass().getSimpleName());
            return null;
        }

        if (!packetManager.isRegisteredPacketId(packet.packetId())) {
            manuellRegisteredIds.add(packet.packetId());
            packetManager.enablePacketId(packet.packetId());
            Packery.debug(this.getClass(), "The PacketID {0} was not registered as Allowed. It was registered manually for the query process and is unregistered again after expiry.", packet.packetId());
        }

        UUID queryUUID = UUID.randomUUID();
        packet.uniqueId(queryUUID);

        Value<QueryResult<?>> handled = new Value<>(null);
        waiting.put(queryUUID, handled);
        packetSender.sendASynchronized(packet);

        int i = 0;

        while (waiting.get(queryUUID).entry() == null && i++ < 5000) {
            try {
                Thread.sleep(0, 500000);
            } catch (InterruptedException ignored) {
            }
        }

        if (i >= 4999) {
            waiting.get(queryUUID).entry(new QueryResult<PR>(queryUUID, null));
        }

        Value<QueryResult<?>> resultValue = waiting.get(queryUUID);

        waiting.remove(queryUUID);
        if (manuellRegisteredIds.contains(packet.packetId())) {
            packetManager.removeEnablePacketId(packet.packetId());
        }

        return resultValue != null && resultValue.entry() != null ? castQueryResult(resultValue) : null;
    }

    @SuppressWarnings("unchecked")
    private <PR extends DefaultPacket> QueryResult<PR> castQueryResult(Value<QueryResult<?>> resultValue) {
        return (QueryResult<PR>) resultValue.entry();
    }

    public void dispatch(DefaultPacket packet) {
        QueryResult<DefaultPacket> queryResult = new QueryResult<>(packet.uniqueId(), packet);
        Value<QueryResult<?>> waitingQuery = waiting.get(packet.uniqueId());
        waitingQuery.entry(queryResult);
    }
}
