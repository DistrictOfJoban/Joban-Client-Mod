/**
 * native_ffm_test.cpp — headless regression for the bridge's JNI-FREE
 * plane (the jcm_* plain-C exports + the JcmFfmHostVTable upcalls).
 *
 * The bridge translation unit is linked INTO this executable, but the JVM
 * is not: nothing in the bridge ever touches a JVM (JDK-free since
 * layout v2), so without the vtable every host-boundary path is inert.
 * The fake vtable installed via jcm_ffm_init stands in for
 * the java.lang.foreign upcall stubs PanamaFfmChannel.java would create —
 * which makes this the exact C contract the real Java side relies on:
 *
 *   1. jcm_ffm_init validation (truncated table / unknown abi rejected,
 *      null atomically restores the legacy plane);
 *   2. module lifecycle purely over downcalls: jcm_module_open, abi /
 *      script_type / script_id / state_size, jcm_module_render,
 *      jcm_module_dispose_instance, jcm_module_close;
 *   3. the shared-memory slot protocol end-to-end: jcm_shm_init,
 *      jcm_shm_register_instance, jcm_shm_kick and the result slot;
 *   4. host_invoke_api served through the vtable (a module's mtr::Api
 *      round-trips into fake-JSON replies);
 *   5. deferred reclamation drains off-thread (jcm_reclaimer_quiesce).
 *
 * Every platform CI runs this: it is the only test that proves the plain-C
 * surface compiles and behaves with NO JVM anywhere (Windows and macOS
 * bridges never see the JVM-harness paths otherwise).
 */
#include <mtr/ffm_host.h>
#include <mtr/shm_channel.h>

#include <cstdio>
#include <cstring>
#include <string>
#include <vector>

/* Declared by bridge.cpp (linked into this executable). */
extern "C" {
int32_t jcm_ffm_init(const JcmFfmHostVTable*, int32_t);
int64_t jcm_module_open(const char*, int32_t);
void    jcm_module_close(int64_t);
int32_t jcm_module_abi(int64_t);
int32_t jcm_module_script_type(int64_t, char*, int32_t);
int32_t jcm_module_script_id(int64_t, char*, int32_t);
int64_t jcm_module_state_size(int64_t);
int32_t jcm_module_render(int64_t, const char*, int32_t, int64_t, int64_t,
                          int32_t, uint8_t*);
void    jcm_module_dispose_instance(int64_t, const char*, int32_t, int64_t, int64_t);
int32_t jcm_shm_init(int64_t, int64_t);
int32_t jcm_shm_kick(void);
void    jcm_shm_register_instance(int64_t, int64_t, const char*, int32_t);
void    jcm_shm_shutdown(void);
int64_t jcm_reclaimer_pending(void);
void    jcm_reclaimer_quiesce(void);
}

static int g_failures = 0;
#define CHECK(cond, msg)                                                  \
    do {                                                                  \
        if (cond) {                                                       \
            std::printf("FFM REGRESSION OK: %s\n", msg);                  \
        } else {                                                          \
            std::fprintf(stderr, "FFM REGRESSION FAIL: %s (line %d)\n",   \
                         msg, __LINE__);                                  \
            ++g_failures;                                                 \
        }                                                                 \
    } while (0)

/* ---------------------------------------------------------------------- */
/* fake upcall vtable                                                     */
/* ---------------------------------------------------------------------- */

static int g_invoke_calls = 0;

/* Minimal canned Java: a handle object for root/new requests, the magic
   int64 for the largeId probe call, null for everything else. Substring
   matching is deliberate — this is a stand-in, not a JSON parser. */
static int64_t fake_invoke_api(const char* key, int32_t key_len,
                               const char* request, int32_t request_len,
                               int32_t* out_len) {
    (void)key; (void)key_len;
    ++g_invoke_calls;
    static thread_local std::string reply;
    const std::string req(request, static_cast<size_t>(request_len));
    if (req.find("\"release\"") != std::string::npos) {
        reply = "{\"value\":null}";
    } else if (req.find("\"largeId\"") != std::string::npos) {
        reply = "{\"value\":9223372036854775000}";
    } else if (req.find("\"op\":\"root\"") != std::string::npos
            || req.find("\"op\":\"new\"") != std::string::npos) {
        reply = "{\"value\":{\"$handle\":\"c0\",\"$type\":\"java.lang.Object\"}}";
    } else {
        reply = "{\"value\":null}";
    }
    *out_len = static_cast<int32_t>(reply.size());
    return reinterpret_cast<int64_t>(reply.data());
}

static int64_t fake_acquire_model(const char* path, int32_t len) {
    (void)path; (void)len;
    return 1234;
}

static int g_release_calls = 0;
static void fake_release_model(int64_t handle) {
    (void)handle;
    ++g_release_calls;
}

static int g_rasterize_calls = 0;
static int32_t fake_rasterize_text(const char* utf8, int32_t len, int32_t x, int32_t y,
                                   int32_t max_w, int32_t r, int32_t g, int32_t b,
                                   uint8_t* pixels, int32_t out_w, int32_t out_h) {
    (void)utf8; (void)len; (void)x; (void)y; (void)max_w; (void)r; (void)g; (void)b;
    ++g_rasterize_calls;
    if (!pixels || out_w <= 0 || out_h <= 0) return -1;
    std::memset(pixels, 0xff, static_cast<size_t>(out_w) * out_h * 4);
    return 1;
}

static int32_t fake_measure_font_text(const char* font, int32_t font_len,
                                      const char* utf8, int32_t len,
                                      double size, double sx, double sy,
                                      double* out_metrics) {
    (void)font; (void)font_len; (void)utf8; (void)len; (void)size; (void)sx; (void)sy;
    if (!out_metrics) return -1;
    out_metrics[0] = 10.0; out_metrics[1] = 8.0; out_metrics[2] = 2.0; out_metrics[3] = 12.0;
    return 0;
}

static int32_t fake_draw_font_text(const char* font, int32_t font_len,
                                   const char* utf8, int32_t len,
                                   double size, double x, double baseline, double sx, double sy,
                                   double compression, uint32_t argb,
                                   int32_t clip_x0, int32_t clip_y0, int32_t clip_x1, int32_t clip_y1,
                                   uint8_t* pixels, int32_t width, int32_t height,
                                   int32_t* out_bounds) {
    (void)font; (void)font_len; (void)utf8; (void)len; (void)size; (void)x; (void)baseline;
    (void)sx; (void)sy; (void)compression; (void)argb;
    (void)clip_x0; (void)clip_y0; (void)clip_x1; (void)clip_y1;
    if (!pixels || width <= 0 || height <= 0 || !out_bounds) return -1;
    std::memset(pixels, 0x7f, static_cast<size_t>(width) * height * 4);
    out_bounds[0] = 0; out_bounds[1] = 0;
    out_bounds[2] = width; out_bounds[3] = height;
    return 0;
}

static int g_resource_calls = 0;
static int32_t fake_create_resources(int32_t tex_count, int32_t quad_count,
                                     const int32_t* widths, const int32_t* heights,
                                     const float* verts, const float* uv,
                                     const int32_t* slots, const int32_t* stages,
                                     int32_t* out_handles) {
    ++g_resource_calls;
    if (widths) CHECK(widths[0] > 0, "create_resources sees honest texture width");
    if (verts)  CHECK(verts[0] == 0.0f, "create_resources sees float geometry");
    if (slots)  CHECK(slots[0] >= 0, "create_resources sees texture slot");
    if (stages) CHECK(stages[0] >= 0, "create_resources sees stage");
    (void)heights; (void)uv;
    for (int32_t i = 0; i < tex_count + quad_count; i++) out_handles[i] = 1000 + i;
    return 0;
}

/* ---------------------------------------------------------------------- */

int main(int argc, char** argv) {
    /* Probe paths arrive as command-line arguments (the CMake test
       registration passes $<TARGET_FILE:...>) so Windows backslash paths
       never round-trip through a C string-escape in a -D flag. */
    if (argc < 3) {
        std::fprintf(stderr, "FFM REGRESSION FAIL: usage: native_ffm_test <legacy_probe> <content_probe>\n");
        return 2;
    }
    const char* legacyPath = argv[1];
    const char* contentPath = argv[2];

    /* ---- 1. vtable install validation -------------------------------- */
    {
        JcmFfmHostVTable table{};
        table.abi = JCM_FFM_HOST_ABI;
        CHECK(jcm_ffm_init(&table, 8) == JCM_SHM_ERR_BAD_LAYOUT,
              "truncated vtable rejected (size guard)");
        table.abi = 99;
        CHECK(jcm_ffm_init(&table, sizeof(table)) == JCM_SHM_ERR_BAD_LAYOUT,
              "unknown vtable abi rejected");
        CHECK(jcm_ffm_init(nullptr, 0) == 0, "null vtable atomically clears");
    }

    JcmFfmHostVTable table{};
    table.abi = JCM_FFM_HOST_ABI;
    table.acquire_model = fake_acquire_model;
    table.release_model = fake_release_model;
    table.rasterize_text = fake_rasterize_text;
    table.measure_font_text = fake_measure_font_text;
    table.draw_font_text = fake_draw_font_text;
    table.create_resources = fake_create_resources;
    table.invoke_api = fake_invoke_api;
    CHECK(jcm_ffm_init(&table, sizeof(table)) == 0, "vtable installed");

    /* ---- 2. module lifecycle over downcalls (ABI 6 legacy probe) ----- */
    const int64_t legacy = jcm_module_open(legacyPath,
            static_cast<int32_t>(std::strlen(legacyPath)));
    CHECK(legacy != 0, "jcm_module_open returns a handle (no System.load)");
    CHECK(jcm_module_abi(legacy) == 6, "abi readable via downcall");
    char id[64];
    const int32_t idLen = jcm_module_script_id(legacy, id, sizeof(id));
    CHECK(idLen == 11 && std::strcmp(id, "test:legacy") == 0,
          "script id readable via downcall");
    char type[64];
    const int32_t typeLen = jcm_module_script_type(legacy, type, sizeof(type));
    CHECK(typeLen > 0, "script type readable via downcall");
    CHECK(jcm_module_state_size(legacy) == 4, "state size readable via downcall");

    static uint8_t snapshot[256];   /* zeroed = valid "empty" ABI blob */
    uint8_t desc[128];
    std::memset(desc, 0, sizeof(desc));
    int32_t status = jcm_module_render(legacy, "k", 1,
            reinterpret_cast<int64_t>(snapshot), 256, 0, desc);
    CHECK(status == JCM_SHM_OK && 0 == *reinterpret_cast<int32_t*>(desc + 4),
          "first direct render creates + renders (ABI 6, zeroed snapshot)");
    status = jcm_module_render(legacy, "k", 1,
            reinterpret_cast<int64_t>(snapshot), 256, 0, desc);
    CHECK(status == JCM_SHM_OK, "second direct render reuses instance state");
    jcm_module_dispose_instance(legacy, "k", 1, 0, -1);
    jcm_reclaimer_quiesce();
    CHECK(jcm_reclaimer_pending() == 0, "instance memory reclaimed off-thread");
    jcm_module_close(legacy);
    jcm_reclaimer_quiesce();
    CHECK(jcm_reclaimer_pending() == 0, "module memory reclaimed off-thread");
    CHECK(jcm_module_open("/nonexistent/probe", 19) == 0,
          "failed open returns a null handle (never a dangling one)");

    /* ---- 3. api upcalls through the vtable (content probe) ---------- */
    const int64_t content = jcm_module_open(contentPath,
            static_cast<int32_t>(std::strlen(contentPath)));
    CHECK(content != 0 && jcm_module_abi(content) == 11, "content probe opened");
    const int invokesBefore = g_invoke_calls;
    status = jcm_module_render(content, "inst", 4,
            reinterpret_cast<int64_t>(snapshot), 256, 1, desc);
    /* create() succeeds (its only api calls round-trip through the fake
       vtable); render() throws on the zeroed PIDS snapshot — status 6. */
    CHECK(status == JCM_SHM_ERR_RENDER,
          "content create succeeds via vtable api, render fails on empty snapshot");
    CHECK(g_invoke_calls >= invokesBefore + 2,
          "module api calls were served by the vtable upcall");
    jcm_module_dispose_instance(content, "inst", 4, 0, -1);
    CHECK(g_invoke_calls > invokesBefore + 2, "dispose api round-trips too");
    jcm_module_close(content);

    /* ---- 4. shm slot protocol over downcalls ------------------------- */
    {
        const int64_t regionSize = 1 << 20;
        std::vector<uint8_t> region(static_cast<size_t>(regionSize), 0);
        uint8_t* base = region.data();
        const uint64_t magic = JCM_SHM_MAGIC;
        const uint32_t version = JCM_SHM_LAYOUT_VERSION;
        std::memcpy(base + JCM_SHM_HDR_MAGIC, &magic, sizeof(magic));
        std::memcpy(base + JCM_SHM_HDR_VERSION, &version, sizeof(version));
        std::memcpy(base + JCM_SHM_HDR_SIZE, &regionSize, sizeof(regionSize));
        CHECK(jcm_shm_init(reinterpret_cast<int64_t>(base), regionSize) == JCM_SHM_OK,
              "shm region accepted via downcall");

        const int64_t mod = jcm_module_open(legacyPath,
                static_cast<int32_t>(std::strlen(legacyPath)));
        CHECK(mod != 0, "module reopened for the shm leg");
        jcm_shm_register_instance(mod, 7, "shm-inst", 8);

        uint8_t* ctl = base + JCM_SHM_CONTROL_OFFSET;
        uint8_t* res = base + JCM_SHM_RESULT_OFFSET;
        const uint64_t seq = 1;
        const uint64_t handle = static_cast<uint64_t>(mod);
        const uint64_t instanceId = 7;
        const uint32_t kind = 0;
        const uint32_t snapLen = 256;
        const uint64_t snapAddr = reinterpret_cast<uint64_t>(snapshot);
        std::memcpy(ctl + JCM_SHM_CTL_MODULE, &handle, sizeof(handle));
        std::memcpy(ctl + JCM_SHM_CTL_INSTANCE, &instanceId, sizeof(instanceId));
        std::memcpy(ctl + JCM_SHM_CTL_KIND, &kind, sizeof(kind));
        std::memcpy(ctl + JCM_SHM_CTL_SNAP_LEN, &snapLen, sizeof(snapLen));
        std::memcpy(ctl + JCM_SHM_CTL_SNAP_ADDR, &snapAddr, sizeof(snapAddr));
        std::memcpy(ctl + JCM_SHM_CTL_SEQ, &seq, sizeof(seq));  /* publish LAST */

        CHECK(jcm_shm_kick() == JCM_SHM_OK, "shm kick renders without any JNIEnv");
        uint64_t echo = 0;
        uint32_t resStatus = 99, count = 99;
        std::memcpy(&echo, res + JCM_SHM_RES_SEQ, sizeof(echo));
        std::memcpy(&resStatus, res + JCM_SHM_RES_STATUS, sizeof(resStatus));
        std::memcpy(&count, res + JCM_SHM_RES_COUNT, sizeof(count));
        CHECK(echo == 1 && resStatus == JCM_SHM_OK,
              "result slot echoes the request sequence");
        CHECK(count == 0, "empty vehicle reports zero records");

        jcm_module_dispose_instance(mod, "shm-inst", 8, 0, -1);
        jcm_module_close(mod);
        jcm_shm_shutdown();
        CHECK(jcm_shm_kick() == JCM_SHM_ERR_NOT_INIT,
              "kick after shutdown refuses cleanly");
    }

    jcm_reclaimer_quiesce();
    CHECK(jcm_reclaimer_pending() == 0, "final deferred reclamation drained");

    /* ---- 5. clearing the vtable restores the legacy plane ------------ */
    jcm_ffm_init(nullptr, 0);
    CHECK(jcm_ffm_init(&table, sizeof(table)) == 0, "vtable re-install after clear");

    if (g_failures == 0) {
        std::printf("FFM REGRESSION OK: JNI-free plane (vtable + downcalls + shm slots)\n");
        return 0;
    }
    std::fprintf(stderr, "FFM REGRESSION FAILED (%d)\n", g_failures);
    return 1;
}
