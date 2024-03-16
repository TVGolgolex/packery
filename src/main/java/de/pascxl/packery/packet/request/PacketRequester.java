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

import de.golgolex.quala.scheduler.Scheduler;
import de.golgolex.quala.utils.data.Value;
import de.pascxl.packery.network.NettyTransmitter;
import de.pascxl.packery.packet.PacketBase;
import de.pascxl.packery.packet.PacketManager;
import de.pascxl.packery.packet.defaults.request.RequestPacket;
import de.pascxl.packery.packet.defaults.request.RespondPacket;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class PacketRequester {

    private final PacketManager packetManager;
    private final Map<UUID, Value<RequestResult<?>>> waiting = new ConcurrentHashMap<>(0);
    private final Scheduler executorService = new Scheduler(1);

    public PacketRequester(PacketManager packetManager) {
        this.packetManager = packetManager;
    }

    public <P extends RequestPacket> CompletableFuture<RespondPacket> queryFuture(P packet, NettyTransmitter transmitter) {
        var packetUniqueId = UUID.randomUUID();
        packet.uniqueId(packetUniqueId);

        var resultFuture = new CompletableFuture<RespondPacket>();
        Value<RequestResult<?>> value = new Value<>(null);
        waiting.put(packetUniqueId, value);
        executorService.schedule(() -> transmitter.sendPacket(packet));

        executorService.schedule(() -> {
            Value<RequestResult<?>> resultValue = waiting.get(packetUniqueId);
            waiting.remove(packetUniqueId);
            resultFuture.complete(resultValue != null && resultValue.value() != null ? (RespondPacket) castResult(resultValue).resultPacket() : null);
        }, 5000);
        return resultFuture;
    }

    public <P extends RequestPacket> RespondPacket queryUnsafe(P packet, NettyTransmitter transmitter) {
        var packetUniqueId = UUID.randomUUID();
        packet.uniqueId(packetUniqueId);

        Value<RequestResult<?>> value = new Value<>(null);
        waiting.put(packetUniqueId, value);
        executorService.schedule(() -> transmitter.sendPacket(packet));

        int i = 0;

        while (waiting.get(packetUniqueId).value() == null && i++ < 5000) {
            try {
                Thread.sleep(0, 500000);
            } catch (InterruptedException ignored) {
            }
        }

        if (i >= 4999) {
            waiting.get(packetUniqueId).value(new RequestResult<>(packetUniqueId, null));
        }

        Value<RequestResult<?>> resultValue = waiting.get(packetUniqueId);
        waiting.remove(packetUniqueId);
        return resultValue != null && resultValue.value() != null ? (RespondPacket) castResult(resultValue).resultPacket() : null;
    }

    @SuppressWarnings("unchecked")
    private <PR extends PacketBase> RequestResult<PR> castResult(Value<RequestResult<?>> resultValue) {
        return (RequestResult<PR>) resultValue.value();
    }

    public void dispatch(PacketBase packet) {
        RequestResult<PacketBase> requestResult = new RequestResult<>(packet.uniqueId(), packet);
        Value<RequestResult<?>> waitingQuery = waiting.get(packet.uniqueId());
        waitingQuery.value(requestResult);
    }
}
