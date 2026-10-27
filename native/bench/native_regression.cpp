#include <mtr/gfx.hpp>
#include <mtr/gfx2d.hpp>
#include <cstdio>
#include <cstring>
#include <vector>

int main() {
    JcmFrameInput in{};
    mtr::GraphicsTexture texture;
    texture.create(in, 5, 4);
    mtr::FrameRecorder frame;
    alignas(8) unsigned char records[128]{};
    float matrices[16]{}, floats[16]{};
    char strings[16]{};
    uint8_t pixels[256]{};
    frame.attach(records, matrices, strings, pixels, sizeof(pixels), floats,
                 sizeof(records), sizeof(matrices), sizeof(strings), sizeof(floats));
    frame.begin_frame();
    texture.upload(frame); // consume initial full-image dirtiness
    frame.begin_frame();
    for (int y = 0; y < 4; y++)
        for (int x = 0; x < 5; x++)
            texture.pixels32()[y * 5 + x] = 0xff000000u | (y << 8) | x;
    texture.mark_dirty(1, 1);
    texture.mark_dirty(2, 2);
    texture.upload(frame);
    JcmFrameOutput out{};
    frame.write_output(out);
    auto* upload = static_cast<const JcmDrawTextureUpload*>(out.records);
    const uint32_t expected[] = {0xff000101u, 0xff000102u, 0xff000201u, 0xff000202u};
    if (out.record_count != 1 || upload->dirty_w != 2 || upload->dirty_h != 2
            || upload->pixel_data_len != sizeof(expected)
            || std::memcmp(out.pixel_arena + upload->pixel_data_offset,
                           expected, sizeof(expected)) != 0) {
        std::fprintf(stderr, "FAIL: cropped texture upload must preserve row stride\n");
        return 1;
    }
    std::puts("NATIVE REGRESSION OK: cropped multi-row BGRA texture upload");
    mtr::GraphicsTexture outline;
    outline.create(in, 320, 120);
    outline.fill(0);
    mtr::Gfx2D g(outline);
    g.set_color(0xffffffffu);
    g.draw_round_rect(20, 20, 280, 80, 80, 80, 3);
    auto ink = [&](int x, int y) { return outline.pixels32()[y * 320 + x] != 0; };
    if (!ink(160, 20) || !ink(160, 100) || !ink(20, 60) || !ink(300, 60)
            || ink(160, 60) || ink(20, 20) || ink(300, 100)) {
        std::fprintf(stderr, "FAIL: rounded outline must be closed, axis-aligned and empty inside\n");
        return 1;
    }
    for (int y = 28; y < 93; y++) for (int x = 70; x < 251; x++) {
        if (ink(x, y)) {
            std::fprintf(stderr, "FAIL: rounded outline has an interior chord at %d,%d\n", x, y);
            return 1;
        }
    }
    std::puts("NATIVE REGRESSION OK: closed rounded outline without diagonal chords");

    /* ---- fullAA()/noAA() (upstream GraphicsTexture port) ----
       Default (no_aa): hard edges — an oval's boundary ring is all-or-
       nothing per pixel. full_aa(): coverage blend — the same ring gains
       intermediate alpha values, the interior stays fully opaque and the
       hard-edge ink total stays within the feather band. */
    {
        mtr::GraphicsTexture hard, soft;
        JcmFrameInput in{};
        hard.create(in, 64, 64);
        soft.create(in, 64, 64);
        mtr::Gfx2D gh(hard), gs(soft);
        gh.no_aa();  /* default */
        gs.full_aa();
        gh.set_color(0xFFFFFFFFu); gs.set_color(0xFFFFFFFFu);
        gh.fill_oval(8, 8, 48, 48);
        gs.fill_oval(8, 8, 48, 48);
        if (gh.aa() || !gs.aa()) { std::fprintf(stderr, "FAIL: aa toggle\n"); return 1; }
        int hard_partial = 0, soft_partial = 0, soft_full = 0;
        for (int i = 0; i < 64 * 64; i++) {
            const uint32_t h = hard.pixels32()[i], s = soft.pixels32()[i];
            const uint8_t ha = (uint8_t)(h >> 24), sa = (uint8_t)(s >> 24);
            if (ha != 0 && ha != 0xFF) hard_partial++;
            if (sa != 0 && sa != 0xFF) soft_partial++;
            if (sa == 0xFF) soft_full++;
        }
        if (hard_partial != 0) { std::fprintf(stderr, "FAIL: hard-edge oval must be binary\n"); return 1; }
        if (soft_partial < 8) { std::fprintf(stderr, "FAIL: AA oval must have a blended rim (%d)\n", soft_partial); return 1; }
        if (soft_full == 0) { std::fprintf(stderr, "FAIL: AA oval interior must stay opaque\n"); return 1; }
        /* AA lines: soft alpha must appear along a diagonal stroke. */
        hard.fill(0); soft.fill(0);
        gh.draw_line(4, 4, 60, 52, 2);
        gs.draw_line(4, 4, 60, 52, 2);
        int line_partial = 0;
        for (int i = 0; i < 64 * 64; i++) {
            const uint8_t sa = (uint8_t)(soft.pixels32()[i] >> 24);
            if (sa != 0 && sa != 0xFF) line_partial++;
        }
        if (line_partial < 4) { std::fprintf(stderr, "FAIL: AA line must blend (%d)\n", line_partial); return 1; }
        /* no_aa() restores hard edges (clear the AA leftovers first). */
        gs.no_aa();
        soft.fill(0);
        gs.fill_oval(8, 8, 48, 48);
        int back_partial = 0;
        for (int i = 0; i < 64 * 64; i++) {
            const uint8_t sa = (uint8_t)(soft.pixels32()[i] >> 24);
            if (sa != 0 && sa != 0xFF) back_partial++;
        }
        if (back_partial != 0) { std::fprintf(stderr, "FAIL: no_aa() must restore hard edges\n"); return 1; }
        std::puts("NATIVE REGRESSION OK: fullAA/noAA coverage blending");
    }

    /* Pixel-arena lifecycle: a spike frame grows the arena, the decaying
       idle shrink must hand the excess back to the allocator (down to the
       1 MB floor) while a quiet workload continues, and a later frame
       after the resize must still produce intact data. Guards the
       C++-mode reclaim policy in FrameRecorder against regressing into
       the old "arena pinned at the session's largest frame forever". */
    {
        mtr::FrameRecorder rec;
        static uint8_t big_records[sizeof(JcmDrawText) * 256];
        static float big_mats[128 * 16];
        static char big_strings[32 * 1024];
        static uint8_t big_pixels[1 << 20];
        static float big_floats[1024];
        rec.attach(big_records, big_mats, big_strings, big_pixels, sizeof(big_pixels), big_floats);
        JcmFrameOutput shrink_out{};
        std::vector<uint8_t> spike(8 << 20);
        rec.begin_frame();
        rec.push_pixels(spike.data(), static_cast<int64_t>(spike.size()));
        rec.write_output(shrink_out);
        const int64_t peak = rec.pixel_capacity();
        if (peak < static_cast<int64_t>(spike.size())) {
            std::fprintf(stderr, "FAIL: pixel arena did not grow for the spike\n");
            return 1;
        }
        for (int i = 0; i < 600; i++) { rec.begin_frame(); rec.write_output(shrink_out); }
        const int64_t idle = rec.pixel_capacity();
        if (idle > (1152 << 10)) {
            std::fprintf(stderr, "FAIL: pixel arena still pinned at %lld KB after idle shrink\n",
                         static_cast<long long>(idle / 1024));
            return 1;
        }
        uint8_t small[4096];
        std::memset(small, 0xAB, sizeof(small));
        rec.begin_frame();
        const int64_t off = rec.push_pixels(small, sizeof(small));
        rec.write_output(shrink_out);
        if (shrink_out.pixel_arena_len != sizeof(small)
                || std::memcmp(shrink_out.pixel_arena + off, small, sizeof(small)) != 0) {
            std::fprintf(stderr, "FAIL: pixel data lost across idle shrink\n");
            return 1;
        }
        std::printf("NATIVE REGRESSION OK: pixel arena %lld KB -> %lld KB after idle, data intact\n",
                    static_cast<long long>(peak / 1024), static_cast<long long>(idle / 1024));
    }

    /* ABI 10 deferred reclamation: with JcmHostServices.defer_free installed,
       a replaced pixel arena must be handed to the HOST (pointer + size),
       never freed on the calling thread; the recorder must also keep
       working when the hook is absent (older host) and when the hook frees
       eagerly. Guards the stutter-free reclaim policy against regressing
       into a synchronous free inside begin_frame(). */
    {
        mtr::FrameRecorder rec;
        static uint8_t big_records[sizeof(JcmDrawText) * 256];
        static float big_mats[128 * 16];
        static char big_strings[32 * 1024];
        static uint8_t big_pixels[1 << 20];
        static float big_floats[1024];
        rec.attach(big_records, big_mats, big_strings, big_pixels, sizeof(big_pixels), big_floats);

        struct DeferStats { int calls; int64_t bytes; } defer{0, 0};
        JcmFrameOutput defer_out{};
        JcmHostServices host{};
        host.user = &defer;
        host.defer_free = [](void* user, void* pointer, size_t size) {
            auto* stats = static_cast<DeferStats*>(user);
            if (!pointer) return;
            stats->calls++;
            stats->bytes += static_cast<int64_t>(size);
            std::free(pointer);   /* the test host IS the reclaimer */
        };
        rec.set_defer_hook(&host);

        std::vector<uint8_t> spike(8 << 20);
        rec.begin_frame();
        rec.push_pixels(spike.data(), static_cast<int64_t>(spike.size()));
        rec.write_output(defer_out);
        const int64_t peak = rec.pixel_capacity();
        if (defer.calls != 0) {
            std::fprintf(stderr, "FAIL: first grow deferred the ATTACHED static arena\n");
            return 1;
        }
        for (int i = 0; i < 600; i++) { rec.begin_frame(); rec.write_output(defer_out); }
        const int64_t idle = rec.pixel_capacity();
        if (idle > (1152 << 10)) {
            std::fprintf(stderr, "FAIL: deferred path did not shrink the pixel arena\n");
            return 1;
        }
        if (defer.calls < 1 || defer.bytes < (7LL << 20)) {
            std::fprintf(stderr, "FAIL: replaced arenas freed WITHOUT the defer hook "
                                 "(%d calls, %lld bytes)\n", defer.calls,
                         static_cast<long long>(defer.bytes));
            return 1;
        }
        uint8_t small[4096];
        std::memset(small, 0xAB, sizeof(small));
        rec.begin_frame();
        const int64_t off = rec.push_pixels(small, sizeof(small));
        rec.write_output(defer_out);
        if (defer_out.pixel_arena_len != sizeof(small)
                || std::memcmp(defer_out.pixel_arena + off, small, sizeof(small)) != 0) {
            std::fprintf(stderr, "FAIL: pixel data lost across deferred shrink\n");
            return 1;
        }
        /* Hook removed -> synchronous free again (older host behaviour). */
        rec.set_defer_hook(nullptr);
        const int defer_calls_before = defer.calls;
        std::vector<uint8_t> spike2(8 << 20);
        rec.begin_frame();
        rec.push_pixels(spike2.data(), static_cast<int64_t>(spike2.size()));
        rec.write_output(defer_out);
        for (int i = 0; i < 600; i++) { rec.begin_frame(); rec.write_output(defer_out); }
        if (rec.pixel_capacity() > (1152 << 10)) {
            std::fprintf(stderr, "FAIL: synchronous shrink path regressed\n");
            return 1;
        }
        if (defer.calls != defer_calls_before) {
            std::fprintf(stderr, "FAIL: null hook still deferred (must free synchronously)\n");
            return 1;
        }
        std::printf("NATIVE REGRESSION OK: %d arena replacement(s) (%lld KB) deferred to host reclaimer, %lld KB -> %lld KB, data intact\n",
                    defer.calls, static_cast<long long>(defer.bytes / 1024),
                    static_cast<long long>(peak / 1024), static_cast<long long>(idle / 1024));
    }
    return 0;
}
