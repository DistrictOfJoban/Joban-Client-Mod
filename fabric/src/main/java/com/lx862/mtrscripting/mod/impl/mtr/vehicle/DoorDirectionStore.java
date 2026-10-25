package com.lx862.mtrscripting.mod.impl.mtr.vehicle;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lx862.jcm.mapping.LoaderImpl;
import com.lx862.mtrscripting.mod.MTRScriptingMod;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Persistent per-platform memory of which sides the doors can open on —
 * "learn once, answer anywhere".
 *
 * <p>Why this exists: a Minecraft client only holds blocks for loaded
 * chunks. While a train runs the mainline, the next stop is typically far
 * outside the client's view distance, so the predictive world scan in
 * {@link VehicleWrapper#predictPlatformDoorDirection} reads void air at
 * the platform's blocks and finds nothing — the direction only becomes
 * non-"not" roughly when the train enters the platform track, i.e. when
 * the chunks around the stop finally load. This store closes that gap:
 * every time a platform's sides <b>are</b> observable (dwelling there,
 * doors opening there, or a successful scan while approaching it), the
 * answer is remembered under the platform's id and written to
 * {@code config/jsblock/door-directions.json}, so the memory survives
 * restarts. While predicting, a remembered platform answers without any
 * world scan at all.
 *
 * <p>Platform ids are random 64-bit values minted when the platform is
 * created and persisted in the world data, so they are stable across
 * sessions and cannot collide between worlds in practice.
 *
 * <p>Stored sides are <b>canonicalized to the platform rail's intrinsic
 * direction</b> (the {@code Position.compareTo} order of its two end
 * anchors — identical for every train, in either traversal direction),
 * not to any particular train's head: forward and reversed trains, and
 * trains of opposite routes calling at the same platform, all agree on
 * one entry. {@link VehicleWrapper} converts between head-relative and
 * canonical sides using the single boolean that says which way the head
 * faces along that rail.
 *
 * <p>Only observed truths are stored — a scan that found nothing is never
 * written, so a far-away (chunk-unloaded) platform can't poison its own
 * entry with a false "neither side". Entries refresh on every visit; the
 * disk file is rewritten lazily (debounced, off-thread, atomic move) and
 * flushed synchronously when the client leaves the world.
 */
public final class DoorDirectionStore {

    private static final Logger LOGGER = MTRScriptingMod.LOGGER;

    private static final String FILE_NAME = "door-directions.json";
    private static final int FORMAT_VERSION = 1;
    /* Hard caps so a broken or foreign file cannot balloon memory. */
    private static final int MAX_ENTRIES = 8192;
    private static final int PRUNE_TO = 4096;
    /* Debounce the async disk writes; the in-memory answer stays instant. */
    private static final long SAVE_INTERVAL_NANOS = 10_000_000_000L;

    private static final Object LOCK = new Object();
    private static final Map<Long, Entry> ENTRIES = new HashMap<>();
    private static boolean loaded = false;
    private static boolean ioWarned = false;
    private static boolean dirty = false;
    private static long lastSaveNanos = 0;
    private static boolean saveInFlight = false;

    static final class Entry {
        boolean left;
        boolean right;
        long at;

        Entry() {
        }

        Entry(boolean left, boolean right, long at) {
            this.left = left;
            this.right = right;
            this.at = at;
        }
    }

    private DoorDirectionStore() {
    }

    private static Path file() {
        return LoaderImpl.getConfigPath().resolve("jsblock").resolve(FILE_NAME);
    }

    /**
     * The remembered canonical sides for a platform, or null when unknown.
     * Never allocates on the hot path beyond the lookup itself.
     */
    public static Entry recall(long platformId) {
        ensureLoaded();
        synchronized(LOCK) {
            return ENTRIES.get(platformId);
        }
    }

    /**
     * Records the canonical sides observed for a platform. Only marks the
     * store dirty (and schedules a debounced save) when the answer actually
     * changes; silent no-op when nothing new is learned.
     */
    public static void remember(long platformId, boolean left, boolean right) {
        /* An observation that found no openable side is not evidence — it
           may just mean "chunks not loaded" — so never store it. */
        if(!left && !right) return;
        ensureLoaded();
        final long now = System.currentTimeMillis();
        synchronized(LOCK) {
            Entry entry = ENTRIES.get(platformId);
            if(entry == null) {
                if(ENTRIES.size() >= MAX_ENTRIES) pruneOldestLocked();
                entry = new Entry();
                ENTRIES.put(platformId, entry);
            }
            entry.at = now;
            if(entry.left != left || entry.right != right) {
                entry.left = left;
                entry.right = right;
                dirty = true;
            }
            if(dirty && !saveInFlight
                    && System.nanoTime() - lastSaveNanos >= SAVE_INTERVAL_NANOS) {
                requestSaveLocked();
            }
        }
    }

    /**
     * Writes the store to disk if anything changed since the last write.
     * Synchronous — meant for the rare "leaving the world" hook, not the
     * render path (which uses the debounced asynchronous saves).
     */
    public static void flush() {
        ensureLoaded();
        synchronized(LOCK) {
            while(saveInFlight) {
                try {
                    LOCK.wait(1000);
                } catch(InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            if(dirty) {
                requestSaveLocked();
                while(saveInFlight) {
                    try {
                        LOCK.wait(1000);
                    } catch(InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
    }

    /* ------------------------------------------------------------------ */

    /**
     * Snapshots the entries under the lock and hands them to a background
     * thread; the file write itself never touches shared state.
     */
    private static void requestSaveLocked() {
        final Map<Long, Entry> snapshot = new HashMap<>();
        for(Map.Entry<Long, Entry> e : ENTRIES.entrySet()) {
            final Entry src = e.getValue();
            snapshot.put(e.getKey(), new Entry(src.left, src.right, src.at));
        }
        dirty = false;
        lastSaveNanos = System.nanoTime();
        saveInFlight = true;
        final Thread thread = new Thread(() -> writeSnapshot(snapshot), "JCM door-direction save");
        thread.setDaemon(true);
        thread.start();
    }

    private static void writeSnapshot(Map<Long, Entry> snapshot) {
        try {
            final Path file = file();
            Files.createDirectories(file.getParent());
            final Path tmp = file.resolveSibling(FILE_NAME + ".tmp");
            try(final BufferedWriter writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                final JsonObject root = new JsonObject();
                root.addProperty("version", FORMAT_VERSION);
                final JsonObject entries = new JsonObject();
                for(Map.Entry<Long, Entry> e : snapshot.entrySet()) {
                    final JsonObject o = new JsonObject();
                    o.addProperty("left", e.getValue().left);
                    o.addProperty("right", e.getValue().right);
                    o.addProperty("at", e.getValue().at);
                    entries.add(Long.toString(e.getKey()), o);
                }
                root.add("entries", entries);
                writer.write(root.toString());
            }
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch(IOException atomicUnsupported) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch(IOException | RuntimeException e) {
            warnOnce("Could not write " + FILE_NAME + " (" + e + "); the door-direction memory stays in memory only.");
        } finally {
            synchronized(LOCK) {
                saveInFlight = false;
                LOCK.notifyAll();
            }
        }
    }

    private static void pruneOldestLocked() {
        final List<Map.Entry<Long, Entry>> byAge = new ArrayList<>(ENTRIES.entrySet());
        byAge.sort(Comparator.comparingLong(e -> e.getValue().at));
        int toRemove = ENTRIES.size() - PRUNE_TO;
        for(int i = 0; i < toRemove && i < byAge.size(); i++) {
            ENTRIES.remove(byAge.get(i).getKey());
        }
    }

    private static void ensureLoaded() {
        synchronized(LOCK) {
            if(loaded) return;
            loaded = true; /* Also on failure: never retry-parse a broken file every frame. */
        }
        final Map<Long, Entry> parsed = new HashMap<>();
        try(final BufferedReader reader = Files.newBufferedReader(file(), StandardCharsets.UTF_8)) {
            final JsonElement rootElement = JsonParser.parseReader(reader);
            if(!rootElement.isJsonObject()) return;
            final JsonObject root = rootElement.getAsJsonObject();
            final JsonElement entriesElement = root.get("entries");
            if(entriesElement == null || !entriesElement.isJsonObject()) return;
            for(Map.Entry<String, JsonElement> e : entriesElement.getAsJsonObject().entrySet()) {
                try {
                    final long id = Long.parseLong(e.getKey());
                    if(!e.getValue().isJsonObject()) continue;
                    final JsonObject o = e.getValue().getAsJsonObject();
                    final JsonElement leftElement = o.get("left");
                    final JsonElement rightElement = o.get("right");
                    final JsonElement atElement = o.get("at");
                    if(leftElement == null || rightElement == null || atElement == null) continue;
                    if(!leftElement.getAsBoolean() && !rightElement.getAsBoolean()) continue;
                    final Entry entry = new Entry(leftElement.getAsBoolean(), rightElement.getAsBoolean(), atElement.getAsLong());
                    parsed.put(id, entry);
                } catch(RuntimeException ignored) {
                    /* Skip a single malformed entry, keep the rest. */
                }
            }
        } catch(IOException | RuntimeException e) {
            /* Missing file is the normal first-run case; only real parse or IO
               trouble deserves a (single, quiet) note. */
            if(!(e instanceof java.nio.file.NoSuchFileException)) {
                warnOnce("Could not read " + FILE_NAME + " (" + e + "); starting with an empty door-direction memory.");
            }
            return;
        }
        synchronized(LOCK) {
            if(parsed.size() > MAX_ENTRIES) {
                /* Keep the freshest slice if a foreign file is huge. */
                final List<Map.Entry<Long, Entry>> byAge = new ArrayList<>(parsed.entrySet());
                byAge.sort(Comparator.comparingLong(x -> x.getValue().at));
                for(int i = 0; i < parsed.size() - PRUNE_TO && i < byAge.size(); i++) {
                    parsed.remove(byAge.get(i).getKey());
                }
            }
            ENTRIES.clear();
            ENTRIES.putAll(parsed);
        }
    }

    private static void warnOnce(String message) {
        /* Debug level on purpose: the store is a pure optimization — a
           broken or missing file must never spam the log. */
        LOGGER.debug("[MTR Scripting via JCM] {}", message);
    }
}
