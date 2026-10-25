/**
 * native_poller_test.cpp — headless regression for the bridge's POLLER
 * plane: the JNI-free fallback that serves Minecraft 1.17.1-1.20.4 on
 * their stock JDK 16/17/21 runtimes, where no native-call API at all is
 * available (the final java.lang.foreign needs 22+, its preview needs
 * --enable-preview).
 *
 * The bridge translation unit is linked INTO this executable, so its
 * constructor-started poller thread is ALREADY scanning for the region
 * before main() runs — exactly like a System.load'ed bridge in a JVM.
 * This test then plays the ENTIRE Java side in plain C:
 *
 *   1. creates the region FILE at the fixed discoverable name, writes the
 *      v2 header (magic2 / layout 2 / epoch / MODE=1) and the HANDSHAKE
 *      pointer files into the temp directories the poller scans;
 *   2. waits for HDR_READY == epoch (the poller's attach publish);
 *   3. drives the full module lifecycle over CONTROL-SLOT commands:
 *      open / abi / identity strings / state size / register / render /
 *      dispose / reclaimer diagnostics / close — every request published
 *      and answered exactly like ShmPoller.java does;
 *   4. ANSWERS the script->Java host services while waiting: the SVC
 *      block is serviced by the same canned adapters native_ffm_test
 *      uses for the vtable, which proves the api_probe module's api
 *      round-trips really cross the shared pages;
 *   5. publishes one CBQ entry (the Java->C++ api re-entry) and waits
 *      for the poller to drain it — the queue every wait loop services;
 *   6. issues CMD_SHUTDOWN and observes the poller detach.
 *
 * Every platform CI runs this: it is the only test that proves the whole
 * plane works with NO JVM and NO native-call API anywhere.
 */
#include <mtr/mtr_native.h>
#include <mtr/shm_channel.h>

#include <atomic>
#include <chrono>
#include <cstdio>
#include <cstring>
#include <string>
#include <thread>
#include <vector>

#if defined(_WIN32)
#include <windows.h>
#else
#include <fcntl.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>
#endif

/* Declared by bridge.cpp (linked into this executable). */
extern "C" {
int32_t jcm_poller_attached(void);
int64_t jcm_reclaimer_pending(void);
void    jcm_reclaimer_quiesce(void);
}

static int g_failures = 0;
#define CHECK(cond, msg)                                                  \
    do {                                                                  \
        if (cond) {                                                       \
            std::printf("POLLER REGRESSION OK: %s\n", msg);               \
        } else {                                                          \
            std::fprintf(stderr, "POLLER REGRESSION FAIL: %s (line %d)\n",\
                         msg, __LINE__);                                 \
            ++g_failures;                                                 \
        }                                                                 \
    } while (0)

/* ---- shared-memory accessors (same discipline as the bridge) -------- */

static inline uint64_t rd64(const uint8_t* p) {
    const volatile uint64_t* q = reinterpret_cast<const volatile uint64_t*>(p);
    const uint64_t v = *q;
    std::atomic_thread_fence(std::memory_order_acquire);
    return v;
}
static inline uint32_t rd32(const uint8_t* p) {
    const volatile uint32_t* q = reinterpret_cast<const volatile uint32_t*>(p);
    const uint32_t v = *q;
    std::atomic_thread_fence(std::memory_order_acquire);
    return v;
}
static inline void wr64(uint8_t* p, uint64_t v) {
    std::atomic_thread_fence(std::memory_order_release);
    *reinterpret_cast<volatile uint64_t*>(p) = v;
}
static inline void put64(uint8_t* p, uint64_t v) { std::memcpy(p, &v, sizeof(v)); }
static inline void put32(uint8_t* p, uint32_t v) { std::memcpy(p, &v, sizeof(v)); }
static inline void puti32(uint8_t* p, int32_t v) { std::memcpy(p, &v, sizeof(v)); }

/* ---------------------------------------------------------------------- */
/* the fake Java side: SVC adapters (same canned behaviour as the FFM      */
/* test's fake vtable, so both planes prove the identical C contract)     */
/* ---------------------------------------------------------------------- */

static int g_invoke_calls = 0;
static int g_acquire_calls = 0;
static int g_rasterize_calls = 0;
static int g_resource_calls = 0;

static thread_local std::string g_reply;

static int64_t fake_invoke_api(const char* key, int32_t key_len,
                               const char* request, int32_t request_len,
                               int32_t* out_len) {
    (void)key; (void)key_len;
    ++g_invoke_calls;
    const std::string req(request, static_cast<size_t>(request_len));
    if (req.find("\"release\"") != std::string::npos) {
        g_reply = "{\"value\":null}";
    } else if (req.find("\"largeId\"") != std::string::npos) {
        g_reply = "{\"value\":9223372036854775000}";
    } else if (req.find("\"op\":\"root\"") != std::string::npos
            || req.find("\"op\":\"new\"") != std::string::npos) {
        g_reply = "{\"value\":{\"$handle\":\"c0\",\"$type\":\"java.lang.Object\"}}";
    } else {
        g_reply = "{\"value\":null}";
    }
    *out_len = static_cast<int32_t>(g_reply.size());
    return reinterpret_cast<int64_t>(g_reply.data());
}

/* ---------------------------------------------------------------------- */
/* SVC dispatch: answers pending host-service requests (Java's role)     */
/* ---------------------------------------------------------------------- */

static uint64_t g_last_served_svc[JCM_SVC_SLOTS] = {0, 0, 0, 0};

static void service_svc(uint8_t* base) {
    /* The poller takes SVC slots by nesting depth: scan every slot, exactly
       like ShmPoller.servicePendingRequests does. */
    for (int slotIdx = 0; slotIdx < JCM_SVC_SLOTS; slotIdx++) {
    uint8_t* svc = base + JCM_SHM_SVC_OFFSET
            + static_cast<size_t>(slotIdx) * JCM_SVC_SLOT_BYTES;
    const uint64_t seq = rd64(svc + JCM_SHM_SVC_SEQ);
    if (seq == 0 || seq == g_last_served_svc[slotIdx]) continue;
    g_last_served_svc[slotIdx] = seq;

    const int id = static_cast<int>(rd32(svc + JCM_SHM_SVC_ID));
    uint64_t w[32] = {0};
    std::memcpy(w, svc + JCM_SHM_SVC_ARGS, sizeof(w));
    long long ret = 0;

    auto addr = [](uint64_t word) {
        return reinterpret_cast<uint8_t*>(static_cast<uintptr_t>(word));
    };
    auto word32 = [](uint64_t word) { return static_cast<int32_t>(word); };

    switch (id) {
    case JCM_SVC_ACQUIRE_MODEL: {
        ++g_acquire_calls;
        ret = 1234;
        break;
    }
    case JCM_SVC_RELEASE_MODEL:
        break;
    case JCM_SVC_RASTERIZE_TEXT: {
        ++g_rasterize_calls;
        uint8_t* pixels = addr(w[8]);
        const int32_t out_w = word32(w[9]);
        const int32_t out_h = word32(w[10]);
        if (pixels && out_w > 0 && out_h > 0) {
            std::memset(pixels, 0xff, static_cast<size_t>(out_w) * out_h * 4);
            ret = 1;
        } else {
            ret = -1;
        }
        break;
    }
    case JCM_SVC_MEASURE_FONT_TEXT: {
        double* out = reinterpret_cast<double*>(addr(w[7]));
        if (out) {
            out[0] = 10.0; out[1] = 8.0; out[2] = 2.0; out[3] = 12.0;
            ret = 0;
        } else {
            ret = -1;
        }
        break;
    }
    case JCM_SVC_DRAW_FONT_TEXT: {
        uint8_t* pixels = addr(w[15]);
        const int32_t width = word32(w[16]);
        const int32_t height = word32(w[17]);
        int32_t* bounds = reinterpret_cast<int32_t*>(addr(w[18]));
        if (pixels && width > 0 && height > 0 && bounds) {
            std::memset(pixels, 0x7f, static_cast<size_t>(width) * height * 4);
            bounds[0] = 0; bounds[1] = 0; bounds[2] = width; bounds[3] = height;
            ret = 0;
        } else {
            ret = -1;
        }
        break;
    }
    case JCM_SVC_CREATE_RESOURCES: {
        ++g_resource_calls;
        const int32_t tex = word32(w[0]);
        const int32_t quad = word32(w[1]);
        int32_t* out = reinterpret_cast<int32_t*>(addr(w[8]));
        if (out) {
            for (int32_t i = 0; i < tex + quad; i++) out[i] = 1000 + i;
            ret = 0;
        } else {
            ret = -1;
        }
        break;
    }
    case JCM_SVC_INVOKE_API: {
        int32_t* out_len = reinterpret_cast<int32_t*>(addr(w[4]));
        ret = fake_invoke_api(reinterpret_cast<const char*>(addr(w[0])),
                             word32(w[1]),
                             reinterpret_cast<const char*>(addr(w[2])),
                             word32(w[3]), out_len);
        break;
    }
    default:
        ret = 0;
        break;
    }
    put64(svc + JCM_SHM_SVC_RET, static_cast<uint64_t>(ret));
    wr64(svc + JCM_SHM_SVC_RSP, seq);   /* answer LAST */
    }
}

/* ---------------------------------------------------------------------- */
/* command publishing + waiting (Java's ShmPoller role)                  */
/* ---------------------------------------------------------------------- */

static uint64_t g_ctl_seq = 0;

static int32_t wait_result(uint8_t* base, uint64_t seq, int64_t timeout_ms) {
    const auto deadline = std::chrono::steady_clock::now()
            + std::chrono::milliseconds(timeout_ms);
    for (;;) {
        service_svc(base);
        if (rd64(base + JCM_SHM_RESULT_OFFSET + JCM_SHM_RES_SEQ) == seq) {
            return static_cast<int32_t>(rd32(base + JCM_SHM_RESULT_OFFSET + JCM_SHM_RES_STATUS));
        }
        if (std::chrono::steady_clock::now() > deadline) {
            return JCM_SHM_ERR_REQUEST;   /* caller reports the timeout */
        }
        std::this_thread::sleep_for(std::chrono::microseconds(100));
    }
}

static int32_t command(uint8_t* base, uint32_t kind, uint64_t module,
                       const void* args, size_t argsLen,
                       void* reply, size_t replyCap, int64_t* extra) {
    uint8_t* ctl = base + JCM_SHM_CONTROL_OFFSET;
    const uint64_t seq = ++g_ctl_seq;
    put64(ctl + JCM_SHM_CTL_MODULE, module);
    put64(ctl + JCM_SHM_CTL_INSTANCE, 0);
    put32(ctl + JCM_SHM_CTL_KIND, kind);
    put64(ctl + JCM_SHM_CTL_ARG_ADDR, reinterpret_cast<uint64_t>(args));
    put64(ctl + JCM_SHM_CTL_ARG_LEN, static_cast<uint64_t>(argsLen));
    put64(ctl + JCM_SHM_CTL_RPLY_ADDR, reinterpret_cast<uint64_t>(reply));
    put64(ctl + JCM_SHM_CTL_RPLY_LEN, static_cast<uint64_t>(replyCap));
    wr64(ctl + JCM_SHM_CTL_SEQ, seq);   /* request publish LAST */
    const int32_t status = wait_result(base, seq, 5000);
    if (extra) {
        uint64_t v = 0;
        std::memcpy(&v, base + JCM_SHM_RESULT_OFFSET + JCM_SHM_RES_EXTRA, sizeof(v));
        *extra = static_cast<int64_t>(v);
    }
    return status;
}

/* ---------------------------------------------------------------------- */
/* region + handshake creation (Java's NativeShm role)                   */
/* ---------------------------------------------------------------------- */

static std::string temp_dir() {
#if defined(_WIN32)
    char buffer[MAX_PATH];
    const UINT n = GetTempPathA(MAX_PATH, buffer);
    return n > 0 && n < MAX_PATH ? std::string(buffer, n) : std::string(".\\");
#else
    const char* tmpdir = std::getenv("TMPDIR");
    if (tmpdir && *tmpdir) return std::string(tmpdir);
    return std::string("/tmp");
#endif
}

static long current_pid() {
#if defined(_WIN32)
    return static_cast<long>(GetCurrentProcessId());
#else
    return static_cast<long>(getpid());
#endif
}

struct Region {
    uint8_t* base = nullptr;
    int64_t size = 0;
    std::string file;
    std::vector<std::string> handshake;
#ifdef _WIN32
    void* map_handle = nullptr;
    void* file_handle = nullptr;
#endif
};

static bool write_text_file(const std::string& path, const std::string& content) {
#ifdef _WIN32
    HANDLE file = CreateFileA(path.c_str(), GENERIC_WRITE, 0, nullptr,
                              CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, nullptr);
    if (file == INVALID_HANDLE_VALUE) return false;
    DWORD written = 0;
    const BOOL ok = WriteFile(file, content.data(),
                              static_cast<DWORD>(content.size()), &written, nullptr);
    CloseHandle(file);
    return ok && written == content.size();
#else
    FILE* f = std::fopen(path.c_str(), "wb");
    if (!f) return false;
    const size_t n = std::fwrite(content.data(), 1, content.size(), f);
    std::fclose(f);
    return n == content.size();
#endif
}

static bool create_region(Region* region, uint32_t epoch) {
    const long pid = current_pid();
    const std::string dir = temp_dir();
    std::string sep;
#ifdef _WIN32
    if (!dir.empty() && dir.back() != '\\' && dir.back() != '/') sep = "\\";
    region->file = dir + sep + "jcm_shmp_" + std::to_string(pid) + ".tmp";
    const std::wstring wide(region->file.begin(), region->file.end());
    /* Share-mode is load-bearing: the bridge's poller co-opens the region
       file with GENERIC_READ|GENERIC_WRITE + share-all — a handle opened
       with share mode 0 denies it (ERROR_SHARING_VIOLATION) and the poller
       can never attach (the old code failed all 23 checks of this test on
       every Windows CI run; Java's Files APIs share by default, which is
       why the JVM-path attach worked). */
    HANDLE file = CreateFileW(wide.c_str(), GENERIC_READ | GENERIC_WRITE,
                              FILE_SHARE_READ | FILE_SHARE_WRITE | FILE_SHARE_DELETE,
                              nullptr, CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, nullptr);
    if (file == INVALID_HANDLE_VALUE) return false;
    LARGE_INTEGER li;
    li.QuadPart = region->size;
    HANDLE mapping = CreateFileMappingW(file, nullptr, PAGE_READWRITE,
                                        li.HighPart, li.LowPart, nullptr);
    if (!mapping) { CloseHandle(file); return false; }
    void* view = MapViewOfFile(mapping, FILE_MAP_ALL_ACCESS, 0, 0,
                               static_cast<SIZE_T>(region->size));
    if (!view) { CloseHandle(mapping); CloseHandle(file); return false; }
    region->file_handle = file;
    region->map_handle = mapping;
    region->base = static_cast<uint8_t*>(view);
#else
    if (!dir.empty() && dir.back() != '/') sep = "/";
    region->file = dir + sep + "jcm_shmp_" + std::to_string(pid) + ".tmp";
    std::remove(region->file.c_str());
    const int fd = open(region->file.c_str(), O_RDWR | O_CREAT, 0600);
    if (fd < 0) return false;
    if (ftruncate(fd, region->size) != 0) { close(fd); return false; }
    void* map = mmap(nullptr, static_cast<size_t>(region->size),
                     PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
    close(fd);
    if (map == MAP_FAILED) return false;
    region->base = static_cast<uint8_t*>(map);
#endif
    /* v2 header (Java's discipline at region creation). */
    std::memset(region->base, 0, JCM_SHM_WINDOWS_OFFSET);
    put64(region->base + JCM_SHM_HDR_MAGIC, JCM_SHM_MAGIC);
    put32(region->base + JCM_SHM_HDR_VERSION, JCM_SHM_LAYOUT_VERSION);
    put64(region->base + JCM_SHM_HDR_SIZE, static_cast<uint64_t>(region->size));
    put64(region->base + JCM_SHM_HDR_CONTROL, JCM_SHM_CONTROL_OFFSET);
    put64(region->base + JCM_SHM_HDR_RESULT, JCM_SHM_RESULT_OFFSET);
    put64(region->base + JCM_SHM_HDR_WINDOWS, JCM_SHM_WINDOWS_OFFSET);
    put32(region->base + JCM_SHM_HDR_EPOCH, epoch);
    put64(region->base + JCM_SHM_HDR_MODE, 1);   /* poller plane owns it */
    put64(region->base + 64, JCM_SHM_SVC_OFFSET);
    put64(region->base + 72, JCM_SHM_CBQ_OFFSET);

    /* Handshake pointer files in every candidate directory. */
    const std::string content = region->file + "\n"
            + std::to_string(region->size) + "\n"
            + std::to_string(epoch) + "\n";
    const std::string hs = "jcm_shm_hs_" + std::to_string(pid);
#ifdef _WIN32
    /* also drop a copy into TEMP/TMP for good measure */
    std::vector<std::string> dirs;
    const char* temp = std::getenv("TEMP");
    if (temp && *temp) dirs.push_back(std::string(temp));
    const char* tmp = std::getenv("TMP");
    if (tmp && *tmp) dirs.push_back(std::string(tmp));
    dirs.push_back(dir);
#else
    std::vector<std::string> dirs{"/tmp", "/var/tmp"};
    if (dir != "/tmp" && dir != "/var/tmp") dirs.push_back(dir);
#endif
    bool any = false;
    for (const std::string& d : dirs) {
        std::string dsep;
#ifdef _WIN32
        if (!d.empty() && d.back() != '\\' && d.back() != '/') dsep = "\\";
#else
        if (!d.empty() && d.back() != '/') dsep = "/";
#endif
        const std::string path = d + dsep + hs;
        if (write_text_file(path, content)) {
            region->handshake.push_back(path);
            any = true;
        }
    }
    return any;
}

static void destroy_region(Region* region) {
    for (const std::string& hs : region->handshake) {
#ifdef _WIN32
        DeleteFileA(hs.c_str());
#else
        std::remove(hs.c_str());
#endif
    }
    region->handshake.clear();
#ifdef _WIN32
    if (region->base) UnmapViewOfFile(region->base);
    if (region->map_handle) CloseHandle(static_cast<HANDLE>(region->map_handle));
    if (region->file_handle) CloseHandle(static_cast<HANDLE>(region->file_handle));
    DeleteFileA(region->file.c_str());
#else
    if (region->base) munmap(region->base, static_cast<size_t>(region->size));
    std::remove(region->file.c_str());
#endif
    region->base = nullptr;
}

/* ---------------------------------------------------------------------- */
/* CBQ: a fake module api callback (the Java->C++ re-entry)               */
/* ---------------------------------------------------------------------- */

static const char* cbq_probe(void*, const char* request, int32_t len, int32_t* out_len) {
    static thread_local std::string reply;
    reply.assign("{\"echo\":\"");
    reply.append(request, static_cast<size_t>(len));
    reply.append("\"}");
    *out_len = static_cast<int32_t>(reply.size());
    return reply.data();
}

static int64_t publish_cbq(uint8_t* base, int slot, long long function, long long user,
                           const char* request, size_t requestLen,
                           char* reply, size_t replyCap, int64_t timeout_ms,
                           int64_t* replyLen) {
    uint8_t* q = base + JCM_SHM_CBQ_OFFSET
            + static_cast<size_t>(slot) * JCM_CBQ_SLOT_BYTES;
    const uint64_t seq = static_cast<uint64_t>(slot) + 1 + 0x100 * (slot + 1);
    put64(q + JCM_SHM_CBQ_FN, static_cast<uint64_t>(function));
    put64(q + JCM_SHM_CBQ_USER, static_cast<uint64_t>(user));
    put64(q + JCM_SHM_CBQ_REQ_ADDR, reinterpret_cast<uint64_t>(request));
    put64(q + JCM_SHM_CBQ_REQ_LEN, static_cast<uint64_t>(requestLen));
    put64(q + JCM_SHM_CBQ_RPLY_ADDR, reinterpret_cast<uint64_t>(reply));
    put64(q + JCM_SHM_CBQ_RPLY_CAP, static_cast<uint64_t>(replyCap));
    wr64(q + JCM_SHM_CBQ_SEQ, seq);   /* publish LAST */
    const auto deadline = std::chrono::steady_clock::now()
            + std::chrono::milliseconds(timeout_ms);
    for (;;) {
        service_svc(base);   /* a nested service request may arrive first */
        if (rd64(q + JCM_SHM_CBQ_RSP) == seq) {
            uint64_t len = 0;
            std::memcpy(&len, q + JCM_SHM_CBQ_RPLY_LEN, sizeof(len));
            *replyLen = static_cast<int64_t>(len);
            return 0;
        }
        if (std::chrono::steady_clock::now() > deadline) return -1;
        std::this_thread::sleep_for(std::chrono::microseconds(100));
    }
}

/* ---------------------------------------------------------------------- */

int main(int argc, char** argv) {
    if (argc < 3) {
        std::fprintf(stderr, "POLLER REGRESSION FAIL: usage: native_poller_test <legacy_probe> <content_probe>\n");
        return 2;
    }
    const char* legacyPath = argv[1];
    const char* apiPath = argv[2];

    CHECK(jcm_poller_attached() == 0, "poller idle before any region exists");

    Region region;
    region.size = 1 << 20;
    const uint32_t epoch = 0xABCD;
    CHECK(create_region(&region, epoch), "region file + handshake created");

    /* Wait for the poller's attach publish (READY == epoch). */
    {
        bool attached = false;
        for (int i = 0; i < 200 && !attached; i++) {
            uint64_t ready = 0;
            std::memcpy(&ready, region.base + JCM_SHM_HDR_READY, sizeof(ready));
            if (ready == epoch) attached = true;
            else std::this_thread::sleep_for(std::chrono::milliseconds(10));
        }
        CHECK(attached, "poller found the region through the handshake and attached");
        CHECK(jcm_poller_attached() == 1, "poller reports itself attached");
    }
    uint8_t* base = region.base;

    /* ---- module lifecycle over commands -------------------------------- */
    int64_t extra = 0;
    CHECK(command(base, JCM_CMD_OPEN, 0, legacyPath, std::strlen(legacyPath),
                  nullptr, 0, &extra) == JCM_SHM_OK && extra != 0,
          "CMD_OPEN returns a module handle");
    const uint64_t legacy = static_cast<uint64_t>(extra);

    CHECK(command(base, JCM_CMD_ABI, legacy, nullptr, 0, nullptr, 0, &extra) == JCM_SHM_OK
          && extra == 6, "CMD_ABI reports ABI 6");
    CHECK(command(base, JCM_CMD_STATE_SIZE, legacy, nullptr, 0, nullptr, 0, &extra)
          == JCM_SHM_OK && extra == 4, "CMD_STATE_SIZE reports the state block");

    char reply[128] = {0};
    CHECK(command(base, JCM_CMD_SCRIPT_ID, legacy, nullptr, 0, reply, sizeof(reply), &extra)
          == JCM_SHM_OK && extra == 11 && std::strcmp(reply, "test:legacy") == 0,
          "CMD_SCRIPT_ID replies through the reply window");
    CHECK(command(base, JCM_CMD_SCRIPT_TYPE, legacy, nullptr, 0, reply, sizeof(reply), &extra)
          == JCM_SHM_OK && extra > 0, "CMD_SCRIPT_TYPE replies");

    /* ---- render through the control slot -------------------------------- */
    static uint8_t snapshot[256];   /* zeroed = valid "empty" ABI blob */
    {
        /* register the instance first (args: u64 id + key utf8) */
        uint8_t args[8 + 8];
        uint64_t id = 7;
        std::memcpy(args, &id, sizeof(id));
        std::memcpy(args + 8, "shm-inst", 8);
        CHECK(command(base, JCM_CMD_REGISTER, legacy, args, sizeof(args),
                      nullptr, 0, &extra) == JCM_SHM_OK,
              "CMD_REGISTER maps the long instance id");

        uint8_t* ctl = base + JCM_SHM_CONTROL_OFFSET;
        const uint64_t seq = ++g_ctl_seq;
        put64(ctl + JCM_SHM_CTL_MODULE, legacy);
        put64(ctl + JCM_SHM_CTL_INSTANCE, 7);
        put32(ctl + JCM_SHM_CTL_KIND, 0);
        put32(ctl + JCM_SHM_CTL_SNAP_LEN, 256);
        put64(ctl + JCM_SHM_CTL_SNAP_ADDR, reinterpret_cast<uint64_t>(snapshot));
        wr64(ctl + JCM_SHM_CTL_SEQ, seq);
        int32_t status = wait_result(base, seq, 5000);
        CHECK(status == JCM_SHM_OK, "render request served from the shared pages");
        uint32_t count = 0;
        std::memcpy(&count, base + JCM_SHM_RESULT_OFFSET + JCM_SHM_RES_COUNT, sizeof(count));
        CHECK(count == 0, "empty vehicle reports zero records");
        /* second render: same instance, state persists */
        const uint64_t seq2 = ++g_ctl_seq;
        wr64(ctl + JCM_SHM_CTL_SEQ, seq2);
        status = wait_result(base, seq2, 5000);
        CHECK(status == JCM_SHM_OK, "second render reuses the instance state");
    }

    /* ---- dispose + reclaimer diagnostics over commands ------------------ */
    {
        uint8_t args[16 + 8];
        uint64_t noLive = 0;
        std::memcpy(args, &noLive, 8);
        std::memcpy(args + 8, &noLive, 8);
        std::memcpy(args + 16, "shm-inst", 8);
        CHECK(command(base, JCM_CMD_DISPOSE, legacy, args, sizeof(args),
                      nullptr, 0, &extra) == JCM_SHM_OK,
              "CMD_DISPOSE disposes the instance (no live buffer -> zeroed fallback)");
        CHECK(command(base, JCM_CMD_RECLAIMER, 0, nullptr, 0, nullptr, 0, &extra)
              == JCM_SHM_OK && extra == 0,
              "CMD_RECLAIMER drains the deferred queue off-thread");
    }

    /* ---- the API round-trip: host services over the SVC block ------------- */
    {
        int64_t handle = 0;
        CHECK(command(base, JCM_CMD_OPEN, 0, apiPath, std::strlen(apiPath),
                      nullptr, 0, &handle) == JCM_SHM_OK && handle != 0,
              "content probe opened over CMD_OPEN");
        const uint64_t api = static_cast<uint64_t>(handle);

        uint8_t args[8 + 4];
        uint64_t id = 1;
        std::memcpy(args, &id, sizeof(id));
        std::memcpy(args + 8, "api", 3);
        CHECK(command(base, JCM_CMD_REGISTER, api, args, sizeof(args),
                      nullptr, 0, &extra) == JCM_SHM_OK,
              "content probe instance registered");

        const int invokesBefore = g_invoke_calls;
        uint8_t* ctl = base + JCM_SHM_CONTROL_OFFSET;
        const uint64_t seq = ++g_ctl_seq;
        put64(ctl + JCM_SHM_CTL_MODULE, api);
        put64(ctl + JCM_SHM_CTL_INSTANCE, 1);
        put32(ctl + JCM_SHM_CTL_KIND, 1);
        put32(ctl + JCM_SHM_CTL_SNAP_LEN, 256);
        put64(ctl + JCM_SHM_CTL_SNAP_ADDR, reinterpret_cast<uint64_t>(snapshot));
        wr64(ctl + JCM_SHM_CTL_SEQ, seq);
        const int32_t status = wait_result(base, seq, 5000);
        /* Same expectations as native_ffm_test: create() succeeds (its api
           calls round-trip through the SVC block), render() throws on the
           zeroed PIDS snapshot — ERR_RENDER. */
        CHECK(status == JCM_SHM_ERR_RENDER,
              "content create succeeds via SVC api, render fails on empty snapshot");
        CHECK(g_invoke_calls > invokesBefore,
              "host invoke_api requests were answered by the fake Java side");

        uint8_t dargs[16 + 3];
        uint64_t noLive = 0;
        std::memcpy(dargs, &noLive, 8);
        std::memcpy(dargs + 8, &noLive, 8);
        std::memcpy(dargs + 16, "api", 3);
        CHECK(command(base, JCM_CMD_DISPOSE, api, dargs, sizeof(dargs),
                      nullptr, 0, &extra) == JCM_SHM_OK,
              "content probe dispose also round-trips (dispose may call the api)");
        CHECK(g_invoke_calls > invokesBefore + 1,
              "dispose api round-trips through the SVC block too");
        CHECK(command(base, JCM_CMD_CLOSE, api, nullptr, 0, nullptr, 0, &extra)
              == JCM_SHM_OK, "CMD_CLOSE drops the module bookkeeping");
    }

    /* ---- the CBQ: Java->C++ api re-entry drained by the poller ----------- */
    {
        char replyBuffer[256] = {0};
        int64_t replyLen = 0;
        const char request[] = "{\"params\":[1,2,3]}";
        const int64_t status = publish_cbq(base, 0,
                reinterpret_cast<long long>(&cbq_probe), 0,
                request, sizeof(request) - 1,
                replyBuffer, sizeof(replyBuffer), 5000, &replyLen);
        CHECK(status == 0 && replyLen > 0
              && std::strstr(replyBuffer, "\"echo\"") != nullptr
              && std::strstr(replyBuffer, request) != nullptr,
              "CBQ entry drained by the poller, reply copied back");
    }

    /* ---- abandon + self-heal (poison discipline, yanyang.5) ----------------
     *
     * ShmPoller poisons HDR_MODE=0 when a Java waiter gives up on the
     * region. Three things must follow, all exercised here:
     *
     *   1. a poller stuck inside a host-service wait (this render's
     *      create() needs the api round-trip, and the fake Java side
     *      deliberately STOPS answering) must bail out through the
     *      svc_call MODE check and publish a degraded result — instead
     *      of wedging the request for the full wait budget;
     *   2. the idle-path liveness revalidation must then detach the
     *      poller from the poisoned region (the reset() contract);
     *   3. a FRESH region with a new epoch must re-attach and serve
     *      commands again — the plane self-heals after an abandon.
     */
    {
        int64_t handle = 0;
        CHECK(command(base, JCM_CMD_OPEN, 0, apiPath, std::strlen(apiPath),
                      nullptr, 0, &handle) == JCM_SHM_OK && handle != 0,
              "abandon scenario: api probe re-opened");
        const uint64_t api = static_cast<uint64_t>(handle);

        uint8_t args[8 + 3];
        uint64_t id = 2;
        std::memcpy(args, &id, sizeof(id));
        std::memcpy(args + 8, "api", 3);
        CHECK(command(base, JCM_CMD_REGISTER, api, args, sizeof(args),
                      nullptr, 0, &extra) == JCM_SHM_OK,
              "abandon scenario: instance registered");

        /* Publish the render, then STOP answering service requests and
           poison after a grace period — exactly a ShmPoller timeout. */
        uint8_t* ctl = base + JCM_SHM_CONTROL_OFFSET;
        const uint64_t seq = ++g_ctl_seq;
        put64(ctl + JCM_SHM_CTL_MODULE, api);
        put64(ctl + JCM_SHM_CTL_INSTANCE, 2);
        put32(ctl + JCM_SHM_CTL_KIND, 1);
        put32(ctl + JCM_SHM_CTL_SNAP_LEN, 256);
        put64(ctl + JCM_SHM_CTL_SNAP_ADDR, reinterpret_cast<uint64_t>(snapshot));
        wr64(ctl + JCM_SHM_CTL_SEQ, seq);

        int32_t status = -1;
        bool poisoned = false;
        const auto started = std::chrono::steady_clock::now();
        for (;;) {
            /* deliberately NOT calling service_svc: the waiter abandoned it */
            if (rd64(base + JCM_SHM_RESULT_OFFSET + JCM_SHM_RES_SEQ) == seq) {
                status = static_cast<int32_t>(
                        rd32(base + JCM_SHM_RESULT_OFFSET + JCM_SHM_RES_STATUS));
                break;
            }
            const auto now = std::chrono::steady_clock::now();
            if (!poisoned && now - started > std::chrono::milliseconds(300)) {
                wr64(base + JCM_SHM_HDR_MODE, 0);   /* the abandon poison */
                poisoned = true;
            }
            if (now - started > std::chrono::milliseconds(8000)) break;
            std::this_thread::sleep_for(std::chrono::microseconds(200));
        }
        CHECK(status != -1, "poller unwound the abandoned request after the poison (no wedge)");
        const auto elapsed = std::chrono::duration_cast<std::chrono::milliseconds>(
                std::chrono::steady_clock::now() - started).count();
        CHECK(elapsed < 4000, "poison bail-out is fast (svc_call MODE check, not the 8 s budget)");

        /* The idle-path liveness revalidation must detach from the poison. */
        bool detached = false;
        for (int i = 0; i < 300 && !detached; i++) {
            if (jcm_poller_attached() == 0) detached = true;
            else std::this_thread::sleep_for(std::chrono::milliseconds(10));
        }
        CHECK(detached, "poller self-detached from the poisoned region (idle revalidation)");

        /* Self-heal: a fresh region (new epoch) re-attaches and serves. */
        destroy_region(&region);
        Region fresh;
        fresh.size = 1 << 20;
        const uint32_t epoch2 = 0xBEEF;
        CHECK(create_region(&fresh, epoch2), "self-heal: fresh region + handshake created");
        bool reattached = false;
        for (int i = 0; i < 200 && !reattached; i++) {
            uint64_t ready = 0;
            std::memcpy(&ready, fresh.base + JCM_SHM_HDR_READY, sizeof(ready));
            if (ready == epoch2) reattached = true;
            else std::this_thread::sleep_for(std::chrono::milliseconds(10));
        }
        CHECK(reattached, "self-heal: poller re-attached to the fresh region");
        /* Mirror ShmPoller.attach(): the per-slot served-seq bookkeeping
           belongs to the REGION, so it resets with the attach — otherwise
           the fresh region's first SVC request (seq restarts at 1) would
           alias the abandoned region's seqs and be skipped forever. */
        for (int i = 0; i < JCM_SVC_SLOTS; i++) g_last_served_svc[i] = 0;
        CHECK(command(fresh.base, JCM_CMD_RECLAIMER, 0, nullptr, 0, nullptr, 0, &extra)
              == JCM_SHM_OK && extra == 0,
              "self-heal: the fresh region serves commands again");
        CHECK(command(fresh.base, JCM_CMD_CLOSE, api, nullptr, 0, nullptr, 0, &extra)
              == JCM_SHM_OK,
              "self-heal: api probe closed over the fresh region");

        region = fresh;   /* the shutdown tail below runs against the fresh region */
        base = region.base;
    }

    /* ---- legacy module close + shutdown ---------------------------------- */
    CHECK(command(base, JCM_CMD_CLOSE, legacy, nullptr, 0, nullptr, 0, &extra)
          == JCM_SHM_OK, "legacy module closed over CMD_CLOSE");
    CHECK(command(base, JCM_CMD_RECLAIMER, 0, nullptr, 0, nullptr, 0, &extra)
          == JCM_SHM_OK && extra == 0, "final deferred reclamation drained");

    CHECK(command(base, JCM_CMD_SHUTDOWN, 0, nullptr, 0, nullptr, 0, &extra)
          == JCM_SHM_OK, "CMD_SHUTDOWN acknowledged");
    {
        bool detached = false;
        for (int i = 0; i < 200 && !detached; i++) {
            if (jcm_poller_attached() == 0) detached = true;
            else std::this_thread::sleep_for(std::chrono::milliseconds(10));
        }
        CHECK(detached, "poller detached after the shutdown command");
    }

    destroy_region(&region);

    if (g_failures == 0) {
        std::printf("POLLER REGRESSION OK: JNI-free plane (handshake + commands + SVC services + CBQ re-entry)\n");
        return 0;
    }
    std::fprintf(stderr, "POLLER REGRESSION FAILED (%d)\n", g_failures);
    return 1;
}
