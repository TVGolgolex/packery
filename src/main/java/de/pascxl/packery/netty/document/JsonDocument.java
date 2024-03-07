package de.pascxl.packery.netty.document;

import com.google.gson.*;
import de.pascxl.packery.Packery;
import lombok.Getter;
import lombok.Setter;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

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
@Getter
@Setter
public class JsonDocument {

    private String name;
    private JsonObject dataCatcher;
    private File file;

    public JsonDocument(String name) {
        this.name = name;
        this.dataCatcher = new JsonObject();
    }

    public JsonDocument(String name, JsonObject source) {
        this.name = name;
        this.dataCatcher = source;
    }

    public JsonDocument(File file, JsonObject jsonObject) {
        this.file = file;
        this.dataCatcher = jsonObject;
    }

    public JsonDocument(String key, String value) {
        this.dataCatcher = new JsonObject();
        this.append(key, value);
    }

    public JsonDocument append(String key, String value) {
        if (value == null) {
            return this;
        }
        this.dataCatcher.addProperty(key, value);
        return this;
    }

    public JsonDocument append(String key, Number value) {
        if (value == null) {
            return this;
        }
        this.dataCatcher.addProperty(key, value);
        return this;
    }

    public JsonDocument append(String key, Boolean value) {
        if (value == null) {
            return this;
        }
        this.dataCatcher.addProperty(key, value);
        return this;
    }

    public JsonDocument append(String key, JsonElement value) {
        if (value == null) {
            return this;
        }
        this.dataCatcher.add(key, value);
        return this;
    }

    @Deprecated
    public JsonDocument append(String key, Object value) {
        if (value == null) {
            return this;
        }
        if (value instanceof JsonDocument) {
            this.append(key, (JsonDocument) value);
            return this;
        }
        this.dataCatcher.add(key, Packery.PRETTY_PRINTING_GSON.toJsonTree(value));
        return this;
    }

    public JsonDocument remove(String key) {
        this.dataCatcher.remove(key);
        return this;
    }

    public Set<String> keys() {
        Set<String> c = new HashSet<>();

        for (Map.Entry<String, JsonElement> x : dataCatcher.entrySet()) {
            c.add(x.getKey());
        }

        return c;
    }

    public String getString(String key) {
        if (!dataCatcher.has(key)) {
            return null;
        }
        return dataCatcher.get(key).getAsString();
    }

    public int getInt(String key) {
        if (!dataCatcher.has(key)) {
            return 0;
        }
        return dataCatcher.get(key).getAsInt();
    }

    public long getLong(String key) {
        if (!dataCatcher.has(key)) {
            return 0L;
        }
        return dataCatcher.get(key).getAsLong();
    }

    public double getDouble(String key) {
        if (!dataCatcher.has(key)) {
            return 0D;
        }
        return dataCatcher.get(key).getAsDouble();
    }

    public boolean getBoolean(String key) {
        if (!dataCatcher.has(key)) {
            return false;
        }
        return dataCatcher.get(key).getAsBoolean();
    }

    public float getFloat(String key) {
        if (!dataCatcher.has(key)) {
            return 0F;
        }
        return dataCatcher.get(key).getAsFloat();
    }

    public short getShort(String key) {
        if (!dataCatcher.has(key)) {
            return 0;
        }
        return dataCatcher.get(key).getAsShort();
    }

    public String convertToJson() {
        return Packery.PRETTY_PRINTING_GSON.toJson(dataCatcher);
    }

    public boolean saveAsConfig(File backend) {
        if (backend == null) {
            return false;
        }

        if (backend.exists()) {
            backend.delete();
        }

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(backend), StandardCharsets.UTF_8)) {
            Packery.PRETTY_PRINTING_GSON.toJson(dataCatcher, (writer));
            return true;
        } catch (IOException ex) {
            ex.getStackTrace();
        }
        return false;
    }

    public boolean saveAsConfig(String path) {
        return saveAsConfig(Paths.get(path));
    }

    public JsonDocument getDocument(String key) {
        if (!dataCatcher.has(key)) {
            return null;
        }
        return new JsonDocument(dataCatcher.get(key).getAsJsonObject());
    }

    public boolean saveAsConfig(Path path) {
        try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(Files.newOutputStream(path), StandardCharsets.UTF_8)) {
            Packery.PRETTY_PRINTING_GSON.toJson(dataCatcher, outputStreamWriter);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public JsonDocument(String key, Object value) {
        this.dataCatcher = new JsonObject();
        this.append(key, value);
    }

    public JsonDocument append(String key, JsonDocument value) {
        if (value == null) {
            return this;
        }
        this.dataCatcher.add(key, value.dataCatcher);
        return this;
    }

    public JsonDocument(String key, Number value) {
        this.dataCatcher = new JsonObject();
        this.append(key, value);
    }

    public JsonDocument(JsonDocument defaults) {
        this.dataCatcher = defaults.dataCatcher;
    }

    public JsonDocument(JsonDocument defaults, String name) {
        this.dataCatcher = defaults.dataCatcher;
        this.name = name;
    }

    public JsonDocument() {
        this.dataCatcher = new JsonObject();
    }

    public JsonDocument(JsonObject source) {
        this.dataCatcher = source;
    }

    public static JsonDocument loadDocument(File backend) {
        return loadDocument(backend.toPath());
    }

    public static JsonDocument loadDocument(Path backend) {
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(backend),
                StandardCharsets.UTF_8); BufferedReader bufferedReader = new BufferedReader(reader)) {
            JsonObject object = JsonParser.parseReader(bufferedReader).getAsJsonObject();
            return new JsonDocument(object);
        } catch (Exception ex) {
            ex.getStackTrace();
        }
        return new JsonDocument();
    }

    public static JsonDocument $loadDocument(File backend) throws Exception {
        try {
            return new JsonDocument(JsonParser.parseString(Files.readString(backend.toPath())).getAsJsonObject());
        } catch (Exception ex) {
            throw new Exception(ex);
        }
    }

    public static JsonDocument load(String input) {
        try (InputStreamReader reader = new InputStreamReader(new StringBufferInputStream(input), StandardCharsets.UTF_8)) {
            return new JsonDocument(JsonParser.parseReader(new BufferedReader(reader)).getAsJsonObject());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new JsonDocument();
    }

    public static JsonDocument load(JsonObject input) {
        return new JsonDocument(input);
    }

    public JsonObject obj() {
        return dataCatcher;
    }

    public JsonDocument append(String key, List<String> value) {
        if (value == null) {
            return this;
        }
        JsonArray jsonElements = new JsonArray();

        for (String b : value) {
            jsonElements.add(b);
        }

        this.dataCatcher.add(key, jsonElements);
        return this;
    }

    public JsonDocument appendValues(Map<String, Object> values) {
        for (Map.Entry<String, Object> valuess : values.entrySet()) {
            append(valuess.getKey(), valuess.getValue());
        }
        return this;
    }

    public JsonElement get(String key) {
        if (!dataCatcher.has(key)) {
            return null;
        }
        return dataCatcher.get(key);
    }

    public <T> T getObject(String key, Class<T> class_) {
        if (!dataCatcher.has(key)) {
            return null;
        }
        JsonElement element = dataCatcher.get(key);
        return Packery.PRETTY_PRINTING_GSON.fromJson(element, class_);
    }

    public JsonDocument clear() {
        for (String key : keys()) {
            remove(key);
        }
        return this;
    }

    public int size() {
        return this.dataCatcher.size();
    }

    public JsonDocument loadProperties(Properties properties) {
        Enumeration<?> enumeration = properties.propertyNames();
        while (enumeration.hasMoreElements()) {
            Object x = enumeration.nextElement();
            this.append(x.toString(), properties.getProperty(x.toString()));
        }
        return this;
    }

    public boolean isEmpty() {
        return this.dataCatcher.isEmpty();
    }

    public JsonArray getArray(String key) {
        return dataCatcher.get(key).getAsJsonArray();
    }

    @Deprecated
    public boolean saveAsConfig0(File backend) {
        if (backend == null) {
            return false;
        }

        if (backend.exists()) {
            backend.delete();
        }

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(backend), StandardCharsets.UTF_8)) {
            Packery.PRETTY_PRINTING_GSON.toJson(dataCatcher, (writer));
            return true;
        } catch (IOException ex) {
            ex.getStackTrace();
        }
        return false;
    }

    public JsonDocument loadToExistingDocument(File backend) {
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(backend), StandardCharsets.UTF_8)) {

            this.dataCatcher = JsonParser.parseReader(reader).getAsJsonObject();
            this.file = backend;
            return this;
        } catch (Exception ex) {
            ex.getStackTrace();
        }
        return new JsonDocument();
    }

    public JsonDocument loadToExistingDocument(Path path) {
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {

            this.dataCatcher = JsonParser.parseReader(reader).getAsJsonObject();
            return this;
        } catch (Exception ex) {
            ex.getStackTrace();
        }
        return new JsonDocument();
    }

    @Override
    public String toString() {
        return convertToJsonString();
    }

    public String convertToJsonString() {
        return dataCatcher.toString();
    }

    public <T> T getObject(String key, Type type) {
        if (!contains(key)) {
            return null;
        }

        return Packery.PRETTY_PRINTING_GSON.fromJson(dataCatcher.get(key), type);
    }

    public boolean contains(String key) {
        return this.dataCatcher.has(key);
    }

    public byte[] toBytesAsUTF_8() {
        return convertToJsonString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] toBytes() {
        return convertToJsonString().getBytes();
    }
}
