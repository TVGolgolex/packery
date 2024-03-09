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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.pascxl.packery.Packery;
import de.pascxl.packery.utils.JsonUtils;
import lombok.Getter;
import lombok.NonNull;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

@Getter
public class JsonDocument {

    protected String name;
    protected JsonObject jsonObject;
    protected File file;

    public JsonDocument(@NonNull String name) {
        this.name = name;
        this.jsonObject = new JsonObject();
    }

    public JsonDocument(@NonNull String name, @NonNull JsonObject jsonObject) {
        this.name = name;
        this.jsonObject = jsonObject;
    }

    public JsonDocument(@NonNull File file, @NonNull JsonObject jsonObject) {
        this.file = file;
        this.jsonObject = jsonObject;
    }

    public JsonDocument(@NonNull String key, @NonNull String string) {
        this.jsonObject = new JsonObject();
        this.write(key, string);
    }

    public JsonDocument(@NonNull String key, @NonNull Object object) {
        this.jsonObject = new JsonObject();
        this.write(key, object);
    }

    public JsonDocument(@NonNull String key, @NonNull Number number) {
        this.jsonObject = new JsonObject();
        this.write(key, number);
    }

    public JsonDocument(@NonNull JsonDocument jsonDocument) {
        this.jsonObject = jsonDocument.jsonObject;
    }

    public JsonDocument(@NonNull String name, @NonNull JsonDocument jsonDocument) {
        this.name = name;
        this.jsonObject = jsonDocument.jsonObject;
    }

    public JsonDocument() {
        this.jsonObject = new JsonObject();
    }

    public JsonDocument(@NonNull JsonObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    public static JsonDocument fromFile(@NonNull File file) {
        return fromPath(file.toPath());
    }

    public static JsonDocument fromFileParse(@NonNull File backend) throws Exception {
        try {
            return new JsonDocument(JsonParser.parseString(Files.readString(backend.toPath())).getAsJsonObject());
        } catch (Exception ex) {
            throw new Exception(ex);
        }
    }

    public static JsonDocument fromPath(@NonNull Path backend) {
        try (var reader = new InputStreamReader(Files.newInputStream(backend),
                StandardCharsets.UTF_8); BufferedReader bufferedReader = new BufferedReader(reader)) {
            JsonObject object = JsonParser.parseReader(bufferedReader).getAsJsonObject();
            return new JsonDocument(object);
        } catch (Exception ex) {
            ex.getStackTrace();
        }
        return new JsonDocument();
    }

    public static JsonDocument parseJson(String input) {
        try (var inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
             InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return new JsonDocument(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (IOException e) {
            Packery.log(Level.SEVERE, JsonDocument.class, e.getMessage());
        }
        return null;
    }

    public JsonDocument write(@NonNull String key, @NonNull String string) {
        this.jsonObject.addProperty(key, string);
        return this;
    }

    public JsonDocument write(@NonNull String key, @NonNull Number number) {
        this.jsonObject.addProperty(key, number);
        return this;
    }

    public JsonDocument write(@NonNull String key, @NonNull Boolean aBoolean) {
        this.jsonObject.addProperty(key, aBoolean);
        return this;
    }

    public JsonDocument write(@NonNull String key, @NonNull JsonElement jsonElement) {
        this.jsonObject.add(key, jsonElement);
        return this;
    }

    @Deprecated
    public JsonDocument write(@NonNull String key, @NonNull Object object) {
        if (object instanceof JsonDocument jsonDocument) {
            this.write(key, jsonDocument);
            return this;
        }
        this.jsonObject.add(key, JsonUtils.PRETTY_JSON.toJsonTree(object));
        return this;
    }

    public JsonDocument write(@NonNull String key, @NonNull JsonDocument jsonDocument) {
        this.jsonObject.add(key, jsonDocument.jsonObject);
        return this;
    }

    public JsonDocument write(@NonNull String key, @NonNull List<String> value) {
        var jsonElements = new JsonArray();

        for (String b : value) {
            jsonElements.add(b);
        }

        this.jsonObject.add(key, jsonElements);
        return this;
    }

    public JsonDocument appendValues(@NonNull Map<String, Object> map) {
        for (var stringObjectEntry : map.entrySet()) {
            write(stringObjectEntry.getKey(), stringObjectEntry.getValue());
        }
        return this;
    }

    public <T> T readObject(@NonNull String key, @NonNull Class<T> aClass) {
        if (!jsonObject.has(key)) {
            return null;
        }
        var element = jsonObject.get(key);
        return JsonUtils.PRETTY_JSON.fromJson(element, aClass);
    }

    public <T> T readObject(@NonNull String key, @NonNull Type type) {
        if (!jsonObject.has(key)) {
            return null;
        }
        var element = jsonObject.get(key);
        return JsonUtils.PRETTY_JSON.fromJson(element, type);
    }

    public JsonArray readJsonArray(@NonNull String key) {
        if (!jsonObject.has(key)) {
            return null;
        }
        return this.jsonObject.get(key).getAsJsonArray();
    }

    public JsonElement readJsonElement(@NonNull String key) {
        if (!jsonObject.has(key)) {
            return null;
        }
        return this.jsonObject.get(key);
    }

    public JsonDocument readJsonDocument(String key) {
        if (!jsonObject.has(key)) {
            return null;
        }
        return new JsonDocument(jsonObject.get(key).getAsJsonObject());
    }

    public String readString(String key) {
        if (!jsonObject.has(key)) {
            return null;
        }
        return this.jsonObject.get(key).getAsString();
    }

    public int readInteger(String key) {
        if (!jsonObject.has(key)) {
            return 0;
        }
        return this.jsonObject.get(key).getAsInt();
    }

    public long readLong(String key) {
        if (!jsonObject.has(key)) {
            return 0L;
        }
        return this.jsonObject.get(key).getAsLong();
    }

    public double readDouble(String key) {
        if (!jsonObject.has(key)) {
            return 0D;
        }
        return this.jsonObject.get(key).getAsDouble();
    }

    public boolean readBoolean(String key) {
        if (!jsonObject.has(key)) {
            return false;
        }
        return this.jsonObject.get(key).getAsBoolean();
    }

    public float readFloat(String key) {
        if (!jsonObject.has(key)) {
            return 0F;
        }
        return this.jsonObject.get(key).getAsFloat();
    }

    public short readShort(String key) {
        if (!jsonObject.has(key)) {
            return 0;
        }
        return this.jsonObject.get(key).getAsShort();
    }

    public void clear() {
        for (var key : jsonObject.keySet()) {
            delete(key);
        }
    }

    public void delete(@NonNull String key) {
        this.jsonObject.remove(key);
    }

    public int count() {
        return this.jsonObject.size();
    }

    public boolean empty() {
        return this.jsonObject.isEmpty();
    }

    public boolean contains(String key) {
        return this.jsonObject.has(key);
    }

    public String prettyJsonString() {
        return JsonUtils.PRETTY_JSON.toJson(this.jsonObject);
    }

    public String jsonObjectToString() {
        return this.jsonObject.toString();
    }

    @Override
    public String toString() {
        return this.jsonObjectToString();
    }

    public byte[] toBytesAsUTF_8() {
        return jsonObjectToString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] toBytes() {
        return jsonObjectToString().getBytes();
    }

    public boolean saveAsConfig(@NonNull Path path) {
        try (var outputStreamWriter = new OutputStreamWriter(Files.newOutputStream(path), StandardCharsets.UTF_8)) {
            JsonUtils.PRETTY_JSON.toJson(jsonObject, outputStreamWriter);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Deprecated
    public boolean saveAsConfig0(@NonNull File backend) {
        if (backend.exists()) {
            backend.delete();
        }
        try (var writer = new OutputStreamWriter(new FileOutputStream(backend), StandardCharsets.UTF_8)) {
            JsonUtils.PRETTY_JSON.toJson(jsonObject, (writer));
            return true;
        } catch (IOException ex) {
            ex.getStackTrace();
        }
        return false;
    }

    public JsonDocument loadToExistingDocument(@NonNull File backend) {
        try (var reader = new InputStreamReader(new FileInputStream(backend), StandardCharsets.UTF_8)) {
            this.jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
            this.file = backend;
            return this;
        } catch (Exception ex) {
            ex.getStackTrace();
        }
        return new JsonDocument();
    }

    public JsonDocument loadToExistingDocument(@NonNull Path path) {
        try (var reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
            this.jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
            return this;
        } catch (Exception ex) {
            ex.getStackTrace();
        }
        return new JsonDocument();
    }

}
