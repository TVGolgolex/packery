package de.pascxl.packery.netty.buffer;

import de.pascxl.packery.netty.document.PDocument;
import de.pascxl.packery.netty.io.CallableDecoder;
import de.pascxl.packery.netty.io.CallableEncoder;
import de.pascxl.packery.netty.io.Decoder;
import de.pascxl.packery.netty.io.Encoder;
import io.netty5.buffer.Buffer;
import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

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
public record ByteBuffer(Buffer buffer) {

    public ByteBuffer writeInt(int value) {
        this.buffer.writeInt(value);
        return this;
    }

    public int readInt() {
        return this.buffer.readInt();
    }

    public ByteBuffer writeString(String value) {
        var bytes = value.getBytes();
        this.buffer.writeInt(bytes.length);
        this.buffer.writeBytes(bytes);
        return this;
    }

    public String readString() {
        return this.buffer.readCharSequence(this.buffer.readInt(), StandardCharsets.UTF_8).toString();
    }

    public void writeDocument(PDocument PDocument) {
        this.writeString(PDocument.convertToJsonString());
    }

    public PDocument readDocument() {
        return PDocument.load(readString());
    }

    public ByteBuffer writeBoolean(Boolean booleanValue) {
        this.buffer.writeBoolean(booleanValue);
        return this;
    }

    public boolean readBoolean() {
        return this.buffer.readBoolean();
    }

    public ByteBuffer writeUUID(UUID uuid) {
        this.buffer.writeLong(uuid.getMostSignificantBits());
        this.buffer.writeLong(uuid.getLeastSignificantBits());
        return this;
    }

    public UUID readUUID() {
        return new UUID(this.buffer.readLong(), this.buffer.readLong());
    }

    public ByteBuffer writeEnum(Enum<?> value) {
        this.buffer.writeInt(value.ordinal());
        return this;
    }

    public <T extends Enum<T>> T readEnum(Class<T> clazz) {
        return clazz.getEnumConstants()[this.buffer.readInt()];
    }

    public ByteBuffer writeLong(long value) {
        this.buffer.writeLong(value);
        return this;
    }

    public long readLong() {
        return this.buffer.readLong();
    }

    public ByteBuffer writeFloat(float value) {
        this.buffer.writeFloat(value);
        return this;
    }

    public float readFloat() {
        return this.buffer.readFloat();
    }

    public ByteBuffer writeDouble(double value) {
        this.buffer.writeDouble(value);
        return this;
    }

    public double readDouble() {
        return this.buffer.readDouble();
    }

    public <T extends Encoder> void writeCollection(final Collection<T> collection) {
        this.writeCollection(collection, Encoder::write);
    }

    public <T extends Decoder> List<T> readCollection(final Supplier<T> factory) {
        return this.readCollection(buffer -> {
            T instance = factory.get();
            instance.read(this);
            return instance;
        });
    }

    public <T> void writeCollection(final Collection<T> collection, final CallableEncoder<T> encoder) {
        this.writeInt(collection.size());
        for (final T entry : collection) {
            encoder.write(entry, this);
        }
    }

    public <T> List<T> readCollection(final CallableDecoder<T> decoder) {
        var size = this.readInt();
        final List<T> data = new ArrayList<>(size);
        for (int i = 0; i < size; i++) data.add(decoder.read(this));
        return data;
    }

    public void writeIntCollection(final Collection<Integer> collection) {
        this.writeCollection(collection, (data, buffer) -> buffer.writeInt(data));
    }

    public List<Integer> readIntCollection() {
        return this.readCollection(ByteBuffer::readInt);
    }

    public void writeStringCollection(final Collection<String> collection) {
        this.writeCollection(collection, (data, buffer) -> buffer.writeString(data));
    }

    public List<String> readStringCollection() {
        return this.readCollection(ByteBuffer::readString);
    }

    public void writeUuidCollection(final Collection<UUID> collection) {
        this.writeCollection(collection, (data, buffer) -> buffer.writeUUID(data));
    }

    public List<UUID> readUuidCollection() {
        return this.readCollection(ByteBuffer::readUUID);
    }

}
