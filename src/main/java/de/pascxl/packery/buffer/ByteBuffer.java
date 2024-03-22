package de.pascxl.packery.buffer;

import io.netty5.buffer.Buffer;
import io.netty5.buffer.BufferAccessor;
import io.netty5.buffer.BufferRef;

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.io.BufferedWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

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

    public ByteBuffer writeCollectionInteger(Collection<Integer> collection) {
        this.writeInt(collection.size());
        for (var element : collection) {
            this.writeInt(element);
        }
        return this;
    }

    public Collection<Integer> readCollectionInteger() {
        var size = this.readInt();
        Collection<Integer> collection = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            collection.add(this.readInt());
        }
        return collection;
    }

    public ByteBuffer writeCollectionByteBuffer(Collection<ByteBuffer> collection) {
        buffer.writeInt(collection.size());
        for (ByteBuffer item : collection) {
            Buffer itemBuffer = item.buffer();
            int length = itemBuffer.readableBytes();
            buffer.writeInt(length);
            buffer.writeBytes(itemBuffer);
        }
        return this;
    }

    public Collection<ByteBuffer> readCollectionByteBuffer() {
        int size = buffer.readInt();
        List<ByteBuffer> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            int length = buffer.readInt();
            byte[] data = new byte[length];
            buffer.readBytes(java.nio.ByteBuffer.wrap(data));
            Buffer itemBuffer = buffer.implicitCapacityLimit(data.length).copy();
            itemBuffer.writeBytes(data);
            ByteBuffer item = new ByteBuffer(itemBuffer);
            result.add(item);
        }
        return result;
    }

    public ByteBuffer writeCollectionString(Collection<String> collection) {
        this.writeInt(collection.size());
        for (var element : collection) {
            this.writeString(element);
        }
        return this;
    }

    public Collection<String> readCollectionString() {
        var size = this.readInt();
        Collection<String> collection = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            collection.add(this.readString());
        }
        return collection;
    }

    public ByteBuffer writeCollectionBoolean(Collection<Boolean> collection) {
        this.writeInt(collection.size());
        for (var element : collection) {
            this.writeBoolean(element);
        }
        return this;
    }

    public Collection<Boolean> readCollectionBoolean() {
        var size = this.readInt();
        Collection<Boolean> collection = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            collection.add(this.readBoolean());
        }
        return collection;
    }

    public ByteBuffer writeCollectionUUID(Collection<UUID> collection) {
        this.writeInt(collection.size());
        for (var element : collection) {
            this.writeUUID(element);
        }
        return this;
    }

    public Collection<UUID> readCollectionUUID() {
        var size = this.readInt();
        Collection<UUID> collection = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            collection.add(this.readUUID());
        }
        return collection;
    }

    public ByteBuffer writeCollectionEnum(Collection<Enum<?>> collection) {
        this.writeInt(collection.size());
        for (var element : collection) {
            this.writeEnum(element);
        }
        return this;
    }

    public <T extends Enum<T>> Collection<T> readCollectionEnum(Class<T> clazz) {
        var size = this.readInt();
        Collection<T> collection = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            collection.add(this.readEnum(clazz));
        }
        return collection;
    }

    public ByteBuffer writeCollectionLong(Collection<Long> collection) {
        this.writeInt(collection.size());
        for (var element : collection) {
            this.writeLong(element);
        }
        return this;
    }

    public Collection<Long> readCollectionLong() {
        var size = this.readInt();
        Collection<Long> collection = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            collection.add(this.readLong());
        }
        return collection;
    }

    public ByteBuffer writeCollectionFloat(Collection<Float> collection) {
        this.writeInt(collection.size());
        for (var element : collection) {
            this.writeFloat(element);
        }
        return this;
    }

    public Collection<Float> readCollectionFloat() {
        var size = this.readInt();
        Collection<Float> collection = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            collection.add(this.readFloat());
        }
        return collection;
    }

    public ByteBuffer writeCollectionDouble(Collection<Double> collection) {
        this.writeInt(collection.size());
        for (var element : collection) {
            this.writeDouble(element);
        }
        return this;
    }

    public Collection<Double> readCollectionDouble() {
        var size = this.readInt();
        Collection<Double> collection = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            collection.add(this.readDouble());
        }
        return collection;
    }

}
