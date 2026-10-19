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
#include <mtr/shm_channel.h>

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
    /* Snapshot retained ONLY for dispose()-time rebuild. The live buffer is
       passed to mtrRender directly (the Java driver blocks inside the JNI
       call while the module reads it); the copy is refreshed when the driver
       hands over a DIFFERENT buffer (reallocated direct buffer), which for
       vehicles never happens after the first frame. This used to be a
       full 256 KB memcpy per vehicle per frame — pure native churn that
       contributed to the reported periodic stutter. */
    std::vector<uint8_t> last_snapshot;
    const void* snapshot_address = nullptr;
    int64_t snapshot_capacity = 0;
    std::string api_key;
    int32_t resource_kind = MTR_RESOURCE_VEHICLE;
    bool created = false;               /* mtrCreate called?            */
    bool initialized = false;
    bool failed = false;

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

/* One cached arena ByteBuffer (global ref) per module. Created once per
   (address, capacity) and rewound each frame by setting position/limit —
   instead of five fresh NewDirectByteBuffer heap objects per vehicle per
   frame, which was pure young-gen garbage on the render thread. */
struct ArenaView {
    jobject ref = nullptr;
    const void* address = nullptr;
    int64_t capacity = 0;
};

struct BridgeModule {
    void* dl = nullptr;
    /* Shm transport: long instance id -> instance key ("instanceKey/module/<handle>").
       Registered once per instance via nativeShmRegisterInstance, scanned
       off when the string-keyed instance is disposed. */
    std::unordered_map<int64_t, std::string> shm_ids;

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
    /* Cached arena ByteBuffers (records/strings/pixels/matrices/floats).
       The arenas live in the module itself and are stable, so the same five
       wrapper objects serve every frame of every instance. */
    ArenaView views[5];

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

/* ---- shared-memory channel state (see include/mtr/shm_channel.h) ----
   The region is created + mapped by Java (FileChannel#map); this bridge
   only receives the mapping's base address ONCE via nativeShmInit and
   then reads/writes the control/result slots through that pointer — it
   never opens or mmaps the file itself, so there is nothing here to leak
   and no platform-specific mapping code to maintain. All exchanges are
   sequential on the render thread (Java writes request -> empty-argument
   nativeShmKick() -> C++ writes result before returning). */
struct ShmState {
    uint8_t* base = nullptr;    /* mapping base, set by nativeShmInit */
    int64_t size = 0;           /* validated region size               */
};
ShmState g_shm;
/* handle -> BridgeModule registry so the shm kick (which learns the
   module from the CONTROL SLOT, not from a JNI argument) can find it.
   Populated in nOpen, removed in nClose; a tiny mutex because module
   load/unload happens off the render thread. */
std::unordered_map<jlong, BridgeModule*> g_modules;
std::mutex g_modules_mutex;
jclass g_api_class = nullptr;
jmethodID g_api_invoke = nullptr;
jclass g_frame_class = nullptr;
jmethodID g_frame_ctor = nullptr;
jmethodID g_mid_create_resources = nullptr;
jmethodID g_mid_upload_pixels = nullptr;
jmethodID g_mid_rasterize_text = nullptr;
jmethodID g_mid_measure_font_text = nullptr;
jmethodID g_mid_draw_font_text = nullptr;
jmethodID g_mid_acquire_model = nullptr;
jmethodID g_mid_release_model = nullptr;
/* java.nio.ByteBuffer position/limit setters for the cached arena views. */
jclass g_bb_class = nullptr;
jmethodID g_bb_position = nullptr;
jmethodID g_bb_limit = nullptr;

/* (still inside the anonymous namespace opened with the bookkeeping structs) */

namespace {
/** Clears and reports a pending JNI throwable, then returns fallback.
 *
 * A failed FindClass / GetMethodID leaves a throwable pending on the thread.
 * In a JNI entry point that is not called from Java, nothing ever clears it,
 * so every later JNI call on that thread is rejected by the VM and the error
 * only surfaces much later at an unrelated call site. Always consume it here.
 */
template <typename T>
T jni_lookup(JNIEnv* env, const char* what, T value) {
    if (value && !env->ExceptionCheck()) return value;
    if (env->ExceptionCheck()) {
        std::fprintf(stderr, "[jcm_native_bridge] JNI lookup failed: %s\n", what);
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
    return nullptr;
}
}  // namespace

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
    g_mid_acquire_model = nullptr;
    g_mid_release_model = nullptr;
    if (!host) return;

    if (!g_api_class) {
        jclass api = jni_lookup(env, "com/lx862/jcm/nativeapi/NativeApiSession",
                env->FindClass("com/lx862/jcm/nativeapi/NativeApiSession"));
        if (api) {
            g_api_class = static_cast<jclass>(env->NewGlobalRef(api));
            g_api_invoke = jni_lookup(env, "NativeApiSession.invoke([B[B)[B",
                    env->GetStaticMethodID(g_api_class, "invoke", "([B[B)[B"));
            env->DeleteLocalRef(api);
        }
        jclass frame = jni_lookup(env, "com/lx862/jcm/nativeapi/NativeScriptManager$NativeFrame",
                env->FindClass("com/lx862/jcm/nativeapi/NativeScriptManager$NativeFrame"));
        if (frame) {
            g_frame_class = static_cast<jclass>(env->NewGlobalRef(frame));
            g_frame_ctor = jni_lookup(env,
                    "NativeFrame.<init>(ByteBuffer,int,ByteBuffer,ByteBuffer,ByteBuffer,ByteBuffer)V",
                    env->GetMethodID(g_frame_class, "<init>",
                        "(Ljava/nio/ByteBuffer;ILjava/nio/ByteBuffer;Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;Ljava/nio/ByteBuffer;)V"));
            env->DeleteLocalRef(frame);
        }
        jclass bb = jni_lookup(env, "java/nio/ByteBuffer",
                env->FindClass("java/nio/ByteBuffer"));
        if (bb) {
            g_bb_class = static_cast<jclass>(env->NewGlobalRef(bb));
            g_bb_position = jni_lookup(env, "ByteBuffer.position(int)",
                    env->GetMethodID(g_bb_class, "position", "(I)Ljava/nio/ByteBuffer;"));
            g_bb_limit = jni_lookup(env, "ByteBuffer.limit(int)",
                    env->GetMethodID(g_bb_class, "limit", "(I)Ljava/nio/ByteBuffer;"));
            env->DeleteLocalRef(bb);
        }
    }

    g_host = env->NewGlobalRef(host);
    jclass cls = jni_lookup(env, "NativeHost class", env->GetObjectClass(host));
    if (!cls) { g_host = nullptr; return; }
    g_mid_create_resources = jni_lookup(env, "NativeHost.createResources",
            env->GetMethodID(cls, "createResources", "([I[I[F[F[I[I)[I"));
    g_mid_upload_pixels = jni_lookup(env, "NativeHost.uploadPixels",
            env->GetMethodID(cls, "uploadPixels", "(IIIII[B)V"));
    g_mid_rasterize_text = jni_lookup(env, "NativeHost.rasterizeText",
            env->GetMethodID(cls, "rasterizeText", "([BIIIIIILjava/nio/ByteBuffer;II)I"));
    g_mid_measure_font_text = jni_lookup(env, "NativeHost.measureFontText",
            env->GetMethodID(cls, "measureFontText", "([B[BDDD)[D"));
    g_mid_draw_font_text = jni_lookup(env, "NativeHost.drawFontText",
            env->GetMethodID(cls, "drawFontText", "([B[BDDDDDDIIIIILjava/nio/ByteBuffer;II)[I"));
    /* Model acquire/release used to look these up PER CALL and never checked
       the result: a null jmethodID reached CallIntMethod/CallVoidMethod and
       crashed the JVM. Cached here like the other five host methods; a
       missing method now degrades to "no model" instead. */
    g_mid_acquire_model = jni_lookup(env, "NativeHost.acquireModel",
            env->GetMethodID(cls, "acquireModel", "(Ljava/lang/String;)I"));
    g_mid_release_model = jni_lookup(env, "NativeHost.releaseModel",
            env->GetMethodID(cls, "releaseModel", "(I)V"));
    env->DeleteLocalRef(cls);

    /* A bridge built from a different revision than the jar it ships in makes
       these lookups fail. Disable the native host up front and say which piece
       is missing: otherwise nRender still tries to build a NativeFrame and the
       log blames the script's render() at a line that has nothing to do with
       the real problem. */
    bool ok = true;
    if (!g_frame_class || !g_frame_ctor) {
        std::fprintf(stderr, "[jcm_native_bridge] NativeScriptManager$NativeFrame constructor "
                             "not found — this jcm_native_bridge does not match "
                             "NativeScriptManager. Rebuild it from native/jni/jni_bridge.cpp.\n");
        ok = false;
    }
    if (!g_host || !g_mid_create_resources || !g_mid_upload_pixels) {
        std::fprintf(stderr, "[jcm_native_bridge] host object is missing "
                             "createResources/uploadPixels — native host disabled\n");
        ok = false;
    }
    if (!ok) {
        if (g_host) env->DeleteGlobalRef(g_host);
        g_host = nullptr;
        g_mid_create_resources = nullptr;
        g_mid_upload_pixels = nullptr;
        g_mid_rasterize_text = nullptr;
        g_mid_measure_font_text = nullptr;
        g_mid_draw_font_text = nullptr;
        g_mid_acquire_model = nullptr;
        g_mid_release_model = nullptr;
    }
    env->ExceptionClear();
}

/* ---- host services handed to the script module (see mtr_native.h) ---- */

int32_t host_acquire_model(void*, const char* path) {
    JNIEnv* env=nullptr;
    if(!path || !g_vm || !g_host || !g_mid_acquire_model) return -1;
    if(g_vm->GetEnv(reinterpret_cast<void**>(&env),JNI_VERSION_1_8)!=JNI_OK) return -1;
    jstring name=env->NewStringUTF(path);
    if(!name) return -1;
    jint handle=env->CallIntMethod(g_host,g_mid_acquire_model,name);
    env->DeleteLocalRef(name);
    if(env->ExceptionCheck()) {env->ExceptionDescribe();env->ExceptionClear();return -1;}
    return handle<0 ? -1 : handle|0x40000000;
}

void host_release_model(void* user, int32_t slot) {
    if(slot>=0&&(slot&0x40000000)) {
        JNIEnv* env=nullptr;
        if(g_vm&&g_host&&g_mid_release_model&&g_vm->GetEnv(reinterpret_cast<void**>(&env),JNI_VERSION_1_8)==JNI_OK) {
            env->CallVoidMethod(g_host,g_mid_release_model,slot&~0x40000000);
            /* A throwing releaseModel must not leave a pending exception for
               the rest of the frame. */
            if(env->ExceptionCheck()) {env->ExceptionDescribe();env->ExceptionClear();}
        }
        return;
    }
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
    if (env->ExceptionCheck()) { env->ExceptionDescribe(); env->ExceptionClear(); return -1; }
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

const char* host_invoke_api(void* user, const char* request, int32_t length, int32_t* result_length) {
    thread_local std::string result;
    auto& inst = *static_cast<Instance*>(user);
    /* Jar/bridge mismatch guard: a null g_vm or a failed NativeApiSession
       lookup used to reach CallStaticObjectMethod with a null class/method
       and crash the JVM on the first mtr::Api call. */
    if (!g_vm || !g_api_class || !g_api_invoke || !result_length) {
        result = "{\"error\":\"native API bridge is not installed\"}";
        *result_length = static_cast<int32_t>(result.size());
        return result.data();
    }
    JNIEnv* env = nullptr;
    bool attached = false;
    if (g_vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_8) == JNI_EDETACHED) {
        if (g_vm->AttachCurrentThread(reinterpret_cast<void**>(&env), nullptr) != JNI_OK) return nullptr;
        attached = true;
    }
    jbyteArray key = utf8_array(env, inst.api_key.data(), static_cast<int32_t>(inst.api_key.size()));
    jbyteArray message = utf8_array(env, request, length);
    auto reply = static_cast<jbyteArray>(env->CallStaticObjectMethod(g_api_class, g_api_invoke, key, message));
    env->DeleteLocalRef(key); env->DeleteLocalRef(message);
    if (env->ExceptionCheck()) { env->ExceptionDescribe(); env->ExceptionClear(); result = "{\"error\":\"Java native API invocation failed\"}"; }
    else if (reply) {
        result.resize(env->GetArrayLength(reply));
        env->GetByteArrayRegion(reply, 0, static_cast<jsize>(result.size()), reinterpret_cast<jbyte*>(result.data()));
        env->DeleteLocalRef(reply);
    } else result = "{\"error\":\"Java native API returned no result\"}";
    if (attached) g_vm->DetachCurrentThread();
    *result_length = static_cast<int32_t>(result.size());
    return result.data();
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
    host.invoke_api = host_invoke_api;
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
    uint8_t* base = const_cast<uint8_t*>(static_cast<const uint8_t*>(out.records));
    const uint8_t* end = base + (out.records_len > 0 ? out.records_len : 0);
    uint8_t* p = base;
    uint8_t* write = p;
    int32_t kept = 0;
    /* The record stream is module-provided, and this walk runs in BRIDGE code
       (outside the module's MTR_GUARDED_BODY crash guard): a bogus record_size
       used to read/memmove past the records arena and SIGSEGV the whole JVM.
       Validate every step against out.records_len before touching memory,
       exactly like the Java replay (NativeContentDriver.apply) already did. */
    for (int32_t i = 0; i < out.record_count; i++) {
        if (p + sizeof(JcmRecordHeader) > end) break;
        const JcmRecordHeader* h = reinterpret_cast<const JcmRecordHeader*>(p);
        const int32_t size = h->record_size;
        if (size < static_cast<int32_t>(sizeof(JcmRecordHeader)) || p + size > end) break;
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
            if (slot >= 0 && (slot & 0x40000000)) m->model_handle = slot & ~0x40000000;
            else if (slot >= 0 && slot < static_cast<int32_t>(inst.model_slot_handles.size())) {
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

/* Rewind/rebuild one cached arena ByteBuffer to (position 0, limit len).
   Returns the cached global ref, or a fresh local ref when the module did
   not report a capacity (pre-ABI-9 modules) — matching the old per-frame
   behaviour for them. */
jobject arena_view(JNIEnv* env, BridgeModule& mod, int slot,
                   const void* address, int64_t len, int64_t capacity) {
    ArenaView& view = mod.views[slot];
    if (len < 0) len = 0;
    if (capacity <= 0 || !address) {
        /* Unknown capacity: cannot keep a stable view, wrap at the exact
           length like the pre-ABI-9 bridge did. */
        if (view.ref) { env->DeleteGlobalRef(view.ref); view.ref = nullptr; }
        view.address = nullptr;
        view.capacity = 0;
        return address ? env->NewDirectByteBuffer(const_cast<void*>(address), len) : nullptr;
    }
    if (view.ref == nullptr || view.address != address || view.capacity != capacity) {
        if (view.ref) env->DeleteGlobalRef(view.ref);
        jobject local = env->NewDirectByteBuffer(const_cast<void*>(address), capacity);
        view.ref = local ? env->NewGlobalRef(local) : nullptr;
        if (local) env->DeleteLocalRef(local);
        view.address = address;
        view.capacity = capacity;
    }
    if (view.ref == nullptr) return nullptr;
    if (g_bb_position) {
        env->CallObjectMethod(view.ref, g_bb_position, static_cast<jint>(0));
        env->CallObjectMethod(view.ref, g_bb_limit, static_cast<jint>(len));
        if (env->ExceptionCheck()) env->ExceptionClear();
    }
    return view.ref;
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
    /* Register for the shm transport, which learns the module from the
       CONTROL SLOT instead of a JNI argument (see nativeShmKick). */
    {
        std::lock_guard<std::mutex> lk(g_modules_mutex);
        g_modules[reinterpret_cast<jlong>(mod)] = mod;
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
/* per-frame pipeline (shared by the JNI and shm transports)           */
/* ------------------------------------------------------------------ */

/* Runs the per-frame pipeline for (module, instance key, snapshot) while
   the caller holds mod->mutex: lazy instance lookup, ABI-6 state init,
   mtrCreate on first use, mtrRender, host resource resolution and
   record-handle patching.

   On JCM_SHM_OK, `out` holds the module-owned, PATCHED arena descriptors
   (record_count / records_len adjusted) and stays valid until the next
   render on this module. Any failure marks the instance failed exactly
   like the JNI-only bridge did, so both transports share one behaviour. */
int32_t run_frame_locked(JNIEnv* env, BridgeModule& mod, const char* key,
                         const void* snapshot, int64_t snapshotLen,
                         int64_t snapshotCapacity, jint kind, JcmFrameOutput& out) {
    Instance& inst = mod.instances[key];
    inst.api_key = key;
    inst.resource_kind = kind;
    if (inst.failed) return JCM_SHM_ERR_CREATE;

    if (inst.state.empty() && mod.state_size() > 0) {
        inst.state.assign(mod.state_size(), 0);
    }

    /* Snapshot retention for dispose()-time rebuild: refreshed only when the
       driver hands over a different buffer (see the note in nRender). The
       live buffer is read in place — zero copies on both transports. */
    if (inst.snapshot_address != snapshot || inst.snapshot_capacity != snapshotCapacity) {
        inst.last_snapshot.assign(static_cast<const uint8_t*>(snapshot),
                                  static_cast<const uint8_t*>(snapshot) + snapshotLen);
        inst.snapshot_address = snapshot;
        inst.snapshot_capacity = snapshotCapacity;
    }

    JcmHostServices host;
    fill_host(host, inst);

    JcmFrameInput in{};
    in.abi_version = mod.abi();
    in.resource_kind = kind;
    in.snapshot = snapshot;
    in.state = inst.state.data();
    in.state_size = inst.state.size();
    in.host = &host;

    if (!inst.created) {
        /* ABI 6: let the module construct its State object inside the block
           BEFORE create() runs. The block is raw zeroed memory, which is not
           a valid State when State holds std::string / std::vector — on
           libstdc++ that is an immediate null dereference, so this is not
           optional for modules that export mtrInit. */
        if (mod.init) {
            mod.init(&in);
        }
        inst.initialized = true;

        if (mod.create(&in) != 0) {
            /* same failure semantics as a JS script that throws in
               create(): drop the instance, let the host cool it down */
            inst.failed = true;
            return JCM_SHM_ERR_CREATE;
        }
        inst.created = true;
    }

    if (mod.render(&in, &out) != 0) { inst.failed = true; return JCM_SHM_ERR_RENDER; }
    // Scripts may allocate or rebuild resources during render().
    if (!resolve_captured_resources(env, inst)) return JCM_SHM_ERR_RESOURCES;

    /* v4/v5: swap the script's slot handles for the real JVM handles and
       drop records that have nothing to draw/upload into. */
    int32_t keptRecords = out.record_count;
    int32_t keptLen = 0;
    patch_frame_handles(inst, out, &keptRecords, &keptLen);
    out.record_count = keptRecords;
    out.records_len = keptLen;
    return JCM_SHM_OK;
}

/* ------------------------------------------------------------------ */
/* per-frame render (JNI transport: the data rides the JNI call itself) */
/* ------------------------------------------------------------------ */

extern "C" JNIEXPORT jobject JNICALL
JNI_M(nRender)(JNIEnv* env, jobject, jlong handle, jstring instanceKey,
                  jobject snapshotBuf, jint snapshotLen, jint resourceKind) {
    auto* mod = reinterpret_cast<BridgeModule*>(handle);
    if (!mod || !instanceKey || !snapshotBuf) return nullptr;

    const char* key = env->GetStringUTFChars(instanceKey, nullptr);
    if (!key) return nullptr;

    /* one JNI round-trip per frame: everything below runs under the
       module lock, mirroring JCM's per-script single-thread executor */
    std::lock_guard<std::mutex> lock(mod->mutex);

    /* snapshot: direct ByteBuffer written sequentially by the Java side.
       snapshotLen is the WRITTEN length (buffer position 0 / limit = len);
       the module reads the live buffer while the Java driver is blocked
       inside this call. */
    const void* snap = env->GetDirectBufferAddress(snapshotBuf);
    const jlong snap_capacity = env->GetDirectBufferCapacity(snapshotBuf);
    if (!snap || snapshotLen <= 0) {
        env->ReleaseStringUTFChars(instanceKey, key);
        return nullptr;
    }

    JcmFrameOutput out{};
    const int32_t status = run_frame_locked(env, *mod, key, snap, snapshotLen,
                                            snap_capacity, resourceKind, out);
    env->ReleaseStringUTFChars(instanceKey, key);
    if (status != 0) return nullptr;

    /* wrap the module-owned arenas as NIO ByteBuffers. ABI 9+ modules report
       arena capacities, so the five wrapper objects are cached per module and
       merely rewound (position 0 / limit = this frame's length); pre-ABI-9
       modules keep the per-frame wrapping. Valid until the next nRender on
       this module — the replay step must complete first (same discipline as
       the JS pipeline). */
    if (!g_frame_class || !g_frame_ctor) return nullptr;

    jobject records = arena_view(env, *mod, 0, out.records, out.records_len,
                                 out.records_capacity);
    jobject strings = arena_view(env, *mod, 1, out.string_arena, out.string_arena_len,
                                 out.string_arena_capacity);
    jobject pixels = arena_view(env, *mod, 2, out.pixel_arena, out.pixel_arena_len,
                                out.pixel_arena_capacity);
    jobject matrices = arena_view(env, *mod, 3, out.matrix_arena, out.matrix_arena_len,
                                  out.matrix_arena_capacity);

    jobject floats = arena_view(env, *mod, 4, out.float_arena, out.float_arena_len,
                                 out.float_arena_capacity);
    jobject result = env->NewObject(g_frame_class, g_frame_ctor, records,
                          static_cast<jint>(out.record_count),
                          strings, pixels, matrices, floats);
    /* The cached views are global refs owned by the module; locals from the
       pre-ABI-9 path are released below (deleting a global ref's LOCAL
       reference here would be wrong, so only delete when it is local —
       arena_view returns either a global ref (owned, do not delete) or a
       fresh local ref). Distinguish by checking the cache. */
    if (records && records != mod->views[0].ref) env->DeleteLocalRef(records);
    if (strings && strings != mod->views[1].ref) env->DeleteLocalRef(strings);
    if (pixels && pixels != mod->views[2].ref) env->DeleteLocalRef(pixels);
    if (matrices && matrices != mod->views[3].ref) env->DeleteLocalRef(matrices);
    if (floats && floats != mod->views[4].ref) env->DeleteLocalRef(floats);
    return result;
}

/* ------------------------------------------------------------------ */
/* shared-memory channel natives (NativeChannel.java)                  */
/*                                                                     */
/* The per-frame exchange lives in a file-backed region mapped by Java */
/* (MappedByteBuffer). Java writes the request into the CONTROL slot   */
/* and makes an EMPTY-ARGUMENT nativeShmKick() call; this handler reads */
/* the request (the snapshot ADDRESS travels per frame and points at    */
/* the shared mapping for driver-owned buffers), runs the same pipeline */
/* as nRender, then writes the result descriptors (arena addresses,     */
/* lengths and capacities — the arenas are module-owned and stable)     */
/* into the RESULT slot. No jstring, no NewDirectByteBuffer, no         */
/* NativeFrame construction on this path.                               */
/* ------------------------------------------------------------------ */

extern "C" JNIEXPORT jint JNICALL
Java_com_lx862_jcm_nativeapi_NativeChannel_nativeShmInit(JNIEnv*, jclass, jlong base, jlong size) {
    if (base == 0 || size < JCM_SHM_WINDOWS_OFFSET + 4096) {
        g_shm.base = nullptr;
        g_shm.size = 0;
        return JCM_SHM_ERR_BAD_LAYOUT;
    }
    const uint8_t* p = reinterpret_cast<const uint8_t*>(base);
    uint64_t magic = 0;
    uint32_t version = 0;
    uint64_t regionSize = 0;
    std::memcpy(&magic, p + JCM_SHM_HDR_MAGIC, sizeof(magic));
    std::memcpy(&version, p + JCM_SHM_HDR_VERSION, sizeof(version));
    std::memcpy(&regionSize, p + JCM_SHM_HDR_SIZE, sizeof(regionSize));
    if (magic != JCM_SHM_MAGIC || version != JCM_SHM_LAYOUT_VERSION
            || static_cast<int64_t>(regionSize) != size) {
        g_shm.base = nullptr;
        g_shm.size = 0;
        return JCM_SHM_ERR_BAD_LAYOUT;
    }
    g_shm.base = reinterpret_cast<uint8_t*>(base);
    g_shm.size = size;
    return JCM_SHM_OK;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_lx862_jcm_nativeapi_NativeChannel_nativeShmKick(JNIEnv* env, jclass) {
    if (!g_shm.base) return JCM_SHM_ERR_NOT_INIT;
    const uint8_t* ctl = g_shm.base + JCM_SHM_CONTROL_OFFSET;
    uint64_t seq = 0, handle = 0, instanceId = 0, snapAddr = 0;
    uint32_t kind = 0, snapLen = 0;
    std::memcpy(&seq, ctl + JCM_SHM_CTL_SEQ, sizeof(seq));
    std::memcpy(&handle, ctl + JCM_SHM_CTL_MODULE, sizeof(handle));
    std::memcpy(&instanceId, ctl + JCM_SHM_CTL_INSTANCE, sizeof(instanceId));
    std::memcpy(&kind, ctl + JCM_SHM_CTL_KIND, sizeof(kind));
    std::memcpy(&snapLen, ctl + JCM_SHM_CTL_SNAP_LEN, sizeof(snapLen));
    std::memcpy(&snapAddr, ctl + JCM_SHM_CTL_SNAP_ADDR, sizeof(snapAddr));
    if (seq == 0 || kind > MTR_RESOURCE_LIFT) return JCM_SHM_ERR_REQUEST;
    if (!snapAddr || snapLen == 0) return JCM_SHM_ERR_SNAPSHOT;

    BridgeModule* mod = nullptr;
    {
        std::lock_guard<std::mutex> lk(g_modules_mutex);
        auto it = g_modules.find(static_cast<jlong>(handle));
        if (it != g_modules.end()) mod = it->second;
    }
    if (!mod) return JCM_SHM_ERR_MODULE;

    uint8_t* res = g_shm.base + JCM_SHM_RESULT_OFFSET;
    auto writeResult = [&](uint64_t echo, int32_t status) {
        std::memcpy(res + JCM_SHM_RES_SEQ, &echo, sizeof(echo));
        std::memcpy(res + JCM_SHM_RES_STATUS, &status, sizeof(status));
    };

    std::lock_guard<std::mutex> lock(mod->mutex);
    const auto idIt = mod->shm_ids.find(static_cast<int64_t>(instanceId));
    if (idIt == mod->shm_ids.end()) return JCM_SHM_ERR_INSTANCE;

    JcmFrameOutput out{};
    const int32_t status = run_frame_locked(env, *mod, idIt->second.c_str(),
            reinterpret_cast<const void*>(static_cast<uintptr_t>(snapAddr)),
            static_cast<int64_t>(snapLen),
            static_cast<int64_t>(snapLen),
            static_cast<jint>(kind), out);
    if (status != 0) {
        writeResult(seq, status);
        return status;
    }

    /* Publish the module-owned arena descriptors; Java wraps them into
       REUSED NIO ByteBuffers (NativeArenaView) — zero JNI objects created. */
    struct ArenaDesc { uint64_t addr, len, cap; };
    const ArenaDesc arenas[5] = {
        { reinterpret_cast<uintptr_t>(out.records),      static_cast<uint64_t>(out.records_len),      static_cast<uint64_t>(out.records_capacity) },
        { reinterpret_cast<uintptr_t>(out.string_arena), static_cast<uint64_t>(out.string_arena_len), static_cast<uint64_t>(out.string_arena_capacity) },
        { reinterpret_cast<uintptr_t>(out.pixel_arena),  static_cast<uint64_t>(out.pixel_arena_len),  static_cast<uint64_t>(out.pixel_arena_capacity) },
        { reinterpret_cast<uintptr_t>(out.matrix_arena), static_cast<uint64_t>(out.matrix_arena_len), static_cast<uint64_t>(out.matrix_arena_capacity) },
        { reinterpret_cast<uintptr_t>(out.float_arena),  static_cast<uint64_t>(out.float_arena_len),  static_cast<uint64_t>(out.float_arena_capacity) },
    };
    static const int arenaOffsets[5] = {
        JCM_SHM_RES_RECORDS, JCM_SHM_RES_STRINGS, JCM_SHM_RES_PIXELS,
        JCM_SHM_RES_MATRICES, JCM_SHM_RES_FLOATS
    };
    int32_t count = out.record_count;
    std::memcpy(res + JCM_SHM_RES_COUNT, &count, sizeof(count));
    for (int i = 0; i < 5; i++) {
        std::memcpy(res + arenaOffsets[i], &arenas[i], sizeof(ArenaDesc));
    }
    /* seq echo LAST: a stale reader can never mistake a half-written
       result for this frame's (same-thread discipline makes this a
       belt-and-suspenders guard, not a correctness requirement). */
    writeResult(seq, JCM_SHM_OK);
    return JCM_SHM_OK;
}

extern "C" JNIEXPORT void JNICALL
Java_com_lx862_jcm_nativeapi_NativeChannel_nativeShmRegisterInstance(JNIEnv* env, jclass,
        jlong handle, jlong instanceId, jstring instanceKey) {
    BridgeModule* mod = nullptr;
    {
        std::lock_guard<std::mutex> lk(g_modules_mutex);
        auto it = g_modules.find(handle);
        if (it != g_modules.end()) mod = it->second;
    }
    if (!mod || !instanceKey) return;
    const char* utf = env->GetStringUTFChars(instanceKey, nullptr);
    if (!utf) return;
    {
        std::lock_guard<std::mutex> lock(mod->mutex);
        mod->shm_ids[static_cast<int64_t>(instanceId)] = utf;
    }
    env->ReleaseStringUTFChars(instanceKey, utf);
}

extern "C" JNIEXPORT void JNICALL
Java_com_lx862_jcm_nativeapi_NativeChannel_nativeShmShutdown(JNIEnv*, jclass) {
    /* The mapping itself is owned and unmapped by Java (NativeShm.close);
       the bridge only forgets the base address here. */
    g_shm.base = nullptr;
    g_shm.size = 0;
}

/* ------------------------------------------------------------------ */
/* dispose                                                             */
/* ------------------------------------------------------------------ */

extern "C" JNIEXPORT void JNICALL
JNI_M(nDisposeInstance)(JNIEnv* env, jobject, jlong handle, jstring instanceKey,
                        jobject snapshotBuf, jint snapshotLen) {
    auto* mod = reinterpret_cast<BridgeModule*>(handle);
    if (!mod || !instanceKey) return;
    const char* key = env->GetStringUTFChars(instanceKey, nullptr);
    if (!key) return;
    std::lock_guard<std::mutex> lock(mod->mutex);
    auto it = mod->instances.find(key);
    env->ReleaseStringUTFChars(instanceKey, key);
    if (it == mod->instances.end()) return;

    /* The driver hands over the LIVE snapshot buffer it rendered this
       instance with (Java still owns it — dispose runs between frames).
       The old retained copy stayed at the instance's FIRST frame forever,
       because the reusable buffers never changed address, so any dispose()
       that read car count / ids / speed saw garbage. Falls back to the
       retained copy only when no buffer is passed. */
    const void* live = nullptr;
    if (snapshotBuf) {
        live = env->GetDirectBufferAddress(snapshotBuf);
        if (snapshotLen < 0) live = nullptr;
    }

    if (it->second.initialized) {
        JcmHostServices host;
        fill_host(host, it->second);
        JcmFrameInput in{};
        in.abi_version = mod->abi();
        in.resource_kind = it->second.resource_kind;
        if (live) {
            in.snapshot = live;
        } else {
            in.snapshot = it->second.last_snapshot.data();
        }
        in.state = it->second.state.data();
        in.state_size = it->second.state.size();
        in.host = &host;
        mod->dispose(&in);
    }
    /* Capture the key BEFORE the instance map entry dies: the shm id scan
       below compares against it (erase invalidates the iterator). */
    const std::string disposed_api_key = it->second.api_key;
    mod->instances.erase(it);
    /* The shm transport keeps a long-id -> key entry for this instance;
       forget it with the instance so ids never outlive their instances. */
    for (auto sit = mod->shm_ids.begin(); sit != mod->shm_ids.end();) {
        if (sit->second == disposed_api_key) {
            sit = mod->shm_ids.erase(sit);
        } else {
            ++sit;
        }
    }
}

extern "C" JNIEXPORT void JNICALL
JNI_M(nClose)(JNIEnv* env, jobject, jlong handle) {
    auto* mod = reinterpret_cast<BridgeModule*>(handle);
    if (!mod) return;
    {
        std::lock_guard<std::mutex> lock(mod->mutex);
        JcmHostServices host;
        for (auto& kv : mod->instances) {
            if (!kv.second.initialized) continue;
            fill_host(host, kv.second);
            JcmFrameInput in{};
            in.abi_version = mod->abi();
            in.resource_kind = kv.second.resource_kind;
            in.snapshot = kv.second.last_snapshot.data();
            in.state = kv.second.state.data();
            in.state_size = kv.second.state.size();
            in.host = &host;
            mod->dispose(&in);
        }
        /* Release the cached arena ByteBuffers (global refs owned by this
           module) before the module memory goes away. */
        for (ArenaView& view : mod->views) {
            if (view.ref) env->DeleteGlobalRef(view.ref);
            view.ref = nullptr;
            view.address = nullptr;
            view.capacity = 0;
        }
        /* Drop the shm instance-id table with the module it belongs to. */
        mod->shm_ids.clear();
    }
    {
        std::lock_guard<std::mutex> lk(g_modules_mutex);
        g_modules.erase(handle);
    }
    /* dlclose happens in ~BridgeModule */
    delete mod;
}

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    g_vm = vm;
    return JNI_VERSION_1_8;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_lx862_jcm_nativeapi_NativeScriptManager_nativeApiCallback(JNIEnv* env, jclass, jlong function, jlong user, jbyteArray parameters) {
    using Callback = const char* (*)(void*, const char*, int32_t, int32_t*);
    if (!function || !parameters) return nullptr;
    std::string bytes(static_cast<size_t>(env->GetArrayLength(parameters)), '\0');
    env->GetByteArrayRegion(parameters, 0, static_cast<jsize>(bytes.size()), reinterpret_cast<jbyte*>(bytes.data()));
    int32_t length = 0;
    const char* result = reinterpret_cast<Callback>(static_cast<uintptr_t>(function))(
        reinterpret_cast<void*>(static_cast<uintptr_t>(user)), bytes.data(), static_cast<int32_t>(bytes.size()), &length);
    return utf8_array(env, result, length);
}
