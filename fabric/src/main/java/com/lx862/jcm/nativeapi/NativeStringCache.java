package com.lx862.jcm.nativeapi;

import org.mtr.mapping.holder.Identifier;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Zero-allocation string/identifier intern table for the native record
 * replay.
 *
 * <p>C++ scripts emit the same strings every frame (destination names, font
 * ids, sound ids); the replay used to decode them into a fresh
 * {@code byte[]}, a fresh {@code String} and — for ids — a fresh parsed
 * {@link Identifier} per record per frame. This table hashes the arena bytes
 * directly (no decode on the hit path) and keeps the decoded artifacts
 * alive, so a steady frame does not allocate anything for repeated strings.
 *
 * <p>Open-addressed linear probing, power-of-two capacity; cleared wholesale
 * only on reload (the same graceful-degradation policy as NativeHost's
 * glyph-mask cache). Overflow used to clear the whole table too — it now
 * evicts in place instead (bounded eviction), see {@link #find}. Render
 * thread only.
 */
final class NativeStringCache {

    /** Entries: {hash, bytes, string, identifier}. Slots rotate in place. */
    private static final class Entry {
        int hash;
        byte[] bytes;
        String string;
        Identifier identifier;
    }

    private static final int CAPACITY = 256;   /* power of two */
    private static final int MASK = CAPACITY - 1;
    /** Size limit (kept from the old design): beyond it new content evicts
     *  instead of filling more slots, so the table stays at half load. */
    private static final int LIMIT = CAPACITY / 2;
    private static final Entry[] TABLE = new Entry[CAPACITY];
    private static int size;

    /** Shared entry for length-0 references. A zero-length range is LEGAL —
     *  an empty string parked at the very end of the arena has
     *  {@code offset == limit} — and must not read a single arena byte (the
     *  old hash read {@code arena.get(offset)} unconditionally and threw
     *  IndexOutOfBoundsException there, killing the rest of the frame's
     *  records plus the 4-second error backoff for the whole script). */
    private static final Entry EMPTY = new Entry();
    static {
        EMPTY.bytes = new byte[0];
        EMPTY.string = "";
    }

    /** Scratch decode buffer, grown on demand, reused across calls. */
    private static byte[] scratch = new byte[64];

    private NativeStringCache() {
    }

    static void clear() {
        Arrays.fill(TABLE, null);
        size = 0;
    }

    /** Scratch-view bytes of an arena range (no allocation). */
    private static byte[] readScratch(ByteBuffer arena, int offset, int length) {
        if (scratch.length < length) scratch = new byte[Math.max(length, scratch.length * 2)];
        for (int i = 0; i < length; i++) {
            scratch[i] = arena.get(offset + i);
        }
        return scratch;
    }

    /* FNV-1a over the FULL UTF-8 content. The old hash mixed only the length
       and the FIRST byte: same-length Chinese station names overwhelmingly
       share their UTF-8 lead byte (the common 3-byte sequence for CJK),
       so different contents piled into one probe cluster and the ninth
       collision flushed the whole table every few frames. The content is
       already in scratch by the time this runs, so the full walk allocates
       nothing. Byte-level equality below still settles real hash
       collisions.

       Hash WITHOUT the arena offset on purpose: identical content at a
       shifted offset (a marquee-length change shifts every later string
       in the arena) is the SAME string and must keep hitting the same
       entry. Equality is content-based (Arrays.equals), so offset changes
       stay correct either way — only the hit rate improves. */
    private static int hashContent(byte[] bytes, int length) {
        int h = 0x811c9dc5;
        for (int i = 0; i < length; i++) {
            h = (h ^ bytes[i]) * 0x01000193;
        }
        h ^= h >>> 15;
        h *= 0x2545f491;
        h ^= h >>> 13;
        return h;
    }

    private static Entry find(ByteBuffer arena, int offset, int length) {
        if (offset < 0 || length < 0 || (long) offset + length > arena.limit()) {
            throw new IllegalArgumentException("Invalid native string reference");
        }
        if (length == 0) return EMPTY;   /* legal empty range: no arena byte is read */
        final byte[] bytes = readScratch(arena, offset, length);
        final int hash = hashContent(bytes, length);
        int slot = hash & MASK;
        int victim = -1;   /* first occupied slot of the probe run: the eviction target */
        for (int probe = 0; probe < 8; probe++) {
            final Entry entry = TABLE[slot];
            if (entry == null) {
                if (size < LIMIT) return insert(slot, hash, bytes, length);
                /* At the size limit: evict rather than grow (the limit keeps
                   the load factor at one half, which is what makes the short
                   probe run reliable in the first place). */
                break;
            }
            if (entry.hash == hash && entry.bytes.length == length
                    && Arrays.equals(entry.bytes, 0, length, bytes, 0, length)) {
                return entry;
            }
            if (victim < 0) victim = slot;
            slot = (slot + 1) & MASK;
        }
        /* Probe run exhausted (8 occupied, no match) or limit reached: BOUNDED
           EVICTION of the run's first slot. The old policy cleared the WHOLE
           table here, so one burst of distinct strings (a route change, a
           mixed-language consist) threw away every hot entry and re-decoded
           the steady frame for several frames. Evicting one cold-enough
           victim keeps the rest interned; the evicted content simply
           re-interns (one Entry + one byte[] + one String) the next time it
           appears. `size` is not touched — eviction replaces in place. */
        if (victim < 0) {
            /* Empty probe cluster at the size limit (slot 0 of the run was
               null): pick the nearest occupied slot instead. The table is at
               half load, so this walk is a couple of steps in practice. */
            victim = slot;
            while (TABLE[victim] == null) victim = (victim + 1) & MASK;
        }
        return insert(victim, hash, bytes, length);
    }

    /** Builds and stores an entry at `slot`; `size` counts only genuinely new
     *  slots, so eviction (occupied slot) leaves it unchanged. */
    private static Entry insert(int slot, int hash, byte[] bytes, int length) {
        final Entry created = new Entry();
        created.hash = hash;
        created.bytes = Arrays.copyOf(bytes, length);
        created.string = new String(created.bytes, 0, length, StandardCharsets.UTF_8);
        if (TABLE[slot] == null) size++;
        TABLE[slot] = created;
        return created;
    }

    /** Decoded string for an arena range; the same String object is returned
     *  every frame while the content is unchanged. */
    static String string(ByteBuffer arena, int offset, int length) {
        return find(arena, offset, length).string;
    }

    /** Parsed identifier for an arena range; parsed exactly once per
     *  distinct content. */
    static Identifier identifier(ByteBuffer arena, int offset, int length) {
        final Entry entry = find(arena, offset, length);
        if (entry.identifier == null) {
            entry.identifier = new Identifier(entry.string);
        }
        return entry.identifier;
    }
}
