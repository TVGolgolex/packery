package de.pascxl.packery.netty.packet.document;

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

import de.pascxl.packery.netty.buffer.ByteBuffer;
import de.pascxl.packery.netty.document.PDocument;
import de.pascxl.packery.netty.packet.DefaultPacket;
import lombok.Getter;

import java.util.UUID;

@Getter
public class DocumentPacket extends DefaultPacket {

    protected PDocument data;

    public DocumentPacket() {
    }

    public DocumentPacket(long packetId) {
        this.packetId = packetId;
        this.data = new PDocument();
    }

    public DocumentPacket(PDocument data) {
        this.data = data;
    }

    public DocumentPacket(long packetId, PDocument data) {
        this.packetId = packetId;
        this.data = data;
    }

    public DocumentPacket(UUID uniqueId, long packetId, PDocument data) {
        this.uniqueId = uniqueId;
        this.packetId = packetId;
        this.data = data;
    }

    @Override
    public void readBuffer(ByteBuffer in) {
        PDocument readDocument = in.readDocument();
        if (!readDocument.contains("packet_state_x1_empty")) {
            this.data = readDocument;
        } else {
            this.data = new PDocument();
        }
    }

    @Override
    public void writeBuffer(ByteBuffer out) {
        if (this.data.isEmpty()) {
            this.data = new PDocument("packet_state_x1_empty", "empty");
        }
        out.writeDocument(this.data);
    }
}
