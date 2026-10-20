/**
 * ffm_host.h — Yanyang "Panama FFM" host vtable (bridge <-> Java, no JNI).
 *
 * The bridge's ORIGINAL Java interface was JNIEnv-shaped: the module host
 * services (JcmHostServices in mtr_native.h) called back into the JVM with
 * FindClass/GetMethodID/Call*Method, which ties the bridge to a
 * System.load()-ed JNI library and puts every upcall on the JNI
 * reflection machinery. This header defines the JNI-FREE replacement: a
 * plain table of C function pointers that Java fills with
 * java.lang.foreign UPCALL STUBS (Linker.upcallStub) and hands to the
 * bridge once via the jcm_ffm_init() export.
 *
 * Calling-thread contract
 * -----------------------
 * Every entry is invoked by bridge code that is ALREADY executing inside a
 * Java->C++ FFM downcall (a frame render, a module dispose, an api
 * round-trip), i.e. on a JVM-owned thread — the only kind of thread an
 * upcall stub may legally be entered from. The bridge's background
 * reclaimer thread NEVER touches this table.
 *
 * Memory conventions
 * ------------------
 *   - Every `const char*` argument arrives as a RAW address plus a byte
 *     length; the callee reads it synchronously inside the call.
 *   - Every output parameter (`uint8_t* pixels`, `double* out_metrics`,
 *     `int32_t* out_bounds`, `int32_t* out_handles`, `int32_t* out_len`)
 *     is a raw address the callee writes before returning; the bridge
 *     copies what it needs out of it synchronously.
 *   - invoke_api returns a raw address of the reply (owned by Java's
 *     per-thread reused reply buffer) valid until the NEXT invoke_api
 *     entry on the same thread; the bridge copies it out immediately,
 *     exactly like the JNI path copied the returned jbyteArray.
 *
 * ABI rules
 * --------
 *   - `abi` MUST equal JCM_FFM_HOST_ABI. Unknown higher abis with a
 *     larger vtable_bytes are still accepted (the bridge only reads the
 *     prefix it knows); the byte size is validated so a truncated table
 *     never reads past the caller's buffer.
 *   - Null entries are legal: the bridge falls back to the legacy JNI
 *     host (when one is installed) or to the "no host" degradation the
 *     headless path already uses.
 *
 * License: MIT (aligned with Joban Client Mod's LICENSE)
 */
#ifndef MTR_FFM_HOST_H
#define MTR_FFM_HOST_H

#include <stdint.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

#define JCM_FFM_HOST_ABI 1

/* JcmHostServices.acquire_model — returns the host model handle (>= 0) or
   -1. The bridge adds its 0x40000000 "host-acquired" tag exactly like the
   JNI path did. */
typedef int64_t (*JcmFfmAcquireModel)(const char* model_path, int32_t path_len);

/* JcmHostServices.release_model for host-acquired handles (the flag bit is
   already stripped by the bridge). */
typedef void (*JcmFfmReleaseModel)(int64_t host_handle);

/* JcmHostServices.rasterize_text — rasterize into the raw BGRA8 pixel
   window at `pixels` (out_w*out_h*4 bytes); returns the glyph count or -1. */
typedef int32_t (*JcmFfmRasterizeText)(const char* utf8, int32_t len,
                                       int32_t x, int32_t y, int32_t max_w,
                                       int32_t r, int32_t g, int32_t b,
                                       uint8_t* pixels, int32_t out_w, int32_t out_h);

/* JcmHostServices.measure_font_text — writes 4 doubles (width, ascent,
   descent, line_height) to out_metrics; returns 0 or -1. */
typedef int32_t (*JcmFfmMeasureFontText)(const char* font_resource, int32_t font_len,
                                         const char* utf8, int32_t len,
                                         double size, double sx, double sy,
                                         double* out_metrics);

/* JcmHostServices.draw_font_text — draws into the raw pixel window and
   writes 4 ints (x0, y0, x1, y1) to out_bounds; returns 0 or -1. */
typedef int32_t (*JcmFfmDrawFontText)(const char* font_resource, int32_t font_len,
                                      const char* utf8, int32_t len,
                                      double size, double x, double baseline,
                                      double sx, double sy, double compression,
                                      uint32_t argb,
                                      int32_t clip_x0, int32_t clip_y0,
                                      int32_t clip_x1, int32_t clip_y1,
                                      uint8_t* pixels, int32_t width, int32_t height,
                                      int32_t* out_bounds);

/* resolve_captured_resources — build the instance's requested textures and
   quad models. Inputs are the bridge's own vectors (read in place, the
   bridge thread is blocked inside this call); out_handles receives
   tex_count + quad_count int32 handles (texture slots first, then model
   slots). Returns 0 on success, -1 on failure (the bridge keeps the
   placeholder handles and drops the affected records). */
typedef int32_t (*JcmFfmCreateResources)(int32_t tex_count, int32_t quad_count,
                                         const int32_t* widths, const int32_t* heights,
                                         const float* quad_verts, const float* quad_uv,
                                         const int32_t* quad_slots, const int32_t* quad_stages,
                                         int32_t* out_handles);

/* JcmHostServices.invoke_api — one Java API round-trip. Returns the reply
   address and writes the reply byte length to *out_len; the reply stays
   valid until the next invoke_api entry on the same thread. */
typedef int64_t (*JcmFfmInvokeApi)(const char* session_key, int32_t key_len,
                                   const char* request, int32_t request_len,
                                   int32_t* out_len);

/* All pointer-sized fields are 8-byte aligned on every supported LP64
   target (x64 / arm64), and `abi` comes first so the layout is
   prefix-stable across future additions. */
typedef struct JcmFfmHostVTable {
    uint64_t                abi;              /* JCM_FFM_HOST_ABI             */
    JcmFfmAcquireModel      acquire_model;    /* may be null                  */
    JcmFfmReleaseModel      release_model;    /* may be null                  */
    JcmFfmRasterizeText     rasterize_text;   /* may be null                  */
    JcmFfmMeasureFontText   measure_font_text;/* may be null                  */
    JcmFfmDrawFontText      draw_font_text;   /* may be null                  */
    JcmFfmCreateResources   create_resources; /* may be null                  */
    JcmFfmInvokeApi         invoke_api;       /* may be null                  */
} JcmFfmHostVTable;

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* MTR_FFM_HOST_H */
