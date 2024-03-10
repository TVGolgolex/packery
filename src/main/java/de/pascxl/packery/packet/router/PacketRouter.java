package de.pascxl.packery.packet.router;

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
import de.pascxl.packery.packet.defaults.relay.RoutingPacket;
import de.pascxl.packery.packet.defaults.relay.RoutingResultReplyPacket;
import de.pascxl.packery.utils.Value;
import de.pascxl.packery.utils.scheduler.TaskScheduler;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PacketRouter {

    @Getter(AccessLevel.NONE)
    private final Map<UUID, Value<RoutingResult>> result = new ConcurrentHashMap<>(0);
    private final TaskScheduler executorService = new TaskScheduler(1);

    public CompletableFuture<RoutingResult> routeFuture(RoutingPacket routingPacket, NettyTransmitter transmitter) {
        var packetUniqueId = UUID.randomUUID();
        routingPacket.uniqueId(packetUniqueId);

        var resultFuture = new CompletableFuture<RoutingResult>();
        var value = new Value<>(RoutingResult.FAILED_UNKNOWN);
        result.put(packetUniqueId, value);
        transmitter.sendPacketAsync(routingPacket);

        executorService.schedule(() -> {
            RoutingResult finalResult = result.get(packetUniqueId).entry();
            result.remove(packetUniqueId);
            resultFuture.complete(finalResult);
        }, 5000);
        return resultFuture;
    }

    public RoutingResult routeUnsafe(RoutingPacket routingPacket, NettyTransmitter transmitter) {
        var packetUniqueId = UUID.randomUUID();
        routingPacket.uniqueId(packetUniqueId);

        var value = new Value<>(RoutingResult.IDLE);
        result.put(packetUniqueId, value);
        transmitter.sendPacketAsync(routingPacket);

        int i = 0;

        while (result.get(packetUniqueId).entry() == RoutingResult.IDLE && i++ < 5000) {
            try {
                Thread.sleep(0, 500000);
            } catch (InterruptedException ignored) {
            }
        }

        if (i >= 4999) {
            result.get(packetUniqueId).entry(RoutingResult.NO_RESULT);
        }

        Value<RoutingResult> resultValue = result.get(packetUniqueId);
        result.remove(packetUniqueId);
        return resultValue == null ? null : resultValue.entry() == null ? null : resultValue.entry();
    }

    public void dispatch(RoutingResultReplyPacket routingResultReplyPacket) {
        var resultValue = result.get(routingResultReplyPacket.uniqueId());
        resultValue.entry(routingResultReplyPacket.routingResult());
    }

}
