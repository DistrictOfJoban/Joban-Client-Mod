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
 * <p>Open-addressed linear probing, power-of-two capacity, cleared wholesale
 * on overflow or reload (the same graceful-degradation policy as
 * NativeHost's glyph-mask cache). Render thread only.
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
    private static final Entry[] TABLE = new Entry[CAPACITY];
    private static int size;

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

    private static int hash(int a, int b, int c) {
        int h = 0x811c9dc5;
        h = 31 * h + a;
        h = 31 * h + b;
        h = 31 * h + c;
        h ^= h >>> 15;
        h *= 0x2545f491;
        h ^= h >>> 13;
        return h;
    }

    private static Entry find(ByteBuffer arena, int offset, int length) {
        if (offset < 0 || length < 0 || (long) offset + length > arena.limit()) {
            throw new IllegalArgumentException("Invalid native string reference");
        }
        final byte[] bytes = readScratch(arena, offset, length);
        final int hash = hash(offset, length, arena.get(offset));
        int slot = hash & MASK;
        for (int probe = 0; probe < 8; probe++) {
            final Entry entry = TABLE[slot];
            if (entry == null) {
                /* MISS -> build once, insert (the arena range is copied out
                   because the scratch buffer is shared). */
                final Entry created = new Entry();
                created.hash = hash;
                created.bytes = Arrays.copyOf(bytes, length);
                created.string = new String(created.bytes, 0, length, StandardCharsets.UTF_8);
                if (size >= CAPACITY / 2) clear();
                TABLE[slot] = created;
                size++;
                return created;
            }
            if (entry.hash == hash && entry.bytes.length == length
                    && Arrays.equals(entry.bytes, 0, length, bytes, 0, length)) {
                return entry;
            }
            slot = (slot + 1) & MASK;
        }
        /* Table too dense for this key (should not happen with size < half
           capacity): fall through to a transient entry — allocates, but
           correct. */
        final Entry transientEntry = new Entry();
        transientEntry.hash = hash;
        transientEntry.bytes = Arrays.copyOf(bytes, length);
        transientEntry.string = new String(transientEntry.bytes, 0, length, StandardCharsets.UTF_8);
        clear();
        return transientEntry;
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
