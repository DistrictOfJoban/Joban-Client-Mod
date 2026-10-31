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
 * <p>Fixed-group (set-associative) layout: 16 sets x 8 ways, 128 entries
 * total. A key's lookup, insertion and eviction happen ONLY inside its own
 * 8 slots — no probe chains crossing the table, so an entry is always
 * findable exactly where it was inserted. A full set rotates a victim.
 * Cleared wholesale only on reload. Render thread only.
 */
final class NativeStringCache {

    /** Entries: {hash, bytes, string, identifier}. Slots rotate in place. */
    private static final class Entry {
        int hash;
        byte[] bytes;
        String string;
        Identifier identifier;
    }

    private static final int SET_COUNT = 16;
    private static final int WAYS = 8;
    private static final Entry[] TABLE = new Entry[SET_COUNT * WAYS];
    private static final byte[] NEXT_VICTIM = new byte[SET_COUNT];

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
        Arrays.fill(NEXT_VICTIM, (byte) 0);
    }

    private static int hashContent(byte[] bytes, int length) {
        int hash = 0x811c9dc5;
        for (int i = 0; i < length; i++) {
            hash = (hash ^ bytes[i]) * 0x01000193;
        }
        hash ^= hash >>> 15;
        hash *= 0x2545f491;
        hash ^= hash >>> 13;
        return hash;
    }

    private static Entry find(ByteBuffer arena, int offset, int length) {
        if (offset < 0 || length < 0 || (long) offset + length > arena.limit()) {
            throw new IllegalArgumentException("Invalid native string reference");
        }
        if (length == 0) return EMPTY;   /* legal empty range: no arena byte is read */
        if (scratch.length < length) {
            scratch = new byte[Math.max(length, scratch.length * 2)];
        }
        for (int i = 0; i < length; i++) {
            scratch[i] = arena.get(offset + i);
        }

        final int hash = hashContent(scratch, length);
        final int set = hash & (SET_COUNT - 1);
        final int base = set * WAYS;
        int empty = -1;

        /* Scan the WHOLE group before deciding: an empty way only marks a
           candidate slot, it never ends the scan (the old open-addressed
           code could stop at an empty probe slot that sat BEFORE the slot
           the entry was actually stored in — a cache-full insert parked
           behind such a gap became unfindable and the content re-allocated
           forever). */
        for (int way = 0; way < WAYS; way++) {
            final int slot = base + way;
            final Entry entry = TABLE[slot];
            if (entry == null) {
                if (empty < 0) empty = slot;
                continue;
            }
            if (entry.hash == hash && entry.bytes.length == length
                    && Arrays.equals(entry.bytes, 0, length, scratch, 0, length)) {
                return entry;
            }
        }

        final int slot;
        if (empty >= 0) {
            slot = empty;
        } else {
            /* Full group: rotate one victim. Keeps capacity bounded, keeps
               the just-inserted entry immediately findable, and never
               touches other groups. */
            final int victim = NEXT_VICTIM[set] & (WAYS - 1);
            slot = base + victim;
            NEXT_VICTIM[set] = (byte) ((victim + 1) & (WAYS - 1));
        }
        final Entry created = new Entry();
        created.hash = hash;
        created.bytes = Arrays.copyOf(scratch, length);
        created.string = new String(created.bytes, StandardCharsets.UTF_8);
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
