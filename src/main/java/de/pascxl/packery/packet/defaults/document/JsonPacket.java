package de.pascxl.packery.packet.defaults.document;

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

import com.google.gson.JsonObject;
import de.golgolex.quala.json.JsonUtils;
import de.golgolex.quala.json.document.JsonDocument;
import de.pascxl.packery.buffer.ByteBuffer;
import de.pascxl.packery.packet.PacketBase;
import lombok.Getter;

import java.util.UUID;

@Getter
public class JsonPacket extends PacketBase {

    private JsonDocument jsonDocument;

    public JsonPacket(long packetId) {
        super(packetId);
        this.jsonDocument = new JsonDocument();
    }

    public JsonPacket(long packetId, JsonDocument jsonDocument) {
        super(packetId);
        this.jsonDocument = jsonDocument;
    }

    public JsonPacket(long packetId, UUID uniqueId, JsonDocument jsonDocument) {
        super(packetId, uniqueId);
        this.jsonDocument = jsonDocument;
    }

    @Override
    public void write(ByteBuffer out) {
        out.writeString(this.jsonDocument.jsonObjectToString());
    }

    @Override
    public void read(ByteBuffer in) {
        this.jsonDocument = new JsonDocument(JsonUtils.JSON.fromJson(in.readString(), JsonObject.class));
    }
}
