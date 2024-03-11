package de.pascxl.packery.packet.queue;

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
import de.pascxl.packery.Packery;
import de.pascxl.packery.packet.PacketBase;
import de.pascxl.packery.packet.sender.PacketSender;
import lombok.NonNull;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class PacketQueue {

    private final PacketSender packetSender;
    private final Queue<PacketBase> queue = new LinkedList<>();
    private boolean sending;

    public PacketQueue(PacketSender packetSender) {
        this.packetSender = packetSender;
    }

    public <P extends PacketBase> PacketQueue addPacket(@NonNull P packet) {
        queue.add(packet);
        return this;
    }

    public void sendDelay(int delay, TimeUnit timeUnit, Threading threading) {
        if (sending) {
            Packery.log(Level.SEVERE, this.getClass(), "Sending already started");
            return;
        }
        Scheduler.runtimeScheduler().schedule(() -> {
            Packery.debug(Level.INFO, this.getClass(), "Starting sending ");
            if (queue.isEmpty()) {
                Packery.log(Level.SEVERE, this.getClass(), "Cannot start sending because the queue is empty");
                return;
            }
            sending = true;
            while (!queue.isEmpty()) {
                switch (threading) {
                    case SYNC -> packetSender.sendPacketSync(queue.poll());
                    case ASYNC -> packetSender.sendPacketAsync(queue.poll());
                    case UNSET -> packetSender.sendPacket(queue.poll());
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Packery.log(Level.SEVERE, this.getClass(), e.getMessage());
                }
            }
            sending = false;
        }, timeUnit.toMillis(delay));
    }

    public void send(Threading threading) {
        if (sending) {
            Packery.log(Level.SEVERE, this.getClass(), "Sending already started");
            return;
        }
        Packery.debug(Level.INFO, this.getClass(), "Starting sending ");
        if (queue.isEmpty()) {
            Packery.log(Level.SEVERE, this.getClass(), "Cannot start sending because the queue is empty");
            return;
        }
        sending = true;
        while (!queue.isEmpty()) {
            switch (threading) {
                case SYNC -> packetSender.sendPacketSync(queue.poll());
                case ASYNC -> packetSender.sendPacketAsync(queue.poll());
                case UNSET -> packetSender.sendPacket(queue.poll());
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Packery.log(Level.SEVERE, this.getClass(), e.getMessage());
            }
        }
        sending = false;
    }

    public enum Threading {
        UNSET,
        ASYNC,
        SYNC
    }
}