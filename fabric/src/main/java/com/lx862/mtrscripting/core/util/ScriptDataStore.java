package com.lx862.mtrscripting.core.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.Scriptable;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.Undefined;
import org.mtr.mapping.holder.MinecraftClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persistent, script-scoped key-value store — the JCM "数据存储" API.
 *
 * <p>Obtained from any script context via {@code context.getDataStore()}
 * (JS: {@code ctx.getDataStore()}; C++: {@code api.context().call("getDataStore")}),
 * where "scoped" means <b>one store per script id</b>: every instance of the
 * same script (every PIDS block, every bound PSD panel, ...) shares the same
 * file. Scripts that need per-instance state derive their own key prefixes
 * from the instance identity they already read (e.g. {@code blockPos()}, for
 * PIDS/PSD/eyecandy, or the vehicle/siding id) — one store, two addressing
 * disciplines, no second API to learn.</p>
 *
 * <p>Values are JSON data: strings, numbers, booleans, null, arrays and
 * nested objects. JS scripts may pass plain JS objects/arrays directly
 * (converted through the relocated Rhino runtime); C++ scripts pass JSON
 * values through the Api bridge. Typed getters ({@link #getString(String, String)},
 * {@link #getNumber(String, double)}, {@link #getBoolean(String, boolean)})
 * cover the common scalar cases without conversion friction.</p>
 *
 * <p>Persistence: the store lives as one JSON file under
 * {@code <run dir>/data/mtrscripting/stores/<sanitized script id>.json} —
 * the same {@code data/mtrscripting} tree as {@link FilesJS}. Writes are
 * debounced (at most one disk write per store per flush window; the client
 * tick flushes every two seconds, plus a flush on resource reload and a JVM
 * shutdown hook), atomic (temp file + move) and size-capped
 * ({@value #MAX_KEYS} keys, {@value #MAX_FILE_BYTES} bytes serialized,
 * {@value #MAX_KEY_LENGTH}-char keys) so a runaway script cannot grow the
 * save directory unchecked. All methods are synchronized — script instances
 * of one script run on different executor threads.</p>
 *
 * <p>The store survives game restarts and resource reloads; it is <b>not</b>
 * world-scoped (it lives in the instance's run directory, like FilesJS
 * data), so the same script id shares data across worlds and servers. Treat
 * it as local configuration/accumulation state, not as world data.</p>
 */
public class ScriptDataStore {

    /** Maximum number of keys one store may hold. */
    public static final int MAX_KEYS = 256;
    /** Maximum serialized JSON size one store may reach, in bytes. */
    public static final int MAX_FILE_BYTES = 64 * 1024;
    /** Maximum length of a single key. */
    public static final int MAX_KEY_LENGTH = 128;
    /** Maximum nesting depth accepted from a script-provided value. */
    public static final int MAX_DEPTH = 32;

    /* serializeNulls: a stored null is a VALUE ("key exists, value null"),
       not a missing key — without it Gson would drop the entry on write and
       has() would flip to false after a restart. */
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().serializeNulls().create();
    private static final Map<String, ScriptDataStore> REGISTRY = new ConcurrentHashMap<>();
    private static final Path ROOT = MinecraftClient.getInstance().getRunDirectoryMapped().toPath().toAbsolutePath().normalize()
            .resolve("data").resolve("mtrscripting").resolve("stores");

    private final String scriptId;
    private final Path file;
    private final Map<String, JsonElement> values = new LinkedHashMap<>();
    private boolean loaded = false;
    private boolean dirty = false;

    private ScriptDataStore(String scriptId) {
        this.scriptId = scriptId;
        this.file = ROOT.resolve(fileNameOf(scriptId));
    }

    /**
     * The store of a script id. The same store instance is shared by every
     * context carrying that script id, so state survives instance disposal
     * and only the file's last flush wins.
     *
     * @param scriptId the script id (the context name), e.g. {@code jcm:pids_1a}
     * @return the store for that script
     */
    public static ScriptDataStore of(String scriptId) {
        final String id = scriptId == null ? "" : scriptId;
        return REGISTRY.computeIfAbsent(id, ScriptDataStore::new);
    }

    /* ------------------------------------------------------------------ */
    /* Script-facing API                                                   */
    /* ------------------------------------------------------------------ */

    /**
     * Gets a value by key.
     *
     * @param key the key
     * @return the decoded JSON value — String, Double, Boolean, null, Java
     * List or Java Map for arrays/objects — or null when the key is unset
     */
    public synchronized Object get(String key) {
        requireValidKey(key);
        loadIfNeeded();
        final JsonElement element = values.get(key);
        return element == null ? null : decode(element);
    }

    /**
     * Gets a value as a string.
     *
     * @param key the key
     * @param fallback value returned when unset or not a string/number/boolean
     * @return the stored string, or the fallback
     */
    public synchronized String getString(String key, String fallback) {
        final Object value = get(key);
        if (value instanceof String s) return s;
        if (value instanceof Number || value instanceof Boolean) return String.valueOf(value);
        return fallback;
    }

    /**
     * Gets a value as a number.
     *
     * @param key the key
     * @param fallback value returned when unset or not numeric
     * @return the stored number, or the fallback
     */
    public synchronized double getNumber(String key, double fallback) {
        final Object value = get(key);
        if (value instanceof Number n) return n.doubleValue();
        return fallback;
    }

    /**
     * Gets a value as a boolean.
     *
     * @param key the key
     * @param fallback value returned when unset or not a boolean
     * @return the stored boolean, or the fallback
     */
    public synchronized boolean getBoolean(String key, boolean fallback) {
        final Object value = get(key);
        if (value instanceof Boolean b) return b;
        return fallback;
    }

    /**
     * Sets a value. Accepted value types: String, Number, Boolean, null,
     * Java Map / Iterable, and JS objects/arrays (converted via the
     * relocated Rhino runtime). Functions and other opaque values are
     * rejected.
     *
     * @param key the key
     * @param value the JSON value to store
     */
    public synchronized void set(String key, Object value) {
        requireValidKey(key);
        loadIfNeeded();
        if (!values.containsKey(key) && values.size() >= MAX_KEYS) {
            throw new IllegalArgumentException("Script data store exceeded " + MAX_KEYS + " keys (script: " + scriptId + ")");
        }
        final JsonElement element = encode(value, 0);
        values.put(key, element);
        dirty = true;
        if (serializedSize() > MAX_FILE_BYTES) {
            values.remove(key);
            throw new IllegalArgumentException("Script data store exceeded " + MAX_FILE_BYTES + " bytes (script: " + scriptId + ")");
        }
    }

    /** Whether the key exists. */
    public synchronized boolean has(String key) {
        requireValidKey(key);
        loadIfNeeded();
        return values.containsKey(key);
    }

    /**
     * Removes a key.
     *
     * @return whether the key existed
     */
    public synchronized boolean delete(String key) {
        requireValidKey(key);
        loadIfNeeded();
        final boolean removed = values.remove(key) != null;
        if (removed) dirty = true;
        return removed;
    }

    /** Number of stored keys. */
    public synchronized int size() {
        loadIfNeeded();
        return values.size();
    }

    /**
     * All stored keys (insertion order). The returned set is a copy —
     * mutating it does not touch the store.
     */
    public synchronized Set<String> keys() {
        loadIfNeeded();
        return new LinkedHashSet<>(values.keySet());
    }

    /** Removes every key. */
    public synchronized void clear() {
        loadIfNeeded();
        if (!values.isEmpty()) {
            values.clear();
            dirty = true;
        }
    }

    /* ------------------------------------------------------------------ */
    /* Lifecycle (host side)                                               */
    /* ------------------------------------------------------------------ */

    /** Write the file now when dirty. Returns whether a write happened. */
    @ApiInternal
    public synchronized boolean flush() {
        if (!dirty) return false;
        try {
            writeNow();
            dirty = false;
            return true;
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot persist script data store " + scriptId, e);
        }
    }

    /** Flush every registered store (resource reload, shutdown). */
    @ApiInternal
    public static void flushAll() {
        for (ScriptDataStore store : REGISTRY.values()) {
            try {
                store.flush();
            } catch (RuntimeException e) {
                /* One failing store must not abort the flush of the rest. */
                com.lx862.jcm.mod.util.JCMLogger.warn("Failed to persist script data store {}: {}", store.scriptId, e.toString());
            }
        }
    }

    /** Flush and forget every store — used on resource reload. */
    @ApiInternal
    public static void reset() {
        flushAll();
        REGISTRY.clear();
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(ScriptDataStore::flushAll, "JCM-ScriptDataStore-Flush"));
    }

    /* ------------------------------------------------------------------ */
    /* Internals                                                           */
    /* ------------------------------------------------------------------ */

    private void loadIfNeeded() {
        if (loaded) return;
        loaded = true;
        try {
            if (Files.isRegularFile(file)) {
                final String content = Files.readString(file, StandardCharsets.UTF_8);
                final JsonElement parsed = parseJson(content);
                if (parsed.isJsonObject()) {
                    for (Map.Entry<String, JsonElement> entry : parsed.getAsJsonObject().entrySet()) {
                        if (values.size() < MAX_KEYS) values.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            /* A corrupt/truncated file (crash mid-write) degrades to an
               empty store; the next successful flush replaces it. */
            com.lx862.jcm.mod.util.JCMLogger.warn("Discarding unreadable script data store {}: {}", scriptId, e.toString());
        }
    }

    /* MC 1.17.1 bundles Gson 2.8.0, which predates the static
       JsonParser.parseString (added in Gson 2.8.6); every other target
       version ships a newer Gson. Manifold picks the right call per
       version — same pattern as NativeApiSession.parseJson. */
    private static JsonElement parseJson(String json) {
        #if MC_VERSION <= "11701"
        return new JsonParser().parse(json);
        #else
        return JsonParser.parseString(json);
        #endif
    }

    private void writeNow() throws IOException {
        final JsonObject root = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : values.entrySet()) {
            root.add(entry.getKey(), entry.getValue());
        }
        final byte[] bytes = GSON.toJson(root).getBytes(StandardCharsets.UTF_8);
        Files.createDirectories(file.getParent());
        final Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.write(temp, bytes);
        try {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private int serializedSize() {
        final JsonObject root = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : values.entrySet()) {
            root.add(entry.getKey(), entry.getValue());
        }
        return GSON.toJson(root).length();
    }

    private static void requireValidKey(String key) {
        if (key == null || key.isEmpty()) throw new IllegalArgumentException("Script data store key must be non-empty");
        if (key.length() > MAX_KEY_LENGTH) {
            throw new IllegalArgumentException("Script data store key longer than " + MAX_KEY_LENGTH + " characters");
        }
    }

    /** Stable, filesystem-safe file name for a script id. */
    private static String fileNameOf(String scriptId) {
        final String sanitized = scriptId.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (sanitized.length() <= 96 && !sanitized.isEmpty() && !sanitized.startsWith(".")) {
            return sanitized + ".json";
        }
        final String prefix = sanitized.substring(0, Math.min(64, sanitized.length()));
        return prefix + "-" + Integer.toHexString(scriptId.hashCode()) + ".json";
    }

    /* ---- value encoding (script objects -> JSON) ---- */

    private static JsonElement encode(Object value, int depth) {
        if (depth > MAX_DEPTH) {
            throw new IllegalArgumentException("Script data store value nesting exceeds " + MAX_DEPTH + " levels (circular value?)");
        }
        if (value == null || value instanceof Undefined) return JsonNull.INSTANCE;
        if (value instanceof String s) return new JsonPrimitive(s);
        if (value instanceof Boolean b) return new JsonPrimitive(b);
        if (value instanceof Character c) return new JsonPrimitive(c.toString());
        if (value instanceof Number n) return new JsonPrimitive(n);
        if (value instanceof Enum<?> e) return new JsonPrimitive(e.name());
        if (value instanceof JsonElement element) return element;
        if (value instanceof Map<?, ?> map) {
            final JsonObject object = new JsonObject();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                object.add(String.valueOf(entry.getKey()), encode(entry.getValue(), depth + 1));
            }
            return object;
        }
        if (value instanceof Iterable<?> iterable) {
            final JsonArray array = new JsonArray();
            for (Object item : iterable) {
                array.add(encode(item, depth + 1));
            }
            return array;
        }
        if (value instanceof Scriptable scriptable) {
            return encodeScriptable(scriptable, depth);
        }
        throw new IllegalArgumentException("Script data store values must be JSON data (string/number/boolean/null/array/object), got " + value.getClass().getName());
    }

    /** JS objects (NativeObject) and arrays (NativeArray) as JSON. */
    private static JsonElement encodeScriptable(Scriptable scriptable, int depth) {
        final Object[] ids = scriptable.getIds();
        boolean arrayLike = ids.length > 0;
        for (Object id : ids) {
            if (!(id instanceof Number)) {
                arrayLike = false;
                break;
            }
        }
        if (arrayLike) {
            final JsonArray array = new JsonArray();
            for (int i = 0; i < ids.length; i++) {
                final int index = ((Number) ids[i]).intValue();
                array.add(encode(scriptable.get(index, scriptable), depth + 1));
            }
            return array;
        }
        final JsonObject object = new JsonObject();
        for (Object id : ids) {
            final String name = String.valueOf(id);
            final Object member = id instanceof Number
                    ? scriptable.get(((Number) id).intValue(), scriptable)
                    : scriptable.get(name, scriptable);
            if (member == Scriptable.NOT_FOUND || member instanceof Undefined) continue;
            object.add(name, encode(member, depth + 1));
        }
        return object;
    }

    /* ---- value decoding (JSON -> script-friendly objects) ---- */

    private static Object decode(JsonElement element) {
        if (element == null || element.isJsonNull()) return null;
        if (element.isJsonPrimitive()) {
            final JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isBoolean()) return primitive.getAsBoolean();
            if (primitive.isNumber()) return primitive.getAsDouble();
            return primitive.getAsString();
        }
        if (element.isJsonArray()) {
            final List<Object> list = new ArrayList<>();
            for (JsonElement item : element.getAsJsonArray()) {
                list.add(decode(item));
            }
            return list;
        }
        if (element.isJsonObject()) {
            final Map<Object, Object> map = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                map.put(entry.getKey(), decode(entry.getValue()));
            }
            return map;
        }
        return null;
    }

    /** Read-only view of the raw JSON (host-side tooling / tests). */
    @ApiInternal
    public synchronized Map<String, JsonElement> rawValues() {
        loadIfNeeded();
        return Collections.unmodifiableMap(values);
    }
}
