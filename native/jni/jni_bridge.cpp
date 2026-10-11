/**
 * jni_bridge.cpp — C-side dispatcher behind NativeScriptManager.java.
 *
 * Java side (fabric/src/main/java/com/lx862/jcm/nativeapi/
 * NativeScriptManager.java) does:
 *
 *     static { System.loadLibrary("jcm_native_bridge"); }
 *     private native long   nOpen(String path);
 *     private native void   nClose(long handle);
 *     private native int    nAbiVersion(long handle);
 *     private native String nScriptType(long handle);
 *     private native String nScriptId(long handle);
 *     private native long   nStateSize(long handle);
 *     private native NativeFrame nRender(long handle, String instanceKey,
 *                                        ByteBuffer snapshot, int resourceKind);
 *     private native void   nDisposeInstance(long handle, String instanceKey);
 *
 * This file implements exactly those entry points on top of the C ABI in
 * include/mtr/mtr_native.h:
 *
 *   nOpen      — dlopen the script module, resolve the 5 mtr* exports,
 *                keep a per-instance state map (one zeroed block of
 *                mtrStateSize() bytes per instanceKey, mirroring JCM's
 *                per-ScriptInstance `state` JS object).
 *   nRender    — ONE boundary crossing per frame:
 *                  direct ByteBuffer snapshot → JcmFrameInput (the last
 *                  snapshot is kept per instance so dispose can rebuild
 *                  the full input), lazy mtrCreate on first use,
 *                  mtrRender, then the output arenas are wrapped as NIO
 *                  ByteBuffers into a NativeFrame — zero copies.
 *   nDisposeInstance / nClose — mtrDispose for one / all instances,
 *                then dlclose.
 *
 * Host services (reference implementation): model/texture handles are
 * opaque ids handed out by this bridge (a production host replaces these
 * with real ModelJS / GraphicsTexture registries through its own JNI
 * callbacks — the function-table shape is identical); rasterize_text
 * returns -1 so scripts fall back to their built-in bitmap font; log
 * goes to stderr.
 *
 * Threading: JCM serializes script execution per module on one
 * background executor (same model as the JS pipeline); the bridge adds
 * a per-module mutex so reload paths stay safe.
 *
 * Build (per platform):
 *   Linux   : g++ -std=c++17 -O2 -fPIC -shared -I<java_include> \
 *             -I<java_include>/linux -I../include jni_bridge.cpp \
 *             -o libjcm_native_bridge.so -ldl
 *   Windows : cl /LD /I<java_include> /I<java_include>\win32 ... → jcm_native_bridge.dll
 *   macOS   : clang++ ... → libjcm_native_bridge.dylib
 * (CI builds all three — see .github/workflows/native-build.yml)
 *
 * NOTE: the output arenas are owned by the module and reused next frame;
 * the Java replay step must finish before the next nRender call (this is
 * the same capture-then-replay discipline JCM's ScriptRenderManager
 * already enforces on the render thread).
 */

#include <jni.h>
#include <mtr/mtr_native.h>

#include <cstdio>
#include <cstring>
#include <mutex>
#include <new>
#include <string>
#include <unordered_map>
#include <vector>

#if defined(_WIN32)
#include <windows.h>
#else
#include <dlfcn.h>
#endif

/* ------------------------------------------------------------------ */
/* module + instance bookkeeping                                       */
/* ------------------------------------------------------------------ */

namespace {

struct Instance {
    std::vector<uint8_t> state;         /* mtrStateSize() zeroed block  */
    std::vector<uint8_t> last_snapshot; /* for safe dispose() rebuild   */
    bool created = false;               /* mtrCreate called?            */

    /* v4/v5: what create()/render() asked the host for, and what Java actually
       handed back. Frame records carry the SLOT index the script saw; the
       bridge rewrites them to the real Java handle before returning. */
    std::vector<int32_t> tex_slot_handles;    /* slot -> GraphicsTexture id */
    std::vector<int32_t> model_slot_handles;  /* quad slot -> ModelJS id    */
    bool resources_ready = false;
    bool resources_dirty = false;
    // Resource slots belong to this module instance and stay stable until
    // dispose. Scripts may allocate or release resources inside render().
    std::vector<jint> tex_w, tex_h;
    std::vector<float> quad_verts, quad_uv;
    std::vector<jint> quad_slot, quad_stage;
};

struct BridgeModule {
    void* dl = nullptr;

    uint32_t (*abi)(void) = nullptr;
    const char* (*type)(void) = nullptr;
    const char* (*id)(void) = nullptr;
    size_t (*state_size)(void) = nullptr;
    /* ABI 6, optional: constructs the per-instance State in the host's block.
       A zero-filled block is NOT a valid object for a State with a non-trivial
       default constructor (std::string / std::vector dereference a null inline
       buffer on libstdc++), so a module that exports this must be asked to
       construct its state before the first create/render. */
    void (*init)(const JcmFrameInput*) = nullptr;
    int32_t (*create)(const JcmFrameInput*) = nullptr;
    int32_t (*render)(const JcmFrameInput*, JcmFrameOutput*) = nullptr;
    int32_t (*dispose)(const JcmFrameInput*) = nullptr;

    std::mutex mutex;
    std::unordered_map<std::string, Instance> instances;

    ~BridgeModule() { close_library(); }

    void close_library() {
        if (dl) {
#if defined(_WIN32)
            FreeLibrary(static_cast<HMODULE>(dl));
#else
            dlclose(dl);
#endif
            dl = nullptr;
        }
    }
};

/* ------------------------------------------------------------------ */
/* Java host hooks (v4/v5)                                             */
/*                                                                     */
/* Model / texture lifetime belongs to the JVM: a GraphicsTexture is a  */
/* NativeImageBackedTexture, a model is an uploaded OptimizedModel.     */
/* The bridge therefore only RECORDS what the script asked for during   */
/* create() and hands the batch to Java, which owns the registries and  */
/* must build them on the render thread.                                */
/*                                                                     */
/* Java side — implemented by com.lx862.jcm.nativeapi.NativeHost */
/* and installed from NativeScriptManager's static initialiser via       */
/* nativeSetHost(host). The bridge binds these resource methods:            */
/*                                                                     */
/*   int[] createResources(int[] widths, int[] heights,                 */
/*                         float[] quadVertices, float[] quadUv,        */
/*                         int[] quadTextureSlot, int[] quadStages)     */
/*                                                                     */
/*       Create N GraphicsTextures (slot i = widths[i] x heights[i])    */
/*       plus the quad models the script described, then return one int */
/*       per texture slot followed by one per quad:                     */
/*         [0 .. texCount)                    = GraphicsTexture handles */
/*         [texCount .. texCount + quadCount) = model handles (-1 none) */
/*       Called when an instance changes resources, on the render thread.         */
/*                                                                     */
/*   void uploadPixels(int textureHandle, int x, int y,                 */
/*                     int w, int h, byte[] rgba)                       */
/*                                                                     */
/*       Blit one dirty rect of an BGRA8 (little-endian ARGB) upload.   */
/*                                                                     */
/* Both are optional: with no host installed the bridge stays silent    */
/* and frame records keep their placeholder handles, which the replay   */
/* step drops — a headless driver still exercises the whole script.     */
/* ------------------------------------------------------------------ */

namespace {

JavaVM* g_vm = nullptr;
jobject g_host = nullptr;                 /* global ref, or null      */
jmethodID g_mid_create_resources = nullptr;
jmethodID g_mid_upload_pixels = nullptr;
jmethodID g_mid_rasterize_text = nullptr;
jmethodID g_mid_measure_font_text = nullptr;
jmethodID g_mid_draw_font_text = nullptr;

/* (still inside the anonymous namespace opened with the bookkeeping structs) */

/** Installed by NativeScriptManager's static initialiser (may be null). */
extern "C" JNIEXPORT void JNICALL
Java_com_lx862_jcm_nativeapi_NativeScriptManager_nativeSetHost(JNIEnv* env, jclass, jobject host) {
    if (g_host) {
        env->DeleteGlobalRef(g_host);
        g_host = nullptr;
    }
    g_mid_create_resources = nullptr;
    g_mid_upload_pixels = nullptr;
    g_mid_rasterize_text = nullptr;
    g_mid_measure_font_text = nullptr;
    g_mid_draw_font_text = nullptr;
    if (!host) return;

    g_host = env->NewGlobalRef(host);
    jclass cls = env->GetObjectClass(host);
    if (!cls) return;
    g_mid_create_resources = env->GetMethodID(cls, "createResources", "([I[I[F[F[I[I)[I");
    g_mid_upload_pixels = env->GetMethodID(cls, "uploadPixels", "(IIIII[B)V");
    g_mid_rasterize_text = env->GetMethodID(cls, "rasterizeText", "([BIIIIIILjava/nio/ByteBuffer;II)I");
    g_mid_measure_font_text = env->GetMethodID(cls, "measureFontText", "([B[BDDD)[D");
    g_mid_draw_font_text = env->GetMethodID(cls, "drawFontText", "([B[BDDDDDDIIIIILjava/nio/ByteBuffer;II)[I");
    if (!g_mid_create_resources || !g_mid_upload_pixels) {
        std::fprintf(stderr, "[jcm_native_bridge] host object is missing "
                             "createResources/uploadPixels — native host disabled\n");
        env->DeleteGlobalRef(g_host);
        g_host = nullptr;
        g_mid_create_resources = nullptr;
        g_mid_upload_pixels = nullptr;
    }
    env->DeleteLocalRef(cls);
}

/* ---- host services handed to the script module (see mtr_native.h) ---- */

int32_t host_acquire_model(void* user, const char* path) {
    /* v5: models come from acquire_quad_model (host-built geometry), so
       this path form only records intent for diagnostics. */
    (void)user;
    (void)path;
    return -1;
}

void host_release_model(void* user, int32_t slot) {
    auto& inst = *static_cast<Instance*>(user);
    if (slot < 0 || slot >= static_cast<int32_t>(inst.quad_slot.size())) return;
    inst.quad_slot[slot] = -1;
    inst.resources_dirty = true;
}

int32_t host_create_texture(void* user, int32_t w, int32_t h) {
    /* Returns a SLOT index; the real GraphicsTexture handle is patched in
       after Java has built the resources (see TextureSlot below). */
    auto& inst = *static_cast<Instance*>(user);
    inst.tex_w.push_back(w);
    inst.tex_h.push_back(h);
    inst.resources_dirty = true;
    return static_cast<int32_t>(inst.tex_w.size()) - 1;
}

void host_release_texture(void* user, int32_t slot) {
    auto& inst = *static_cast<Instance*>(user);
    if (slot < 0 || slot >= static_cast<int32_t>(inst.tex_w.size())) return;
    inst.tex_w[slot] = inst.tex_h[slot] = 0;
    inst.resources_dirty = true;
}

/* Rasterise non-ASCII glyphs through the JVM's bundled Noto font; the host
   caches glyph masks, and writes into the native canvas without copying it. */
int32_t host_rasterize_text(void*, const char* utf8, int32_t len, int32_t x, int32_t y,
                            int32_t max_w, uint8_t r, uint8_t g, uint8_t b, uint8_t* pixels,
                            int32_t out_w, int32_t out_h) {
    if (!g_vm || !g_host || !g_mid_rasterize_text || !pixels || len <= 0
            || out_w <= 0 || out_h <= 0) return -1;
    JNIEnv* env = nullptr;
    if (g_vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_8) != JNI_OK) return -1;
    jbyteArray text = env->NewByteArray(len);
    env->SetByteArrayRegion(text, 0, len, reinterpret_cast<const jbyte*>(utf8));
    jobject target = env->NewDirectByteBuffer(pixels, static_cast<jlong>(out_w) * out_h * 4);
    const jint result = env->CallIntMethod(g_host, g_mid_rasterize_text, text,
                                          x, y, max_w, static_cast<jint>(r),
                                          static_cast<jint>(g), static_cast<jint>(b),
                                          target, out_w, out_h);
    env->DeleteLocalRef(target);
    env->DeleteLocalRef(text);
    if (env->ExceptionCheck()) { env->ExceptionDescribe(); return -1; }
    return result;
}

void host_log(void*, int32_t level, const char* utf8, int32_t len) {
    std::fprintf(stderr, "[native-script/%d] %.*s\n", level, len, utf8);
}

jbyteArray utf8_array(JNIEnv* env, const char* text, int32_t len) {
    jbyteArray result = env->NewByteArray(len);
    if (result && len) env->SetByteArrayRegion(result, 0, len, reinterpret_cast<const jbyte*>(text));
    return result;
}

int32_t host_measure_font_text(void*, const char* font, const char* utf8, int32_t len,
                                double size, double sx, double sy, JcmFontMetrics* out) {
    if (!g_vm || !g_host || !g_mid_measure_font_text || !font || !utf8 || len < 0 || !out) return -1;
    JNIEnv* env = nullptr;
    if (g_vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_8) != JNI_OK) return -1;
    if (env->PushLocalFrame(4) < 0) return -1;
    jbyteArray resource = utf8_array(env, font, static_cast<int32_t>(std::strlen(font)));
    jbyteArray text = utf8_array(env, utf8, len);
    jdoubleArray result = nullptr;
    if (resource && text) result = static_cast<jdoubleArray>(env->CallObjectMethod(
        g_host, g_mid_measure_font_text, resource, text, size, sx, sy));
    int32_t status = -1;
    if (!env->ExceptionCheck() && result && env->GetArrayLength(result) == 4) {
        jdouble values[4];
        env->GetDoubleArrayRegion(result, 0, 4, values);
        *out = {values[0], values[1], values[2], values[3]};
        status = 0;
    }
    if (env->ExceptionCheck()) { env->ExceptionDescribe(); env->ExceptionClear(); }
    env->PopLocalFrame(nullptr);
    return status;
}

int32_t host_draw_font_text(void*, const char* font, const char* utf8, int32_t len,
                             double size, double x, double baseline, double sx, double sy,
                             double compression, uint32_t argb, int32_t clip_x0, int32_t clip_y0,
                             int32_t clip_x1, int32_t clip_y1, uint8_t* pixels,
                             int32_t width, int32_t height, int32_t* bounds) {
    if (!g_vm || !g_host || !g_mid_draw_font_text || !font || !utf8 || len < 0
            || !pixels || width <= 0 || height <= 0 || !bounds) return -1;
    JNIEnv* env = nullptr;
    if (g_vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_8) != JNI_OK) return -1;
    if (env->PushLocalFrame(5) < 0) return -1;
    jbyteArray resource = utf8_array(env, font, static_cast<int32_t>(std::strlen(font)));
    jbyteArray text = utf8_array(env, utf8, len);
    jobject target = env->NewDirectByteBuffer(pixels, static_cast<jlong>(width) * height * 4);
    jintArray result = nullptr;
    if (resource && text && target) result = static_cast<jintArray>(env->CallObjectMethod(
        g_host, g_mid_draw_font_text, resource, text, size, x, baseline, sx, sy, compression,
        static_cast<jint>(argb), clip_x0, clip_y0, clip_x1, clip_y1, target, width, height));
    int32_t status = -1;
    if (!env->ExceptionCheck() && result && env->GetArrayLength(result) == 4) {
        jint values[4];
        env->GetIntArrayRegion(result, 0, 4, values);
        for (int i = 0; i < 4; i++) bounds[i] = static_cast<int32_t>(values[i]);
        status = 0;
    }
    if (env->ExceptionCheck()) { env->ExceptionDescribe(); env->ExceptionClear(); }
    env->PopLocalFrame(nullptr);
    return status;
}

/* v4: host-built textured quad — record the geometry for Java. */
int32_t host_acquire_quad_model(void* user, const float* verts, const float* uv,
                                int32_t vertex_count, int32_t render_stage,
                                int32_t texture_slot) {
    if (!verts || !uv || vertex_count != 4) return -1;
    auto& inst = *static_cast<Instance*>(user);
    inst.quad_verts.insert(inst.quad_verts.end(), verts, verts + 12);
    inst.quad_uv.insert(inst.quad_uv.end(), uv, uv + 8);
    inst.quad_slot.push_back(texture_slot);
    inst.quad_stage.push_back(render_stage);
    inst.resources_dirty = true;
    return static_cast<int32_t>(inst.quad_slot.size()) - 1;
}

void fill_host(JcmHostServices& host, Instance& inst) {
    std::memset(&host, 0, sizeof(host));
    host.user = &inst;
    host.acquire_model = host_acquire_model;
    host.release_model = host_release_model;
    host.create_texture = host_create_texture;
    host.release_texture = host_release_texture;
    host.rasterize_text = host_rasterize_text;
    host.log = host_log;
    host.acquire_quad_model = host_acquire_quad_model;
    host.measure_font_text = host_measure_font_text;
    host.draw_font_text = host_draw_font_text;
}

#if !defined(_WIN32)
void* load_library(const char* path) { return dlopen(path, RTLD_NOW | RTLD_LOCAL); }
void* load_symbol(void* lib, const char* name) { return dlsym(lib, name); }
const char* last_error() { const char* e = dlerror(); return e ? e : "unknown"; }
#else
void* load_library(const char* path) {
    return static_cast<void*>(LoadLibraryA(path));
}
void* load_symbol(void* lib, const char* name) {
    return reinterpret_cast<void*>(GetProcAddress(static_cast<HMODULE>(lib), name));
}
const char* last_error() { return "LoadLibrary/GetProcAddress failed"; }
#endif

/* JNI native method names for the nested class
   com.lx862.jcm.nativeapi.NativeScriptManager$NativeScriptModule.
   "$" becomes "_00024" in the mangled form.
   NOTE: written as ONE fully-expanded token so no nested ## expansion is
   needed (MSVC 19.5x emitted the unexpanded "Java_JNI_CLASS_PATH_nOpen"
   when JNI_CLASS_PATH was pasted in through another macro). */
#define JNI_M(name) \
    Java_com_lx862_jcm_nativeapi_NativeScriptManager_00024NativeScriptModule_##name

/* ------------------------------------------------------------------ */
/* v4/v5: turn the resources create()/render() asked for into real JVM objects  */
/*                                                                     */
/* Runs right after mtrCreate, on the render thread (the caller is the  */
/* same mixin that drives JS scripts, which already requires that).     */
/* ------------------------------------------------------------------ */

bool resolve_captured_resources(JNIEnv* env, Instance& inst) {
    if (!inst.resources_dirty) return true;
    if (inst.tex_w.empty() && inst.quad_verts.empty()) {
        inst.resources_ready = true;
        inst.resources_dirty = false;
        return true;   /* nothing to build (pure-record script) */
    }
    if (!g_host || !g_mid_create_resources) {
        /* No host installed: keep the placeholder handles. Frame records
           then reference slots that Java does not know, and the replay
           step drops them — the script still runs end-to-end. */
        inst.resources_ready = false;
        return true;
    }

    const jsize texCount = static_cast<jsize>(inst.tex_w.size());
    const jsize quadCount = static_cast<jsize>(inst.quad_slot.size());

    jintArray widths = env->NewIntArray(texCount);
    jintArray heights = env->NewIntArray(texCount);
    if (texCount > 0) {
        env->SetIntArrayRegion(widths, 0, texCount, inst.tex_w.data());
        env->SetIntArrayRegion(heights, 0, texCount, inst.tex_h.data());
    }

    jfloatArray verts = env->NewFloatArray(static_cast<jsize>(inst.quad_verts.size()));
    jfloatArray uv = env->NewFloatArray(static_cast<jsize>(inst.quad_uv.size()));
    if (!inst.quad_verts.empty()) {
        env->SetFloatArrayRegion(verts, 0, static_cast<jsize>(inst.quad_verts.size()),
                                 inst.quad_verts.data());
        env->SetFloatArrayRegion(uv, 0, static_cast<jsize>(inst.quad_uv.size()),
                                 inst.quad_uv.data());
    }
    jintArray slots = env->NewIntArray(quadCount);
    jintArray stages = env->NewIntArray(quadCount);
    if (quadCount > 0) {
        env->SetIntArrayRegion(slots, 0, quadCount, inst.quad_slot.data());
        env->SetIntArrayRegion(stages, 0, quadCount, inst.quad_stage.data());
    }

    jintArray result = static_cast<jintArray>(env->CallObjectMethod(
        g_host, g_mid_create_resources, widths, heights, verts, uv, slots, stages));

    bool ok = false;
    if (result != nullptr && !env->ExceptionCheck()
            && env->GetArrayLength(result) == texCount + quadCount) {
        const jsize n = env->GetArrayLength(result);
        std::vector<jint> out(static_cast<size_t>(n));
        env->GetIntArrayRegion(result, 0, n, out.data());
        inst.tex_slot_handles.assign(out.begin(), out.begin() + texCount);
        inst.model_slot_handles.assign(out.begin() + texCount, out.end());
        inst.resources_ready = true;
        inst.resources_dirty = false;
        ok = true;
    } else {
        std::fprintf(stderr, "[jcm_native_bridge] host createResources failed "
                             "(%d textures / %d quads)\n", texCount, quadCount);
        if (env->ExceptionCheck()) env->ExceptionDescribe();
    }

    env->DeleteLocalRef(widths);
    env->DeleteLocalRef(heights);
    env->DeleteLocalRef(verts);
    env->DeleteLocalRef(uv);
    env->DeleteLocalRef(slots);
    env->DeleteLocalRef(stages);
    if (result) env->DeleteLocalRef(result);

    return ok;
}

/** Rewrite upload placeholders (slot index) -> real GraphicsTexture id,
    and drop model draws whose quad has no host model. Mutates the frame
    records in place; the module owns the arena until the next nRender. */
void patch_frame_handles(const Instance& inst, const JcmFrameOutput& out,
                         int32_t* kept_records, int32_t* new_records_len) {
    uint8_t* p = const_cast<uint8_t*>(static_cast<const uint8_t*>(out.records));
    uint8_t* write = p;
    int32_t kept = 0;
    for (int32_t i = 0; i < out.record_count; i++) {
        const JcmRecordHeader* h = reinterpret_cast<const JcmRecordHeader*>(p);
        const int32_t size = h->record_size;
        bool keep = true;

        if (h->kind == JCM_DRAW_TEXTURE_UPLOAD) {
            JcmDrawTextureUpload* u = reinterpret_cast<JcmDrawTextureUpload*>(p);
            const int32_t slot = u->texture_handle;
            if (slot >= 0 && slot < static_cast<int32_t>(inst.tex_slot_handles.size())) {
                u->texture_handle = inst.tex_slot_handles[static_cast<size_t>(slot)];
                if (u->texture_handle < 0) keep = false;
            } else {
                keep = false;   /* unknown slot: nothing to upload into */
            }
        } else if (h->kind == JCM_DRAW_MODEL) {
            JcmDrawModel* m = reinterpret_cast<JcmDrawModel*>(p);
            const int32_t slot = m->model_handle;
            if (slot >= 0 && slot < static_cast<int32_t>(inst.model_slot_handles.size())) {
                m->model_handle = inst.model_slot_handles[static_cast<size_t>(slot)];
                if (m->model_handle < 0) keep = false;   /* host refused the quad */
            } else if (slot >= 0) {
                keep = false;
            }
            /* slot < 0 (script had no host) => keep, so a headless run
               still produces the record for inspection */
        }

        if (keep) {
            if (write != p) std::memmove(write, p, static_cast<size_t>(size));
            write += size;
            kept++;
        }
        p += size;
    }
    *kept_records = kept;
    *new_records_len = static_cast<int32_t>(write - const_cast<uint8_t*>(
        static_cast<const uint8_t*>(out.records)));
}

} /* anonymous namespace */

} /* anonymous namespace */

/* ------------------------------------------------------------------ */
/* lifecycle natives                                                   */
/* ------------------------------------------------------------------ */

extern "C" JNIEXPORT jlong JNICALL
JNI_M(nOpen)(JNIEnv* env, jobject, jstring path) {
    const char* utf = env->GetStringUTFChars(path, nullptr);
    if (!utf) return 0;
    void* lib = load_library(utf);
    if (!lib) {
        std::fprintf(stderr, "[jcm_native_bridge] dlopen failed: %s (%s)\n",
                     utf, last_error());
        env->ReleaseStringUTFChars(path, utf);
        return 0;
    }
    env->ReleaseStringUTFChars(path, utf);

    auto* mod = new (std::nothrow) BridgeModule();
    if (!mod) return 0;
    mod->dl = lib;

    mod->abi        = reinterpret_cast<uint32_t (*)(void)>(load_symbol(lib, "mtrNativeAbiVersion"));
    mod->type       = reinterpret_cast<const char* (*)(void)>(load_symbol(lib, "mtrScriptType"));
    mod->id         = reinterpret_cast<const char* (*)(void)>(load_symbol(lib, "mtrScriptId"));
    mod->state_size = reinterpret_cast<size_t (*)(void)>(load_symbol(lib, "mtrStateSize"));
    /* Optional (ABI 6). Absent on ABI<=5 modules, which relied on a zeroed
       block being usable — fine for their MSVC-tuned layouts. */
    mod->init       = reinterpret_cast<void (*)(const JcmFrameInput*)>(load_symbol(lib, "mtrInit"));
    mod->create     = reinterpret_cast<int32_t (*)(const JcmFrameInput*)>(load_symbol(lib, "mtrCreate"));
    mod->render     = reinterpret_cast<int32_t (*)(const JcmFrameInput*, JcmFrameOutput*)>(load_symbol(lib, "mtrRender"));
    mod->dispose    = reinterpret_cast<int32_t (*)(const JcmFrameInput*)>(load_symbol(lib, "mtrDispose"));

    if (!mod->abi || !mod->type || !mod->id || !mod->state_size ||
        !mod->create || !mod->render || !mod->dispose) {
        std::fprintf(stderr, "[jcm_native_bridge] module misses mtr* exports\n");
        mod->close_library();
        delete mod;
        return 0;
    }
    return reinterpret_cast<jlong>(mod);
}

extern "C" JNIEXPORT jint JNICALL
JNI_M(nAbiVersion)(JNIEnv*, jobject, jlong handle) {
    auto* mod = reinterpret_cast<BridgeModule*>(handle);
    return mod ? static_cast<jint>(mod->abi()) : 0;
}

extern "C" JNIEXPORT jstring JNICALL
JNI_M(nScriptType)(JNIEnv* env, jobject, jlong handle) {
    auto* mod = reinterpret_cast<BridgeModule*>(handle);
    return (mod && mod->type) ? env->NewStringUTF(mod->type()) : nullptr;
}

extern "C" JNIEXPORT jstring JNICALL
JNI_M(nScriptId)(JNIEnv* env, jobject, jlong handle) {
    auto* mod = reinterpret_cast<BridgeModule*>(handle);
    return (mod && mod->id) ? env->NewStringUTF(mod->id()) : nullptr;
}

extern "C" JNIEXPORT jlong JNICALL
JNI_M(nStateSize)(JNIEnv*, jobject, jlong handle) {
    auto* mod = reinterpret_cast<BridgeModule*>(handle);
    return mod ? static_cast<jlong>(mod->state_size()) : 0;
}

/* ------------------------------------------------------------------ */
/* per-frame render                                                    */
/* ------------------------------------------------------------------ */

extern "C" JNIEXPORT jobject JNICALL
JNI_M(nRender)(JNIEnv* env, jobject, jlong handle, jstring instanceKey,
                  jobject snapshotBuf, jint resourceKind) {
    auto* mod = reinterpret_cast<BridgeModule*>(handle);
    if (!mod || !instanceKey || !snapshotBuf) return nullptr;

    const char* key = env->GetStringUTFChars(instanceKey, nullptr);
    if (!key) return nullptr;

    /* one JNI round-trip per frame: everything below runs under the
       module lock, mirroring JCM's per-script single-thread executor */
    std::lock_guard<std::mutex> lock(mod->mutex);

    Instance& inst = mod->instances[key];
    env->ReleaseStringUTFChars(instanceKey, key);

    if (inst.state.empty() && mod->state_size() > 0) {
        inst.state.assign(mod->state_size(), 0);
    }

    /* snapshot: direct ByteBuffer written sequentially by the Java side */
    const void* snap = env->GetDirectBufferAddress(snapshotBuf);
    const jlong snap_len = env->GetDirectBufferCapacity(snapshotBuf);
    if (!snap || snap_len <= 0) return nullptr;
    inst.last_snapshot.assign(static_cast<const uint8_t*>(snap),
                              static_cast<const uint8_t*>(snap) + snap_len);

    JcmHostServices host;
    fill_host(host, inst);

    JcmFrameInput in{};
    in.abi_version = mod->abi();
    in.resource_kind = resourceKind;
    in.snapshot = inst.last_snapshot.data();
    in.state = inst.state.data();
    in.state_size = inst.state.size();
    in.host = &host;

    if (!inst.created) {
        /* ABI 6: let the module construct its State object inside the block
           BEFORE create() runs. The block is raw zeroed memory, which is not a
           valid State when State holds std::string / std::vector — on
           libstdc++ that is an immediate null dereference, so this is not
           optional for modules that export mtrInit. */
        if (mod->init) {
            mod->init(&in);
        }

        if (mod->create(&in) != 0) {
            /* same failure semantics as a JS script that throws in
               create(): drop the instance, let the host cool it down */
            return nullptr;
        }
        inst.created = true;

    }

    JcmFrameOutput out{};
    if (mod->render(&in, &out) != 0) return nullptr;
    // Synchronise AFTER render as well as create: LCDs allocate on their first
    // render, and both ports rebuild their resources when car count changes.
    if (!resolve_captured_resources(env, inst)) return nullptr;

    /* v4/v5: swap the script's slot handles for the real JVM handles and
       drop records that have nothing to draw/upload into. */
    int32_t keptRecords = out.record_count;
    int32_t keptLen = 0;
    patch_frame_handles(inst, out, &keptRecords, &keptLen);
    out.record_count = keptRecords;
    out.records_len = keptLen;

    /* wrap the module-owned arenas as NIO ByteBuffers (zero copy).
       Valid until the next nRender on this module — the replay step
       must complete first (same discipline as the JS pipeline). */
    jclass frameCls = env->FindClass(
        "com/lx862/jcm/nativeapi/NativeScriptManager$NativeFrame");
    if (!frameCls) return nullptr;
    jmethodID ctor = env->GetMethodID(
        frameCls, "<init>",
        "(Ljava/nio/ByteBuffer;ILjava/nio/ByteBuffer;"
        "Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;)V");
    if (!ctor) return nullptr;

    jobject records = env->NewDirectByteBuffer(
        const_cast<void*>(out.records), out.records_len);
    jobject strings = env->NewDirectByteBuffer(
        out.string_arena ? const_cast<char*>(out.string_arena) : nullptr,
        out.string_arena_len);
    jobject pixels = env->NewDirectByteBuffer(
        const_cast<uint8_t*>(out.pixel_arena), out.pixel_arena_len);
    jobject matrices = env->NewDirectByteBuffer(
        out.matrix_arena ? const_cast<float*>(out.matrix_arena) : nullptr,
        out.matrix_arena_len);

    return env->NewObject(frameCls, ctor, records,
                          static_cast<jint>(out.record_count),
                          strings, pixels, matrices);
}

/* ------------------------------------------------------------------ */
/* dispose                                                             */
/* ------------------------------------------------------------------ */

extern "C" JNIEXPORT void JNICALL
JNI_M(nDisposeInstance)(JNIEnv* env, jobject, jlong handle, jstring instanceKey) {
    auto* mod = reinterpret_cast<BridgeModule*>(handle);
    if (!mod || !instanceKey) return;
    const char* key = env->GetStringUTFChars(instanceKey, nullptr);
    if (!key) return;
    std::lock_guard<std::mutex> lock(mod->mutex);
    auto it = mod->instances.find(key);
    env->ReleaseStringUTFChars(instanceKey, key);
    if (it == mod->instances.end()) return;

    if (it->second.created) {
        JcmHostServices host;
        fill_host(host, it->second);
        JcmFrameInput in{};
        in.abi_version = mod->abi();
        in.resource_kind = MTR_RESOURCE_VEHICLE; /* informational for dispose */
        in.snapshot = it->second.last_snapshot.data();
        in.state = it->second.state.data();
        in.state_size = it->second.state.size();
        in.host = &host;
        mod->dispose(&in);
    }
    mod->instances.erase(it);
}

extern "C" JNIEXPORT void JNICALL
JNI_M(nClose)(JNIEnv*, jobject, jlong handle) {
    auto* mod = reinterpret_cast<BridgeModule*>(handle);
    if (!mod) return;
    {
        std::lock_guard<std::mutex> lock(mod->mutex);
        JcmHostServices host;
        for (auto& kv : mod->instances) {
            if (!kv.second.created) continue;
            fill_host(host, kv.second);
            JcmFrameInput in{};
            in.abi_version = mod->abi();
            in.resource_kind = MTR_RESOURCE_VEHICLE;
            in.snapshot = kv.second.last_snapshot.data();
            in.state = kv.second.state.data();
            in.state_size = kv.second.state.size();
            in.host = &host;
            mod->dispose(&in);
        }
    }
    /* dlclose happens in ~BridgeModule */
    delete mod;
}

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    g_vm = vm;
    return JNI_VERSION_1_8;
}
