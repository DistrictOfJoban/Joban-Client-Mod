/**
 * jni_bridge.cpp — the C-side dispatcher behind NativeScriptManager.java.
 *
 * TWO invocation planes reach the same cores, and NEITHER one contains a
 * single line of JNI (the JNI plane was removed in layout v2):
 *
 *   Plain-C jcm_* exports (the FFM plane, JDK 22+) — module lifecycle,
 *   the per-frame shared-memory kick, the direct render call, the JS
 *   proxy api callback, and the JcmFfmHostVTable upcalls installed via
 *   jcm_ffm_init. The bridge is loaded with SymbolLookup.libraryLookup,
 *   and every host service call crosses back into Java through a
 *   java.lang.foreign upcall stub.
 *
 *   The POLLER plane (JDK 16..21, any JVM without java.lang.foreign) —
 *   the bridge is System.load'ed purely to run its CONSTRUCTOR, which
 *   starts a background poller thread. The poller finds the shared
 *   region through a filesystem handshake (no native call receives the
 *   address: Java writes "jcm_shm_hs_<pid>" pointer files, the poller
 *   opens+mmaps the region file itself), validates it and then serves
 *   every request straight from the mapped pages: per-frame renders,
 *   module lifecycle commands, and — while it runs module code — the
 *   script->Java host services, which it posts back into the region as
 *   SVC requests that the waiting Java thread answers. Java->C++ api
 *   re-entry (JS-side proxies into module callbacks) travels a CBQ ring
 *   that the poller drains in every wait loop. The whole session crosses
 *   ZERO native-call API boundaries in both directions: no JNI, no
 *   FFM, just shared pages — see include/mtr/shm_channel.h (layout v2).
 *
 * Module hosting is identical on both planes: dlopen the script module,
 * resolve the 5 mtr* exports, keep a per-instance state map (one zeroed
 * block of mtrStateSize() bytes per instanceKey, mirroring JCM's
 * per-ScriptInstance `state` JS object), lazy mtrCreate on first use,
 * mtrRender with the live snapshot read in place, arena descriptor
 * publishing (shm result slot / direct render buffer — zero copies),
 * record handle patching, and deferred off-thread reclamation of every
 * non-trivial free (see the Reclaimer below).
 *
 * Threading: JCM serializes script execution per module on one executor;
 * the bridge adds a per-module mutex so reload paths stay safe. The
 * background Reclaimer and Poller threads never perform FFM upcalls;
 * the poller's host-service requests are answered by the very Java
 * thread whose command it is currently serving.
 *
 * Build (per platform):
 *   Linux   : g++ -std=c++17 -O2 -fPIC -shared -I../include jni_bridge.cpp \
 *             -o libjcm_native_bridge.so -ldl -pthread
 *   Windows : cl /LD /utf-8 /O2 /EHsc /I..\include ... → jcm_native_bridge.dll
 *   macOS   : clang++ ... → libjcm_native_bridge.dylib
 * (No JDK headers are needed anymore — the bridge is JDK-free.)
 * (CI builds all platforms — see .github/workflows/native-build.yml)
 *
 * NOTE: the output arenas are owned by the module and reused next frame;
 * the Java replay step must finish before the next render call (this is
 * the same capture-then-replay discipline JCM's ScriptRenderManager
 * already enforces on the render thread).
 */

#include <mtr/mtr_native.h>
#include <mtr/shm_channel.h>
#include <mtr/ffm_host.h>

#include <atomic>
#include <algorithm>
#include <chrono>
#include <condition_variable>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <deque>
#include <functional>
#include <mutex>
#include <new>
#include <string>
#include <thread>
#include <unordered_map>
#include <vector>

#if defined(_WIN32)
#include <windows.h>
#else
#include <dlfcn.h>
#include <fcntl.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>
#endif

namespace {

/* ------------------------------------------------------------------ */
/* module + instance bookkeeping                                       */
/* ------------------------------------------------------------------ */

struct Instance {
    std::vector<uint8_t> state;         /* mtrStateSize() zeroed block  */
    /* Length of the last snapshot the module actually consumed — used ONLY
       to size the module-owned zeroed fallback snapshot that dispose()
       receives when the driver hands over no live buffer. There is NO
       per-instance copy of the snapshot: with reusable driver buffers the
       retained copy used to be taken once at the first frame and then never
       refreshed, so it both cost ~snapshot-size bytes per instance for the
       instance's whole life and fed dispose() stale first-frame data. The
       C++-mode contract is now: live buffer when available, otherwise ONE
       shared, zero-filled snapshot per module (deterministic "empty"
       vehicle/PIDS instead of ancient garbage). */
    int64_t snapshot_len = 0;
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
    std::vector<int32_t> tex_w, tex_h;
    std::vector<float> quad_verts, quad_uv;
    std::vector<int32_t> quad_slot, quad_stage;
};

struct BridgeModule {
    void* dl = nullptr;
    /* Shm transport: long instance id -> instance key ("instanceKey/module/<handle>").
       Registered once per instance via the register command, scanned off
       when the string-keyed instance is disposed. */
    std::unordered_map<int64_t, std::string> shm_ids;
    /* ZEROED fallback snapshot shared by every dispose that arrives without
       a live buffer (module teardown, no-buffer dispose calls). One buffer
       per module instead of one stale copy per instance; sized to the
       largest snapshot this module has consumed and reused for its whole
       lifetime (RAII-freed with the module). A zeroed snapshot is a valid
       ABI struct whose counts are 0 — modules see an "empty" train/PIDS,
       which is both safer and more honest than a stale first-frame copy. */
    std::vector<uint8_t> zero_snapshot;

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

    /* ABI 10 lifetime policy — the DSO STAYS LOADED when the module closes:
       ~BridgeModule forgets the bookkeeping but does NOT dlclose. See the
       keep-loaded registry note in open_module(). */
    ~BridgeModule() = default;

    void close_library() {
        dl = nullptr;
    }
};

/* ------------------------------------------------------------------ */
/* shared-memory release/acquire accessors (portable, C++17)           */
/*                                                                     */
/* The region is written by several threads. Fence-based acquire/release */
/* pairs around volatile word accesses are correct on every supported    */
/* target (x86 is total-store-order anyway; MSVC honours the fences,     */
/* GCC/Clang lower them to the architecturally required instructions).   */
/* ------------------------------------------------------------------ */

inline uint64_t rd64(const uint8_t* p) {
    const volatile uint64_t* q = reinterpret_cast<const volatile uint64_t*>(p);
    const uint64_t v = *q;
    std::atomic_thread_fence(std::memory_order_acquire);
    return v;
}

inline uint32_t rd32(const uint8_t* p) {
    const volatile uint32_t* q = reinterpret_cast<const volatile uint32_t*>(p);
    const uint32_t v = *q;
    std::atomic_thread_fence(std::memory_order_acquire);
    return v;
}

inline void wr64(uint8_t* p, uint64_t v) {
    std::atomic_thread_fence(std::memory_order_release);
    *reinterpret_cast<volatile uint64_t*>(p) = v;
}

inline void wr32(uint8_t* p, uint32_t v) {
    std::atomic_thread_fence(std::memory_order_release);
    *reinterpret_cast<volatile uint32_t*>(p) = v;
}

inline void write_u64(uint8_t* p, uint64_t v) { std::memcpy(p, &v, sizeof(v)); }
inline void write_u32(uint8_t* p, uint32_t v) { std::memcpy(p, &v, sizeof(v)); }
inline void write_i32(uint8_t* p, int32_t v) { std::memcpy(p, &v, sizeof(v)); }

inline void cpu_pause() {
#if defined(__x86_64__) || defined(__i386__)
    __builtin_ia32_pause();
#else
    std::this_thread::yield();
#endif
}

/* ------------------------------------------------------------------ */
/* forward declarations (the Poller references the shared cores)      */
/* ------------------------------------------------------------------ */

BridgeModule* open_module(const char* path);
int32_t run_frame_locked(BridgeModule& mod, const char* key,
                         const void* snapshot, int64_t snapshotLen,
                         int32_t kind, JcmFrameOutput& out);
void dispose_instance_core(BridgeModule* mod, const char* key,
                           const void* live_snapshot, int64_t live_len);
void close_module_core(BridgeModule* mod);
/* Serves one request (render or command) off the control slot of ANY
   attached region mapping and publishes the result. Returns
   JCM_SHM_ERR_DETACHED after the shutdown command asks for a detach. */
int32_t serve_request(uint8_t* base, uint64_t seq);

/* ------------------------------------------------------------------ */
/* Java host hooks (v4/v5)                                             */
/* ------------------------------------------------------------------ */
/*
 * Model / texture lifetime belongs to the JVM: a GraphicsTexture is a
 * NativeImageBackedTexture, a model is an uploaded OptimizedModel. The
 * bridge therefore only RECORDS what the script asked for during create()
 * and hands the batch to the host, which owns the registries and must
 * build them on the Java side (render-thread semantics are preserved on
 * the SVC plane too: the services execute on the very Java thread whose
 * command is being served).
 */

/* ---- Panama FFM host vtable (JNI-free upcalls, JDK 22+ plane) -------
   Installed once via the jcm_ffm_init export by PanamaFfmChannel.java
   AFTER it creates java.lang.foreign upcall stubs. */
JcmFfmHostVTable g_ffm_host = {};
/* Function pointers do not implicitly convert to void* in C++, so the
   presence check takes the entry by its own pointer type. */
template <typename Entry>
static inline bool ffm_has(Entry entry) {
    return g_ffm_host.abi == JCM_FFM_HOST_ABI && entry != nullptr;
}

/* ---- shared-memory channel state (see include/mtr/shm_channel.h) ----
   FFM plane: the mapping's base address arrives once via jcm_shm_init
   (Java hands its own MappedByteBuffer address over), and jcm_shm_kick
   serves synchronously on the calling Java thread. Poller plane: the
   Poller maps the region file itself and serves requests in the
   background; Java publishes + waits. Both servers share the served-seq
   claim CAS, so a request can never be served twice. */
struct ShmState {
    uint8_t* base = nullptr;    /* FFM-plane mapping base (jcm_shm_init) */
    int64_t size = 0;
};
ShmState g_shm;
std::atomic<uint64_t> g_served_seq{0};
std::unordered_map<int64_t, BridgeModule*> g_modules;
std::mutex g_modules_mutex;

/* ------------------------------------------------------------------ */
/* C++-mode background reclamation                                     */
/* ------------------------------------------------------------------ */
/*
 * Every non-trivial free that used to run synchronously inside an entry
 * point — per-instance state blocks at dispose, the module-owned zeroed
 * fallback snapshot at close, oversized API reply buffers, and (via
 * JcmHostServices.defer_free, ABI 10) the SDK FrameRecorder's replaced
 * pixel arenas — is handed to this single worker thread instead, so the
 * free cost never lands on a JVM-owned thread (see the frame-budget
 * analysis in the project history).
 *
 * The reclaimer is a PLAIN C++ thread: the JVM does not know it exists —
 * no safepoint polls, no GC coupling. Callers only do an O(1) enqueue. */
class Reclaimer final {
public:
    Reclaimer() = default;
    Reclaimer(const Reclaimer&) = delete;
    Reclaimer& operator=(const Reclaimer&) = delete;

    ~Reclaimer() {
        {
            std::unique_lock<std::mutex> lk(m_);
            stop_ = true;
            if (!worker_.joinable()) return;   /* never used: nothing to join */
            lk.unlock();
            cv_.notify_all();
            worker_.join();
        }
    }

    void defer(std::function<void()>&& job) {
        {
            std::lock_guard<std::mutex> lk(m_);
            jobs_.emplace_back(std::move(job));
            ++pending_;
        }
        {
            std::lock_guard<std::mutex> lk(m_);
            if (!worker_.joinable()) {
                worker_ = std::thread([this] { run(); });
            }
        }
        cv_.notify_one();
    }

    void defer_bytes(std::vector<uint8_t>&& dead) {
        defer([dead = std::move(dead)] {});
    }

    template <typename T>
    void defer_container(std::vector<T>&& dead) {
        defer([dead = std::move(dead)] {});
    }

    void defer_str(std::string&& dead) {
        defer([dead = std::move(dead)] {});
    }

    void defer_malloc(void* pointer) {
        if (!pointer) return;
        defer([pointer] { std::free(pointer); });
    }

    int64_t pending() const {
        std::lock_guard<std::mutex> lk(m_);
        return pending_;
    }

    int64_t completed() const {
        std::lock_guard<std::mutex> lk(m_);
        return completed_;
    }

    void quiesce() {
        std::unique_lock<std::mutex> lk(m_);
        cv_done_.wait(lk, [this] { return pending_ == 0; });
    }

private:
    void run() {
        std::unique_lock<std::mutex> lk(m_);
        for (;;) {
            cv_.wait(lk, [this] { return stop_ || !jobs_.empty(); });
            if (jobs_.empty()) {
                if (stop_) return;
                continue;
            }
            std::function<void()> job = std::move(jobs_.front());
            jobs_.pop_front();
            lk.unlock();
            job();
            lk.lock();
            --pending_;
            ++completed_;
            if (pending_ == 0) {
                cv_done_.notify_all();
            }
        }
    }

    mutable std::mutex m_;
    std::condition_variable cv_;
    std::condition_variable cv_done_;
    std::deque<std::function<void()>> jobs_;
    std::thread worker_;
    bool stop_ = false;
    int64_t pending_ = 0;
    int64_t completed_ = 0;
};

Reclaimer g_reclaimer;

/* ABI 10 host hook: SDK modules hand their dead heap blocks here instead
 * of freeing them inside mtrRender/mtrDispose (JVM-owned threads). */
void host_defer_free(void*, void* pointer, size_t size) {
    (void)size;
    g_reclaimer.defer_malloc(pointer);
}

/* ------------------------------------------------------------------ */
/* The poller plane (JDK 16..21): region discovery + request service    */
/* ------------------------------------------------------------------ */

class Poller final {
public:
    Poller() = default;
    Poller(const Poller&) = delete;
    Poller& operator=(const Poller&) = delete;

    /* NOTE: no start() here, and no std::thread/std::mutex members. The
     * boot path (poller_boot() below) runs from the library constructor /
     * DllMain, which the linker places BEFORE the translation unit's
     * _GLOBAL__sub_I_* static constructors — i.e. while every namespace-
     * scope C++ object is still raw .bss zeros. std::mutex/std::thread are
     * C++ objects, so touching them from there was reading an unconstructed
     * zeroed pthread_mutex_t: glibc treats all-zero as a valid default
     * mutex (silently worked), but macOS does NOT (its pthread mutex needs
     * the _PTHREAD_MUTEX_SIG signature) and pthread_mutex_lock returned
     * EINVAL → uncaught std::system_error → SIGABRT on every macOS CI job
     * (see workflow run 39597883900). The boot therefore uses ONLY C
     * primitives — pthread_once/InitOnceExecuteOnce + a detached
     * pthread_create/CreateThread — and the Poller object itself is a
     * Meyers singleton constructed lazily INSIDE the booted thread, long
     * after every static constructor has run. */
    /** True when the poller is attached to a live region in poller mode.
     *  Host services gate their SVC tier on this. */
    bool svc_available() const {
        return attached_.load(std::memory_order_acquire) != nullptr;
    }

    /** Ask the run loop to detach from the current region (shutdown
     *  command). The loop observes the flag after the request that set
     *  it has been fully answered and published. */
    void request_detach() {
        detach_requested_.store(true, std::memory_order_release);
    }

    /** Publishes one host-service request into the SVC ring and waits
     *  for the Java answer (a waiting Java thread services it). Only
     *  ever called from THIS poller thread — the only thread that runs
     *  module code on the poller plane. */
    bool svc_call(uint32_t id, const uint64_t* words, int word_count, uint64_t* ret) {
        uint8_t* base = attached_.load(std::memory_order_acquire);
        if (!base) return false;
        /* REENTRANCY: a service answered by Java may re-enter C++ (a CBQ
           proxy) whose module code may ask for another service — nested
           calls take the NEXT ring slot by depth so an unanswered outer
           request is never overwritten (the single-slot design deadlocked
           exactly there). Depth exhaustion degrades the innermost call. */
        if (svc_depth_ >= JCM_SVC_SLOTS) {
            std::fprintf(stderr, "[jcm_native_bridge] service ring exhausted "
                                 "(depth %d) — innermost call degrades\n", svc_depth_);
            return false;
        }
        const int slot = svc_depth_++;
        struct DepthGuard {
            int& depth;
            ~DepthGuard() { --depth; }
        } depth_guard{svc_depth_};
        uint8_t* svc = base + JCM_SHM_SVC_OFFSET
                + static_cast<size_t>(slot) * JCM_SVC_SLOT_BYTES;
        const uint64_t seq = ++svc_seq_;
        for (int i = 0; i < word_count && i < 32; i++) {
            write_u64(svc + JCM_SHM_SVC_ARGS + 8ull * i, words[i]);
        }
        write_u32(svc + JCM_SHM_SVC_ID, id);
        write_u64(svc + JCM_SHM_SVC_RET, 0);
        wr64(svc + JCM_SHM_SVC_SEQ, seq);      /* publish request LAST */

        /* Drain the CBQ ring while waiting: a nested Java->C++ re-entry
           must never deadlock against this wait. A Java waiter typically
           answers in microseconds (it is spinning right next to us); the
           sleeps only matter for a JVM hiccup. */
        uint64_t spins = 0;
        for (;;) {
            drain_cbq(base);
            if (rd64(svc + JCM_SHM_SVC_RSP) == seq) {
                if (ret) *ret = rd64(svc + JCM_SHM_SVC_RET);
                return true;
            }
            if ((++spins & 0x3FF) == 0) {
                if (attached_.load(std::memory_order_acquire) != base) {
                    return false;   /* region reset from under us */
                }
                std::this_thread::sleep_for(std::chrono::microseconds(120));
            } else {
                cpu_pause();
            }
        }
    }

    /** Drains pending Java->C++ callback-queue slots (if any). Cheap to
     *  call from any of this thread's loops. */
    void drain_cbq(uint8_t* base) {
        for (int slot = 0; slot < JCM_CBQ_SLOTS; slot++) {
            uint8_t* q = base + JCM_SHM_CBQ_OFFSET
                    + static_cast<size_t>(slot) * JCM_CBQ_SLOT_BYTES;
            const uint64_t seq = rd64(q + JCM_SHM_CBQ_SEQ);
            if (seq == 0 || seq == cbq_served_[slot]) continue;
            cbq_served_[slot] = seq;
            using Callback = const char* (*)(void*, const char*, int32_t, int32_t*);
            auto fn = reinterpret_cast<Callback>(
                    static_cast<uintptr_t>(rd64(q + JCM_SHM_CBQ_FN)));
            const uint64_t user = rd64(q + JCM_SHM_CBQ_USER);
            const uint64_t reqAddr = rd64(q + JCM_SHM_CBQ_REQ_ADDR);
            const int32_t reqLen = static_cast<int32_t>(
                    std::min<uint64_t>(rd64(q + JCM_SHM_CBQ_REQ_LEN), 0x7fffffffu));
            const uint64_t rplyAddr = rd64(q + JCM_SHM_CBQ_RPLY_ADDR);
            const uint64_t rplyCap = rd64(q + JCM_SHM_CBQ_RPLY_CAP);
            int32_t replyLen = -1;
            const char* reply = nullptr;
            if (fn && rplyAddr && reqAddr) {
                reply = fn(reinterpret_cast<void*>(static_cast<uintptr_t>(user)),
                           reinterpret_cast<const char*>(static_cast<uintptr_t>(reqAddr)),
                           reqLen, &replyLen);
            }
            /* Publish the reply length and echo the seq LAST; Java spins
               on the rsp field and consumes the reply bytes after it. */
            wr64(q + JCM_SHM_CBQ_RPLY_LEN,
                 reply ? static_cast<uint64_t>(static_cast<uint32_t>(replyLen)) : 0);
            if (reply && replyLen > 0 && rplyCap > 0) {
                const int64_t n = std::min<int64_t>(replyLen, static_cast<int64_t>(rplyCap));
                std::memcpy(reinterpret_cast<void*>(static_cast<uintptr_t>(rplyAddr)),
                            reply, static_cast<size_t>(n));
            }
            wr64(q + JCM_SHM_CBQ_RSP, seq);
        }
    }

    /* ---- main loop entry (public: called by the boot trampolines) -- */

    void run_outer() {
        try {
            run_inner();
        } catch (const std::exception& e) {
            /* An exception escaping the raw thread entry would terminate
               the process — the poller must survive ANY allocation hiccup
               and keep scanning; report once and keep the plane degraded. */
            std::fprintf(stderr, "[jcm_native_bridge] poller thread exception: %s\n", e.what());
        } catch (...) {
            std::fprintf(stderr, "[jcm_native_bridge] poller thread unknown exception\n");
        }
    }

private:
    /* ---- region attachment -------------------------------------- */

    struct RegionFile {
        void* map = nullptr;      /* mmap / MapViewOfFile base       */
        void* mapping_handle = nullptr;   /* Windows: mapping handle  */
        void* file_handle = nullptr;       /* Windows: file handle     */
        int64_t size = 0;
#ifndef _WIN32
        int fd = -1;
#endif
    };

    static void close_region(RegionFile& r) {
#ifdef _WIN32
        if (r.map) UnmapViewOfFile(r.map);
        if (r.mapping_handle) CloseHandle(static_cast<HANDLE>(r.mapping_handle));
        if (r.file_handle) CloseHandle(static_cast<HANDLE>(r.file_handle));
#else
        if (r.map) munmap(r.map, static_cast<size_t>(r.size));
        if (r.fd >= 0) close(r.fd);
#endif
        r = RegionFile{};
    }

    /* Candidate directories for the handshake probe. Mirrors the Java
       side's write list (NativeShm.writeHandshake) — the overlap covers
       every supported platform, and JCM_SHM_HANDSHAKE_DIR is the
       documented escape hatch for exotic setups. */
    static std::vector<std::string> candidate_dirs() {
        std::vector<std::string> out;
        const char* override_dir = std::getenv("JCM_SHM_HANDSHAKE_DIR");
        if (override_dir && *override_dir) out.push_back(std::string(override_dir));
#ifdef _WIN32
        wchar_t wide[MAX_PATH];
        const UINT n = GetTempPathW(MAX_PATH, wide);
        if (n > 0 && n < MAX_PATH) {
            char utf8[MAX_PATH * 3];
            const int bytes = WideCharToMultiByte(CP_UTF8, 0, wide, static_cast<int>(n),
                                                  utf8, sizeof(utf8), nullptr, nullptr);
            if (bytes > 0) out.push_back(std::string(utf8, static_cast<size_t>(bytes)));
        }
        const char* temp = std::getenv("TEMP");
        if (temp && *temp) out.push_back(std::string(temp));
        const char* tmp = std::getenv("TMP");
        if (tmp && *tmp) out.push_back(std::string(tmp));
#else
        const char* tmpdir = std::getenv("TMPDIR");
        if (tmpdir && *tmpdir) out.push_back(std::string(tmpdir));
        out.push_back("/tmp");
        out.push_back("/var/tmp");
#endif
        /* de-duplicate, keep order */
        std::vector<std::string> unique;
        for (const std::string& d : out) {
            bool dup = false;
            for (const std::string& u : unique) dup = dup || u == d;
            if (!dup) unique.push_back(d);
        }
        return unique;
    }

    static long current_pid() {
#ifdef _WIN32
        return static_cast<long>(GetCurrentProcessId());
#else
        return static_cast<long>(getpid());
#endif
    }

    static bool parse_handshake(const std::string& text, std::string* path,
                                uint64_t* size, uint32_t* epoch) {
        size_t p = 0;
        std::string values[3];
        for (int line = 0; line < 3; line++) {
            const size_t nl = text.find('\n', p);
            if (nl == std::string::npos) return false;
            values[line] = text.substr(p, nl - p);
            p = nl + 1;
            while (!values[line].empty() && (values[line].back() == '\r'
                    || values[line].back() == ' ')) values[line].pop_back();
            while (!values[line].empty() && values[line].front() == ' ') {
                values[line].erase(values[line].begin());
            }
        }
        if (values[0].empty()) return false;
        *path = values[0];
        *size = std::strtoull(values[1].c_str(), nullptr, 10);
        *epoch = static_cast<uint32_t>(std::strtoul(values[2].c_str(), nullptr, 10));
        return *size >= JCM_SHM_WINDOWS_OFFSET + 4096ULL;
    }

#ifdef _WIN32
    static std::wstring utf8_to_wide(const std::string& text) {
        const int need = MultiByteToWideChar(CP_UTF8, 0, text.c_str(), -1, nullptr, 0);
        std::wstring wide(static_cast<size_t>(need > 0 ? need : 1), L'\0');
        MultiByteToWideChar(CP_UTF8, 0, text.c_str(), -1, &wide[0], need);
        while (!wide.empty() && wide.back() == L'\0') wide.pop_back();
        return wide;
    }
#endif

    static bool read_small_file(const std::string& path, std::string* out) {
#ifdef _WIN32
        HANDLE file = CreateFileW(utf8_to_wide(path).c_str(), GENERIC_READ,
                                  FILE_SHARE_READ | FILE_SHARE_WRITE | FILE_SHARE_DELETE,
                                  nullptr, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, nullptr);
        if (file == INVALID_HANDLE_VALUE) return false;
        char buffer[4096];
        DWORD got = 0;
        const BOOL ok = ReadFile(file, buffer, sizeof(buffer) - 1, &got, nullptr);
        CloseHandle(file);
        if (!ok) return false;
        out->assign(buffer, got);
        return true;
#else
        FILE* f = std::fopen(path.c_str(), "rb");
        if (!f) return false;
        char buffer[4096];
        const size_t got = std::fread(buffer, 1, sizeof(buffer) - 1, f);
        std::fclose(f);
        out->assign(buffer, got);
        return true;
#endif
    }

    /* Opens + maps the region file and validates its header against the
       handshake (magic, layout, size, epoch, poller mode). */
    static RegionFile open_region_file(const std::string& path, uint64_t size, uint32_t epoch) {
        RegionFile r;
#ifdef _WIN32
        const std::wstring wide = utf8_to_wide(path);
        HANDLE file = CreateFileW(wide.c_str(), GENERIC_READ | GENERIC_WRITE,
                                  FILE_SHARE_READ | FILE_SHARE_WRITE | FILE_SHARE_DELETE,
                                  nullptr, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, nullptr);
        if (file == INVALID_HANDLE_VALUE) return r;
        r.file_handle = file;
        LARGE_INTEGER li;
        li.QuadPart = static_cast<LONGLONG>(size);
        HANDLE mapping = CreateFileMappingW(file, nullptr, PAGE_READWRITE,
                                            li.HighPart, li.LowPart, nullptr);
        if (!mapping) { close_region(r); return RegionFile{}; }
        r.mapping_handle = mapping;
        void* view = MapViewOfFile(mapping, FILE_MAP_ALL_ACCESS, 0, 0,
                                   static_cast<SIZE_T>(size));
        if (!view) { close_region(r); return RegionFile{}; }
        r.map = view;
        r.size = static_cast<int64_t>(size);
#else
        const int fd = open(path.c_str(), O_RDWR);
        if (fd < 0) return r;
        struct stat st{};
        if (fstat(fd, &st) != 0 || st.st_size < static_cast<off_t>(size)) {
            close(fd);
            return RegionFile{};
        }
        void* map = mmap(nullptr, static_cast<size_t>(size), PROT_READ | PROT_WRITE,
                         MAP_SHARED, fd, 0);
        if (map == MAP_FAILED) { close(fd); return RegionFile{}; }
        r.map = map;
        r.fd = fd;
        r.size = static_cast<int64_t>(size);
#endif
        const uint8_t* base = static_cast<const uint8_t*>(r.map);
        uint64_t magic = 0, regionSize = 0, mode = 0;
        uint32_t version = 0, regionEpoch = 0;
        std::memcpy(&magic, base + JCM_SHM_HDR_MAGIC, sizeof(magic));
        std::memcpy(&version, base + JCM_SHM_HDR_VERSION, sizeof(version));
        std::memcpy(&regionSize, base + JCM_SHM_HDR_SIZE, sizeof(regionSize));
        std::memcpy(&regionEpoch, base + JCM_SHM_HDR_EPOCH, sizeof(regionEpoch));
        std::memcpy(&mode, base + JCM_SHM_HDR_MODE, sizeof(mode));
        if (magic != JCM_SHM_MAGIC || version != JCM_SHM_LAYOUT_VERSION
                || regionSize != size || regionEpoch != epoch || mode != 1) {
            close_region(r);
            return RegionFile{};
        }
        return r;
    }

    /* Scans the candidate dirs for the handshake of THIS process. Attaches
       when found and valid; returns true when attached. */
    bool try_scan_and_attach() {
        const long pid = current_pid();
        for (const std::string& dir : candidate_dirs()) {
            if (dir.empty()) continue;
            std::string sep;
#ifdef _WIN32
            if (dir.back() != '\\' && dir.back() != '/') sep = "\\";
#else
            if (dir.back() != '/') sep = "/";
#endif
            const std::string hs = dir + sep + "jcm_shm_hs_" + std::to_string(pid);
            std::string text;
            std::string path;
            uint64_t size = 0;
            uint32_t epoch = 0;
            if (!read_small_file(hs, &text)
                    || !parse_handshake(text, &path, &size, &epoch)) {
                continue;
            }
            RegionFile region = open_region_file(path, size, epoch);
            if (!region.map) continue;

            /* Publish readiness LAST: Java acquire-polls HDR_READY for its
               epoch and proceeds only after this store. */
            wr64(static_cast<uint8_t*>(region.map) + JCM_SHM_HDR_READY, epoch);
            attached_region_ = std::move(region);
            attached_epoch_ = epoch;
            svc_seq_ = 0;
            svc_depth_ = 0;
            for (int i = 0; i < JCM_CBQ_SLOTS; i++) cbq_served_[i] = 0;
            g_served_seq.store(0, std::memory_order_release);
            attached_.store(static_cast<uint8_t*>(attached_region_.map),
                            std::memory_order_release);
            std::fprintf(stderr,
                    "[jcm_native_bridge] poller attached to region %s (epoch %u, %lld KB)\n",
                    path.c_str(), epoch, static_cast<long long>(size / 1024));
            return true;
        }
        return false;
    }

    void detach() {
        uint8_t* base = attached_.load(std::memory_order_acquire);
        attached_.store(nullptr, std::memory_order_release);
        if (base) {
            close_region(attached_region_);
            std::fprintf(stderr, "[jcm_native_bridge] poller detached (epoch %u)\n",
                         attached_epoch_);
        }
        detach_requested_.store(false, std::memory_order_release);
    }

    bool revalidate(const uint8_t* base) const {
        uint64_t magic = 0, ready = 0;
        uint32_t epoch = 0;
        std::memcpy(&magic, base + JCM_SHM_HDR_MAGIC, sizeof(magic));
        std::memcpy(&epoch, base + JCM_SHM_HDR_EPOCH, sizeof(epoch));
        std::memcpy(&ready, base + JCM_SHM_HDR_READY, sizeof(ready));
        return magic == JCM_SHM_MAGIC && epoch == attached_epoch_
                && static_cast<uint32_t>(ready) == attached_epoch_;
    }

    /* ---- main loop ------------------------------------------------ */

    void run_inner() {
        long idleRounds = 0;
        for (;;) {
            uint8_t* base = attached_.load(std::memory_order_acquire);
            if (!base) {
                if (try_scan_and_attach()) {
                    idleRounds = 0;
                    continue;
                }
                /* Cheap presence probes; back off when nothing appears. */
                const int sleepMs = idleRounds < 40 ? 25 : 250;
                std::this_thread::sleep_for(std::chrono::milliseconds(sleepMs));
                idleRounds++;
                continue;
            }

            bool busy = false;
            const uint64_t seq = rd64(base + JCM_SHM_CONTROL_OFFSET + JCM_SHM_CTL_SEQ);
            const uint64_t served = g_served_seq.load(std::memory_order_acquire);
            if (seq != 0 && seq != served) {
                uint64_t expected = served;
                if (g_served_seq.compare_exchange_strong(expected, seq)) {
                    busy = true;
                    if (!revalidate(base)) {
                        g_served_seq.store(seq, std::memory_order_release);
                        detach();
                        continue;
                    }
                    const int32_t status = serve_request(base, seq);
                    if (status == JCM_SHM_ERR_DETACHED
                            || detach_requested_.load(std::memory_order_acquire)) {
                        detach();
                        continue;
                    }
                }
            }
            drain_cbq(base);

            if (attached_.load(std::memory_order_acquire) != base) continue;

            /* Adaptive wait: right after activity a short spin keeps frame
               latency at microseconds; a quiet region backs off so an idle
               world costs almost nothing. */
            if (busy) {
                idleRounds = 0;
            } else if ((++idleRounds & 3) != 0) {
                cpu_pause();
            } else {
                std::this_thread::sleep_for(std::chrono::microseconds(150));
            }
        }
    }

    std::atomic<uint8_t*> attached_{nullptr};
    std::atomic<bool> detach_requested_{false};
    RegionFile attached_region_{};
    uint32_t attached_epoch_ = 0;
    uint64_t svc_seq_ = 0;
    int svc_depth_ = 0;                    /* poller-thread nesting depth */
    uint64_t cbq_served_[JCM_CBQ_SLOTS] = {0, 0, 0, 0, 0, 0, 0, 0};
};

/* Meyers singleton. A function-local static is guarded (cxa_guard) and
 * is NEVER re-constructed by _GLOBAL__sub_I_* — which is what silently
 * zeroed the old namespace-scope object right after the constructor-
 * attribute boot had already written it (leaking its thread on glibc and
 * crashing with EINVAL on macOS). The first poller() call happens on the
 * booted poller thread itself, so construction order can never matter
 * again. */
Poller& poller() {
    static Poller instance;
    return instance;
}

/* ---- init-order-safe boot (C primitives ONLY) ---------------------- */
/*
 * Runs from the library constructor / DllMain — a context in which every
 * C++ static object of this translation unit is still unconstructed (the
 * constructor attribute is ordered BEFORE _GLOBAL__sub_I_*), so it must
 * not touch any of them. pthread_once_t / INIT_ONCE are plain C objects
 * with static initializers (all-zero) that no C++ constructor ever
 * re-writes, and pthread_create / CreateThread create the poller thread
 * WITHOUT any std::thread bookkeeping. The thread itself enters the
 * Meyers singleton above, so every C++ object it uses is constructed
 * on demand, strictly after all static initialization. */
#if defined(_WIN32)
static INIT_ONCE g_poller_boot_once = INIT_ONCE_STATIC_INIT;
static DWORD WINAPI poller_thread_entry(LPVOID) {
    poller().run_outer();
    return 0;
}
static BOOL CALLBACK poller_boot_impl(PINIT_ONCE, PVOID, PVOID*) {
    /* Create the thread WITHOUT waiting for it — the documented
       loader-lock contract for DllMain. Closing the handle makes it fully
       detached: it dies with the process, exactly like the poller loop's
       own lifecycle (the shutdown command detaches the region, never the
       thread). */
    HANDLE thread = CreateThreadW(nullptr, 0, poller_thread_entry, nullptr, 0, nullptr);
    if (thread) CloseHandle(thread);
    return TRUE;
}
#else
static pthread_once_t g_poller_boot_once = PTHREAD_ONCE_INIT;
static void* poller_thread_entry(void*) {
    poller().run_outer();
    return nullptr;
}
static void poller_boot_impl(void) {
    pthread_attr_t attr;
    pthread_attr_init(&attr);
    pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);
    pthread_t tid;
    const int rc = pthread_create(&tid, &attr, poller_thread_entry, nullptr);
    (void)rc;   /* a failed create leaves the plane degraded, never aborts */
    pthread_attr_destroy(&attr);
}
#endif

/** Idempotent, init-order-safe poller boot. Safe to call from ANY
 *  context — module initializers, DllMain, runtime entry points. */
inline void poller_boot() {
#if defined(_WIN32)
    InitOnceExecuteOnce(&g_poller_boot_once, poller_boot_impl, nullptr, nullptr);
#else
    pthread_once(&g_poller_boot_once, poller_boot_impl);
#endif
}

/* ------------------------------------------------------------------ */
/* host services handed to the script module (see mtr_native.h)        */
/*                                                                     */
/* Dispatch order per service: FFM vtable (JDK 22+ plane) FIRST, the   */
/* shared-memory SVC request (poller plane) SECOND, graceful no-host    */
/* degradation LAST (headless tests keep placeholder handles).         */
/* ------------------------------------------------------------------ */

int32_t host_acquire_model(void*, const char* path) {
    if (!path) return -1;
    if (ffm_has(g_ffm_host.acquire_model)) {
        const int64_t handle = g_ffm_host.acquire_model(path,
                static_cast<int32_t>(std::strlen(path)));
        return handle < 0 ? -1 : static_cast<int32_t>(handle) | 0x40000000;
    }
    if (poller().svc_available()) {
        const uint64_t words[2] = {
            reinterpret_cast<uintptr_t>(path),
            std::strlen(path),
        };
        uint64_t ret = 0;
        if (poller().svc_call(JCM_SVC_ACQUIRE_MODEL, words, 2, &ret)) {
            return static_cast<int32_t>(ret) < 0 ? -1
                    : (static_cast<int32_t>(ret) | 0x40000000);
        }
    }
    return -1;
}

void host_release_model(void* user, int32_t slot) {
    if (slot >= 0 && (slot & 0x40000000)) {
        if (ffm_has(g_ffm_host.release_model)) {
            g_ffm_host.release_model(slot & ~0x40000000);
            return;
        }
        if (poller().svc_available()) {
            const uint64_t words[1] = { static_cast<uint64_t>(
                    static_cast<uint32_t>(slot & ~0x40000000)) };
            poller().svc_call(JCM_SVC_RELEASE_MODEL, words, 1, nullptr);
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
       after Java has built the resources (see resolve_captured_resources). */
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
    if (!pixels || len <= 0 || out_w <= 0 || out_h <= 0) return -1;
    if (ffm_has(g_ffm_host.rasterize_text)) {
        return g_ffm_host.rasterize_text(utf8, len, x, y, max_w,
                                        static_cast<int32_t>(r), static_cast<int32_t>(g),
                                        static_cast<int32_t>(b), pixels, out_w, out_h);
    }
    if (poller().svc_available()) {
        const uint64_t words[11] = {
            reinterpret_cast<uintptr_t>(utf8), static_cast<uint32_t>(len),
            static_cast<uint32_t>(x), static_cast<uint32_t>(y),
            static_cast<uint32_t>(max_w), static_cast<uint32_t>(r),
            static_cast<uint32_t>(g), static_cast<uint32_t>(b),
            reinterpret_cast<uintptr_t>(pixels),
            static_cast<uint32_t>(out_w), static_cast<uint32_t>(out_h),
        };
        uint64_t ret = 0;
        if (poller().svc_call(JCM_SVC_RASTERIZE_TEXT, words, 11, &ret)) {
            return static_cast<int32_t>(ret);
        }
    }
    return -1;
}

void host_log(void*, int32_t level, const char* utf8, int32_t len) {
    std::fprintf(stderr, "[native-script/%d] %.*s\n", level, len, utf8);
}

int32_t host_measure_font_text(void*, const char* font, const char* utf8, int32_t len,
                                double size, double sx, double sy, JcmFontMetrics* out) {
    if (!font || !utf8 || len < 0 || !out) return -1;
    if (ffm_has(g_ffm_host.measure_font_text)) {
        double metrics[4] = {0, 0, 0, 0};
        const int32_t status = g_ffm_host.measure_font_text(font,
                static_cast<int32_t>(std::strlen(font)), utf8, len, size, sx, sy, metrics);
        if (status == 0) { out->width = metrics[0]; out->ascent = metrics[1];
                           out->descent = metrics[2]; out->height = metrics[3]; }
        return status;
    }
    if (poller().svc_available()) {
        uint64_t words[8] = {
            reinterpret_cast<uintptr_t>(font), std::strlen(font),
            reinterpret_cast<uintptr_t>(utf8), static_cast<uint32_t>(len),
            0, 0, 0,
            reinterpret_cast<uintptr_t>(out),
        };
        std::memcpy(&words[4], &size, sizeof(double));
        std::memcpy(&words[5], &sx, sizeof(double));
        std::memcpy(&words[6], &sy, sizeof(double));
        uint64_t ret = 0;
        if (poller().svc_call(JCM_SVC_MEASURE_FONT_TEXT, words, 8, &ret)) {
            return static_cast<int32_t>(ret);
        }
    }
    return -1;
}

int32_t host_draw_font_text(void*, const char* font, const char* utf8, int32_t len,
                             double size, double x, double baseline, double sx, double sy,
                             double compression, uint32_t argb, int32_t clip_x0, int32_t clip_y0,
                             int32_t clip_x1, int32_t clip_y1, uint8_t* pixels,
                             int32_t width, int32_t height, int32_t* bounds) {
    if (!font || !utf8 || len < 0 || !pixels || width <= 0 || height <= 0 || !bounds) return -1;
    if (ffm_has(g_ffm_host.draw_font_text)) {
        return g_ffm_host.draw_font_text(font, static_cast<int32_t>(std::strlen(font)),
                utf8, len, size, x, baseline, sx, sy, compression, argb,
                clip_x0, clip_y0, clip_x1, clip_y1, pixels, width, height, bounds);
    }
    if (poller().svc_available()) {
        uint64_t words[19] = {
            reinterpret_cast<uintptr_t>(font), std::strlen(font),
            reinterpret_cast<uintptr_t>(utf8), static_cast<uint32_t>(len),
            0, 0, 0, 0, 0, 0,
            static_cast<uint64_t>(argb),
            static_cast<uint32_t>(clip_x0), static_cast<uint32_t>(clip_y0),
            static_cast<uint32_t>(clip_x1), static_cast<uint32_t>(clip_y1),
            reinterpret_cast<uintptr_t>(pixels),
            static_cast<uint32_t>(width), static_cast<uint32_t>(height),
            reinterpret_cast<uintptr_t>(bounds),
        };
        std::memcpy(&words[4], &size, sizeof(double));
        std::memcpy(&words[5], &x, sizeof(double));
        std::memcpy(&words[6], &baseline, sizeof(double));
        std::memcpy(&words[7], &sx, sizeof(double));
        std::memcpy(&words[8], &sy, sizeof(double));
        std::memcpy(&words[9], &compression, sizeof(double));
        uint64_t ret = 0;
        if (poller().svc_call(JCM_SVC_DRAW_FONT_TEXT, words, 19, &ret)) {
            return static_cast<int32_t>(ret);
        }
    }
    return -1;
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
    /* Release an oversized reply left over from a PREVIOUS call before any
       new reply is built: the transient memory goes back to the allocator
       (C++-mode hygiene) instead of being pinned until thread exit — and
       via the background reclaimer, so even a multi-MB reply's free never
       lands on a JVM-owned thread. */
    if (result.capacity() > (1u << 20)) {
        std::string dead;
        dead.swap(result);
        g_reclaimer.defer_str(std::move(dead));
    }
    auto& inst = *static_cast<Instance*>(user);
    if (result_length) *result_length = 0;
    if (ffm_has(g_ffm_host.invoke_api) && result_length) {
        /* FFM upcall: Java owns a per-thread REUSED reply buffer, so the
           returned address stays valid until the next invoke_api entry on
           this thread — the copy below completes the exchange. */
        int32_t reply_len = -1;
        const int64_t reply_addr = g_ffm_host.invoke_api(inst.api_key.data(),
                static_cast<int32_t>(inst.api_key.size()), request, length, &reply_len);
        if (reply_addr == 0 || reply_len < 0) {
            result = "{\"error\":\"Java native API invocation failed\"}";
        } else {
            result.assign(static_cast<size_t>(reply_len), '\0');
            std::memcpy(&result[0], reinterpret_cast<const void*>(
                    static_cast<uintptr_t>(reply_addr)), static_cast<size_t>(reply_len));
        }
        *result_length = static_cast<int32_t>(result.size());
        return result.data();
    }
    if (poller().svc_available() && result_length) {
        /* SVC round-trip: word 4 is the address of a local length slot the
           Java adapter writes through (shared address space), and the RET
           word carries the reply ADDRESS — Java's thread-local reused
           reply buffer, read out synchronously below. */
        int32_t reply_len = -1;
        const uint64_t words[5] = {
            reinterpret_cast<uintptr_t>(inst.api_key.data()),
            inst.api_key.size(),
            reinterpret_cast<uintptr_t>(request),
            static_cast<uint32_t>(length > 0 ? length : 0),
            reinterpret_cast<uintptr_t>(&reply_len),
        };
        uint64_t reply_addr = 0;
        const bool answered = poller().svc_call(JCM_SVC_INVOKE_API, words, 5, &reply_addr);
        if (!answered || reply_addr == 0 || reply_len < 0) {
            result = "{\"error\":\"Java native API invocation failed\"}";
        } else {
            result.assign(static_cast<size_t>(reply_len), '\0');
            std::memcpy(&result[0], reinterpret_cast<const void*>(
                    static_cast<uintptr_t>(reply_addr)), static_cast<size_t>(reply_len));
        }
        *result_length = static_cast<int32_t>(result.size());
        return result.data();
    }
    result = "{\"error\":\"native API bridge is not installed\"}";
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
    host.defer_free = host_defer_free;
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

/* ------------------------------------------------------------------ */
/* v4/v5: turn the resources create()/render() asked for into real host objects */
/*                                                                     */
/* Runs right after mtrCreate, on whichever thread serves the render   */
/* (an FFM-plane Java thread / the poller thread), while the driving    */
/* Java thread is blocked waiting for the frame.                        */
/* ------------------------------------------------------------------ */

bool resolve_captured_resources(Instance& inst) {
    if (!inst.resources_dirty) return true;
    if (inst.tex_w.empty() && inst.quad_verts.empty()) {
        inst.resources_ready = true;
        inst.resources_dirty = false;
        return true;   /* nothing to build (pure-record script) */
    }
    const int32_t texCount = static_cast<int32_t>(inst.tex_w.size());
    const int32_t quadCount = static_cast<int32_t>(inst.quad_slot.size());
    if (ffm_has(g_ffm_host.create_resources)) {
        /* JNI-free upcall. Inputs are read in place from the bridge's own
           vectors (this thread is blocked inside the call); the Java side
           writes the real handles into out_handles before returning. */
        std::vector<int32_t> out(static_cast<size_t>(texCount + quadCount), -1);
        const int32_t status = g_ffm_host.create_resources(
                texCount, quadCount,
                inst.tex_w.data(), inst.tex_h.data(),
                inst.quad_verts.data(), inst.quad_uv.data(),
                inst.quad_slot.data(), inst.quad_stage.data(), out.data());
        if (status != 0) {
            std::fprintf(stderr, "[jcm_native_bridge] host createResources failed "
                                 "(%d textures / %d quads)\n", texCount, quadCount);
            return false;
        }
        inst.tex_slot_handles.assign(out.begin(), out.begin() + texCount);
        inst.model_slot_handles.assign(out.begin() + texCount, out.end());
        inst.resources_ready = true;
        inst.resources_dirty = false;
        return true;
    }
    if (poller().svc_available()) {
        std::vector<int32_t> out(static_cast<size_t>(texCount + quadCount), -1);
        const uint64_t words[9] = {
            static_cast<uint32_t>(texCount), static_cast<uint32_t>(quadCount),
            reinterpret_cast<uintptr_t>(inst.tex_w.data()),
            reinterpret_cast<uintptr_t>(inst.tex_h.data()),
            reinterpret_cast<uintptr_t>(inst.quad_verts.data()),
            reinterpret_cast<uintptr_t>(inst.quad_uv.data()),
            reinterpret_cast<uintptr_t>(inst.quad_slot.data()),
            reinterpret_cast<uintptr_t>(inst.quad_stage.data()),
            reinterpret_cast<uintptr_t>(out.data()),
        };
        uint64_t status = 0;
        const bool answered = poller().svc_call(JCM_SVC_CREATE_RESOURCES, words, 9, &status);
        if (!answered || static_cast<int32_t>(status) != 0) {
            std::fprintf(stderr, "[jcm_native_bridge] host createResources failed "
                                 "(%d textures / %d quads)\n", texCount, quadCount);
            return false;
        }
        inst.tex_slot_handles.assign(out.begin(), out.begin() + texCount);
        inst.model_slot_handles.assign(out.begin() + texCount, out.end());
        inst.resources_ready = true;
        inst.resources_dirty = false;
        return true;
    }
    /* No host on either plane: keep the placeholder handles — the replay
       step drops those records, the script still runs end-to-end. */
    inst.resources_ready = false;
    return true;
}

/** Rewrite upload placeholders (slot index) -> real GraphicsTexture id,
    and drop model draws whose quad has no host model. Mutates the frame
    records in place; the module owns the arena until the next render. */
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
       Validate every step against out.records_len before touching memory. */
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

/** Module-owned zeroed fallback for dispose-without-live-buffer: grown to
    the largest snapshot this module has consumed, reused by every instance
    of the module (callers hold mod->mutex). Deterministic C++ lifetime —
    created lazily, freed with the module. A zeroed block reads as a valid
    ABI struct with zero counts ("empty" train / PIDS / lift). */
const void* fallback_snapshot(BridgeModule& mod, const Instance& inst) {
    /* 256 B floor so scalar/header reads stay in bounds even when an
       instance never recorded a length (defensive: uninitialized instances
       skip mtrDispose anyway). */
    const int64_t want = inst.snapshot_len > 256 ? inst.snapshot_len : 256;
    if (static_cast<int64_t>(mod.zero_snapshot.size()) < want) {
        mod.zero_snapshot.assign(static_cast<size_t>(want), 0);
    }
    return mod.zero_snapshot.data();
}

BridgeModule* find_module(uint64_t handle) {
    std::lock_guard<std::mutex> lk(g_modules_mutex);
    const auto it = g_modules.find(static_cast<int64_t>(handle));
    return it != g_modules.end() ? it->second : nullptr;
}

/* ------------------------------------------------------------------ */
/* module lifecycle (shared by the FFM exports and the poller commands) */
/* ------------------------------------------------------------------ */

/** Shared dlopen + symbol resolution + registry entry for one module
 *  library (the FFM jcm_module_open and the poller CMD_OPEN both land
 *  here). Returns null on failure (reason already logged).
 *
 *  ABI 10 keep-loaded policy: successfully opened DSOs are NEVER dlclosed.
 *  The bridge keeps the dlopen refcount for the process lifetime and only
 *  deletes the per-module bookkeeping (BridgeModule) on close. The JVM
 *  itself never dlclose's its own libraries for the same reason: the
 *  dynamic loader's scope bookkeeping is a process-wide lazily-rebuilt
 *  structure, and unload/reload cycles under a JVM can leave a reopened
 *  library's scope lists in a state its own dlsym walk faults on.
 *  Keeping the mapping also makes F3+T resource-pack reloads cheaper
 *  (no relocations) and cannot leak unboundedly: the number of distinct
 *  module files per session is small, and re-opens return the SAME live
 *  mapping instead of re-mapping. */
BridgeModule* open_module(const char* path) {
    void* lib = load_library(path);
    if (!lib) {
        std::fprintf(stderr, "[jcm_native_bridge] dlopen failed: %s (%s)\n",
                     path, last_error());
        return nullptr;
    }
    auto* mod = new (std::nothrow) BridgeModule();
    if (!mod) {
        std::fprintf(stderr, "[jcm_native_bridge] out of memory loading %s\n", path);
#if defined(_WIN32)
        FreeLibrary(static_cast<HMODULE>(lib));
#else
        dlclose(lib);
#endif
        return nullptr;
    }
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
        /* Failure path (no keep-loaded registration happened yet): actually
           unload the dlopen'd library so a bad module file cannot leak a
           mapping per reload attempt. */
        if (mod->dl) {
#if defined(_WIN32)
            FreeLibrary(static_cast<HMODULE>(mod->dl));
#else
            dlclose(mod->dl);
#endif
            mod->dl = nullptr;
        }
        delete mod;
        return nullptr;
    }
    /* Register for the request servers, which learn the module from the
       CONTROL SLOT instead of an argument (shm_channel.h). */
    {
        std::lock_guard<std::mutex> lk(g_modules_mutex);
        g_modules[reinterpret_cast<int64_t>(mod)] = mod;
    }
    return mod;
}

/* ------------------------------------------------------------------ */
/* per-frame pipeline (identical on both planes)                        */
/* ------------------------------------------------------------------ */

/* Runs the per-frame pipeline for (module, instance key, snapshot) while
   the caller holds mod->mutex: lazy instance lookup, ABI-6 state init,
   mtrCreate on first use, mtrRender, host resource resolution and
   record-handle patching.

   On JCM_SHM_OK, `out` holds the module-owned, PATCHED arena descriptors
   (record_count / records_len adjusted) and stays valid until the next
   render on this module. Any failure marks the instance failed. */
int32_t run_frame_locked(BridgeModule& mod, const char* key,
                         const void* snapshot, int64_t snapshotLen,
                         int32_t kind, JcmFrameOutput& out) {
    Instance& inst = mod.instances[key];
    /* Assign only on change: instance keys are stable for the whole life of
       an instance, and they exceed SSO length, so an unconditional assign
       is a hidden malloc + copy on EVERY frame of EVERY instance. */
    if (inst.api_key != key) inst.api_key = key;
    inst.resource_kind = kind;
    if (inst.failed) return JCM_SHM_ERR_CREATE;

    if (inst.state.empty() && mod.state_size() > 0) {
        inst.state.assign(mod.state_size(), 0);
    }
    /* No snapshot retention: the live buffer is read in place (the Java
       driver is blocked waiting for the result), and dispose without a
       live buffer receives the shared zeroed fallback. */
    inst.snapshot_len = snapshotLen;

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
           libstdc++ that is an immediate null dereference. */
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
    if (!resolve_captured_resources(inst)) return JCM_SHM_ERR_RESOURCES;

    int32_t keptRecords = out.record_count;
    int32_t keptLen = 0;
    patch_frame_handles(inst, out, &keptRecords, &keptLen);
    out.record_count = keptRecords;
    out.records_len = keptLen;
    return JCM_SHM_OK;
}

/* ------------------------------------------------------------------ */
/* dispose + close (shared cores)                                       */
/* ------------------------------------------------------------------ */

/** Shared dispose pipeline (the FFM downcall and the poller command both
 *  land here). `live_snapshot` is the LIVE driver buffer when available,
 *  else null (the module's zeroed fallback snapshot substitutes — the
 *  bridge never retained a per-instance copy). */
void dispose_instance_core(BridgeModule* mod, const char* key,
                           const void* live_snapshot, int64_t live_len) {
    if (!mod || !key) return;
    std::lock_guard<std::mutex> lock(mod->mutex);
    auto it = mod->instances.find(key);
    if (it == mod->instances.end()) return;

    const void* live = live_len >= 0 ? live_snapshot : nullptr;

    if (it->second.initialized) {
        JcmHostServices host;
        fill_host(host, it->second);
        JcmFrameInput in{};
        in.abi_version = mod->abi();
        in.resource_kind = it->second.resource_kind;
        if (live) {
            in.snapshot = live;
        } else {
            /* No live buffer: the module-owned zeroed fallback replaces the
               old per-instance retained copy (stale first-frame data). */
            in.snapshot = fallback_snapshot(*mod, it->second);
        }
        in.state = it->second.state.data();
        in.state_size = it->second.state.size();
        in.host = &host;
        mod->dispose(&in);
    }
    /* Capture the key BEFORE the instance map entry dies: the shm id scan
       below compares against it (erase invalidates the iterator). */
    const std::string disposed_api_key = it->second.api_key;

    /* C++-mode reclamation: mtrDispose above already ran the module's State
       destructor, so every remaining payload here is TRIVIALLY destructible
       raw memory. Handing it to the background reclaimer keeps those frees
       — potentially hundreds of KB per instance — off the frame. */
    g_reclaimer.defer_bytes(std::move(it->second.state));
    g_reclaimer.defer_str(std::move(it->second.api_key));
    g_reclaimer.defer_container(std::move(it->second.tex_slot_handles));
    g_reclaimer.defer_container(std::move(it->second.model_slot_handles));
    g_reclaimer.defer_container(std::move(it->second.tex_w));
    g_reclaimer.defer_container(std::move(it->second.tex_h));
    g_reclaimer.defer_container(std::move(it->second.quad_verts));
    g_reclaimer.defer_container(std::move(it->second.quad_uv));
    g_reclaimer.defer_container(std::move(it->second.quad_slot));
    g_reclaimer.defer_container(std::move(it->second.quad_stage));

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

/** Shared close pipeline (the FFM jcm_module_close and the poller
 *  CMD_CLOSE): dispose every instance against the zeroed fallback, drop
 *  the shm id table, defer the payloads to the background reclaimer,
 *  unregister, and delete the bookkeeping — the DSO mapping deliberately
 *  stays loaded (ABI 10 keep-loaded policy). */
void close_module_core(BridgeModule* mod) {
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
            /* Module teardown has no live buffers anymore: every instance
               disposes against the shared zeroed fallback snapshot. */
            in.snapshot = fallback_snapshot(*mod, kv.second);
            in.state = kv.second.state.data();
            in.state_size = kv.second.state.size();
            in.host = &host;
            mod->dispose(&in);
        }
        mod->shm_ids.clear();
        for (auto& kv : mod->instances) {
            Instance& dead = kv.second;
            g_reclaimer.defer_bytes(std::move(dead.state));
            g_reclaimer.defer_str(std::move(dead.api_key));
            g_reclaimer.defer_container(std::move(dead.tex_slot_handles));
            g_reclaimer.defer_container(std::move(dead.model_slot_handles));
            g_reclaimer.defer_container(std::move(dead.tex_w));
            g_reclaimer.defer_container(std::move(dead.tex_h));
            g_reclaimer.defer_container(std::move(dead.quad_verts));
            g_reclaimer.defer_container(std::move(dead.quad_uv));
            g_reclaimer.defer_container(std::move(dead.quad_slot));
            g_reclaimer.defer_container(std::move(dead.quad_stage));
        }
        mod->instances.clear();
        g_reclaimer.defer_bytes(std::move(mod->zero_snapshot));
    }
    {
        std::lock_guard<std::mutex> lk(g_modules_mutex);
        g_modules.erase(reinterpret_cast<int64_t>(mod));
    }
    /* Bookkeeping deleted; the DSO mapping deliberately STAYS LOADED (ABI 10
       keep-loaded policy — see open_module). */
    delete mod;
}

/* ------------------------------------------------------------------ */
/* request service (shared by the FFM kick and the poller thread)        */
/* ------------------------------------------------------------------ */

/** Serves one request (per-frame render or lifecycle command) off the
 *  control slot of ANY attached mapping and publishes the result slot.
 *  The seq echo is written LAST (see shm_channel.h). Returns
 *  JCM_SHM_ERR_DETACHED after the shutdown command. */
int32_t serve_request(uint8_t* base, uint64_t seq) {
    const uint8_t* ctl = base + JCM_SHM_CONTROL_OFFSET;
    uint8_t* res = base + JCM_SHM_RESULT_OFFSET;
    const uint64_t handle = rd64(ctl + JCM_SHM_CTL_MODULE);
    const uint64_t instanceId = rd64(ctl + JCM_SHM_CTL_INSTANCE);
    const uint32_t kind = rd32(ctl + JCM_SHM_CTL_KIND);
    const uint64_t snapAddr = rd64(ctl + JCM_SHM_CTL_SNAP_ADDR);
    /* 32-bit control field (see shm_channel.h): a full-word read would
       swallow the low half of SNAP_ADDR as the length's high bytes and
       produce a multi-terabyte "length" — exactly the dispose-time
       bad_alloc the first full-harness run surfaced. */
    const int64_t snapLen = static_cast<int32_t>(rd32(ctl + JCM_SHM_CTL_SNAP_LEN));
    const uint64_t argAddr = rd64(ctl + JCM_SHM_CTL_ARG_ADDR);
    const uint64_t argLen = rd64(ctl + JCM_SHM_CTL_ARG_LEN);
    const uint64_t rplyAddr = rd64(ctl + JCM_SHM_CTL_RPLY_ADDR);
    const uint64_t rplyCap = rd64(ctl + JCM_SHM_CTL_RPLY_LEN);

    auto publish = [&](int32_t status, uint64_t extra) {
        write_i32(res + JCM_SHM_RES_STATUS, status);
        write_u64(res + JCM_SHM_RES_EXTRA, extra);
        write_u64(res + JCM_SHM_RES_SEQ, seq);   /* echo LAST */
    };

    /* ---- lifecycle commands ---- */
    if (kind >= JCM_CMD_BASE) {
        const uint8_t* args = argAddr
                ? reinterpret_cast<const uint8_t*>(static_cast<uintptr_t>(argAddr)) : nullptr;
        switch (kind) {
        case JCM_CMD_OPEN: {
            if (!args || argLen == 0 || argLen > (1u << 20)) {
                publish(JCM_SHM_ERR_REQUEST, 0);
                return JCM_SHM_ERR_REQUEST;
            }
            const std::string path(reinterpret_cast<const char*>(args),
                                   static_cast<size_t>(argLen));
            BridgeModule* mod = open_module(path.c_str());
            publish(mod ? JCM_SHM_OK : JCM_SHM_ERR_MODULE,
                    reinterpret_cast<uint64_t>(mod));
            return mod ? JCM_SHM_OK : JCM_SHM_ERR_MODULE;
        }
        case JCM_CMD_CLOSE: {
            close_module_core(find_module(handle));
            publish(JCM_SHM_OK, 0);
            return JCM_SHM_OK;
        }
        case JCM_CMD_ABI: {
            BridgeModule* mod = find_module(handle);
            const uint32_t abi = (mod && mod->abi) ? mod->abi() : 0;
            publish(JCM_SHM_OK, abi);
            return JCM_SHM_OK;
        }
        case JCM_CMD_SCRIPT_TYPE:
        case JCM_CMD_SCRIPT_ID: {
            BridgeModule* mod = find_module(handle);
            const char* (*fn)(void) = nullptr;
            if (mod) fn = (kind == JCM_CMD_SCRIPT_TYPE) ? mod->type : mod->id;
            const char* s = fn ? fn() : nullptr;
            if (!s) {
                publish(JCM_SHM_ERR_MODULE, 0);
                return JCM_SHM_ERR_MODULE;
            }
            const size_t n = std::strlen(s);
            if (!rplyAddr || rplyCap < n) {
                publish(JCM_SHM_ERR_OVERFLOW, 0);
                return JCM_SHM_ERR_OVERFLOW;
            }
            std::memcpy(reinterpret_cast<void*>(static_cast<uintptr_t>(rplyAddr)), s, n);
            publish(JCM_SHM_OK, n);
            return JCM_SHM_OK;
        }
        case JCM_CMD_STATE_SIZE: {
            BridgeModule* mod = find_module(handle);
            const uint64_t size = (mod && mod->state_size)
                    ? static_cast<uint64_t>(mod->state_size()) : 0;
            publish(JCM_SHM_OK, size);
            return JCM_SHM_OK;
        }
        case JCM_CMD_DISPOSE: {
            BridgeModule* mod = find_module(handle);
            if (!mod || !args || argLen < 16) {
                publish(JCM_SHM_ERR_REQUEST, 0);
                return JCM_SHM_ERR_REQUEST;
            }
            uint64_t snapAddress = 0, snapSize = 0;
            std::memcpy(&snapAddress, args, sizeof(snapAddress));
            std::memcpy(&snapSize, args + 8, sizeof(snapSize));
            const std::string key(reinterpret_cast<const char*>(args + 16),
                                  static_cast<size_t>(argLen - 16));
            dispose_instance_core(mod, key.c_str(),
                    snapAddress ? reinterpret_cast<const void*>(
                            static_cast<uintptr_t>(snapAddress)) : nullptr,
                    snapAddress ? static_cast<int64_t>(snapSize) : -1);
            publish(JCM_SHM_OK, 0);
            return JCM_SHM_OK;
        }
        case JCM_CMD_REGISTER: {
            BridgeModule* mod = find_module(handle);
            if (!mod || !args || argLen < 8) {
                publish(JCM_SHM_ERR_REQUEST, 0);
                return JCM_SHM_ERR_REQUEST;
            }
            uint64_t id = 0;
            std::memcpy(&id, args, sizeof(id));
            const std::string key(reinterpret_cast<const char*>(args + 8),
                                  static_cast<size_t>(argLen - 8));
            std::lock_guard<std::mutex> lock(mod->mutex);
            mod->shm_ids[static_cast<int64_t>(id)] = key;
            publish(JCM_SHM_OK, 0);
            return JCM_SHM_OK;
        }
        case JCM_CMD_RECLAIMER: {
            /* Diagnostics: drain the deferred-reclamation queue and report
               the (now zero) pending count — the poller-plane twin of the
               jcm_reclaimer_quiesce/pending exports. */
            g_reclaimer.quiesce();
            publish(JCM_SHM_OK, static_cast<uint64_t>(g_reclaimer.pending()));
            return JCM_SHM_OK;
        }
        case JCM_CMD_SHUTDOWN: {
            publish(JCM_SHM_OK, 0);
            /* Poison the region BEFORE the answer is even visible: between
               this answer and Java's file cleanup there is a window where
               the scan can re-find the stale handshake, and re-attaching
               would re-serve this very shutdown forever (the first bring-up
               looped exactly there). MODE 0 permanently disqualifies the
               region for attachment, so the poller idles on its cheap scan
               until the next (genuine) handshake appears. */
            wr64(base + JCM_SHM_HDR_MODE, 0);
            poller().request_detach();
            return JCM_SHM_ERR_DETACHED;
        }
        default:
            publish(JCM_SHM_ERR_REQUEST, 0);
            return JCM_SHM_ERR_REQUEST;
        }
    }

    /* ---- per-frame render ---- */
    if (seq == 0 || kind > MTR_RESOURCE_LIFT) {
        std::fprintf(stderr, "[jcm_native_bridge] shm request: bad request (seq %llu, kind %u)\n",
                     (unsigned long long)seq, kind);
        publish(JCM_SHM_ERR_REQUEST, 0);
        return JCM_SHM_ERR_REQUEST;
    }
    if (!snapAddr || snapLen == 0) {
        std::fprintf(stderr, "[jcm_native_bridge] shm request: invalid snapshot (addr %llu, len %llu)\n",
                     (unsigned long long)snapAddr, (unsigned long long)snapLen);
        publish(JCM_SHM_ERR_SNAPSHOT, 0);
        return JCM_SHM_ERR_SNAPSHOT;
    }
    BridgeModule* mod = find_module(handle);
    if (!mod) {
        std::fprintf(stderr, "[jcm_native_bridge] shm request: module handle %llu not registered (%zu modules)\n",
                     (unsigned long long)handle, g_modules.size());
        publish(JCM_SHM_ERR_MODULE, 0);
        return JCM_SHM_ERR_MODULE;
    }
    std::lock_guard<std::mutex> lock(mod->mutex);
    const auto idIt = mod->shm_ids.find(static_cast<int64_t>(instanceId));
    if (idIt == mod->shm_ids.end()) {
        std::fprintf(stderr, "[jcm_native_bridge] shm request: instance id %llu not registered (%zu ids)\n",
                     (unsigned long long)instanceId, mod->shm_ids.size());
        publish(JCM_SHM_ERR_INSTANCE, 0);
        return JCM_SHM_ERR_INSTANCE;
    }
    JcmFrameOutput out{};
    const int32_t status = run_frame_locked(*mod, idIt->second.c_str(),
            reinterpret_cast<const void*>(static_cast<uintptr_t>(snapAddr)),
            static_cast<int64_t>(snapLen), static_cast<int32_t>(kind), out);
    if (status != 0) {
        publish(status, 0);
        return status;
    }
    /* Publish the module-owned arena descriptors; Java wraps them into
       REUSED NIO ByteBuffers (NativeArenaView) — zero crossing objects. */
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
    write_i32(res + JCM_SHM_RES_COUNT, out.record_count);
    for (int i = 0; i < 5; i++) {
        std::memcpy(res + arenaOffsets[i], &arenas[i], sizeof(ArenaDesc));
    }
    publish(JCM_SHM_OK, 0);
    return JCM_SHM_OK;
}

/* ------------------------------------------------------------------ */
/* shared-memory region cores (FFM jcm_shm_* and the poller share these) */
/* ------------------------------------------------------------------ */

int32_t shm_init_core(int64_t base, int64_t size) {
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
    g_served_seq.store(0, std::memory_order_release);
    std::fprintf(stderr, "[jcm_native_bridge] shm init: base %llu size %llu\n",
                 (unsigned long long)base, (unsigned long long)size);
    return JCM_SHM_OK;
}

/* Serves one request from the FFM-plane mapping (jcm_shm_kick downcall,
   executed synchronously on the calling Java thread). Claim discipline
   is shared with the poller: exactly-once per request seq. */
int32_t shm_kick_core() {
    uint8_t* base = g_shm.base;
    if (!base) return JCM_SHM_ERR_NOT_INIT;
    const uint64_t seq = rd64(base + JCM_SHM_CONTROL_OFFSET + JCM_SHM_CTL_SEQ);
    if (seq == 0) return JCM_SHM_ERR_REQUEST;
    const uint64_t served = g_served_seq.load(std::memory_order_acquire);
    if (seq == served) return JCM_SHM_ERR_REQUEST;   /* stale re-kick */
    uint64_t expected = served;
    if (!g_served_seq.compare_exchange_strong(expected, seq)) {
        /* The poller claimed this request (not expected on the FFM plane,
           but never double-serve): it will publish the result. */
        return JCM_SHM_ERR_REQUEST;
    }
    return serve_request(base, seq);
}

} /* anonymous namespace */

/* ------------------------------------------------------------------ */
/* plain-C jcm_* export surface (the FFM plane; called via
   java.lang.foreign downcalls on JDK 22+, or linked into the headless
   native tests directly)
 * ------------------------------------------------------------------ */
#if defined(_WIN32)
#  define JCM_BRIDGE_EXPORT extern "C" __declspec(dllexport)
#else
#  define JCM_BRIDGE_EXPORT extern "C" __attribute__((visibility("default")))
#endif

/* ---- module lifecycle -------------------------------------------------- */

JCM_BRIDGE_EXPORT int64_t jcm_module_open(const char* path, int32_t path_len) {
    if (!path || path_len <= 0) return 0;
    const std::string owned(path, static_cast<size_t>(path_len));
    BridgeModule* mod = open_module(owned.c_str());
    return reinterpret_cast<int64_t>(mod);
}

JCM_BRIDGE_EXPORT void jcm_module_close(int64_t handle) {
    close_module_core(reinterpret_cast<BridgeModule*>(static_cast<uintptr_t>(handle)));
}

JCM_BRIDGE_EXPORT int32_t jcm_module_abi(int64_t handle) {
    auto* mod = reinterpret_cast<BridgeModule*>(static_cast<uintptr_t>(handle));
    return (mod && mod->abi) ? static_cast<int32_t>(mod->abi()) : 0;
}

/** Writes the module's NUL-terminated identity string (script type or
 *  script id) into the caller-provided buffer; returns the written byte
 *  length, or -1 on a bad argument. Caller-buffer on purpose: keeps the
 *  FFM channel clear of C-string-returning (restricted) reads. */
static int32_t copy_module_string(int64_t handle,
                                  const char* (* (BridgeModule::*field))(void),
                                  char* out, int32_t cap) {
    auto* mod = reinterpret_cast<BridgeModule*>(static_cast<uintptr_t>(handle));
    if (!out || cap <= 0) return -1;
    /* BridgeModule keeps PLAIN function pointers as data members (resolved
       at dlopen time), so the selector is a pointer-to-data-member. */
    const char* (*fn)(void) = mod ? (mod->*field) : nullptr;
    const char* s = fn ? fn() : nullptr;
    if (!s) return -1;
    int32_t n = 0;
    for (; s[n] && n < cap - 1; ++n) out[n] = s[n];
    out[n] = '\0';
    return n;
}

JCM_BRIDGE_EXPORT int32_t jcm_module_script_type(int64_t handle, char* out, int32_t cap) {
    return copy_module_string(handle, &BridgeModule::type, out, cap);
}

JCM_BRIDGE_EXPORT int32_t jcm_module_script_id(int64_t handle, char* out, int32_t cap) {
    return copy_module_string(handle, &BridgeModule::id, out, cap);
}

JCM_BRIDGE_EXPORT int64_t jcm_module_state_size(int64_t handle) {
    auto* mod = reinterpret_cast<BridgeModule*>(static_cast<uintptr_t>(handle));
    return (mod && mod->state_size) ? static_cast<int64_t>(mod->state_size()) : 0;
}

/** Direct per-frame render WITHOUT the shared-memory region: the caller
 *  passes the instance key, the raw snapshot address and the resource kind,
 *  and the bridge writes a compact frame descriptor into `out_desc`:
 *
 *      +0  int32 status      (JCM_SHM_* ; 0 = ok)
 *      +4  int32 record_count
 *      +8  5 x { u64 address, u64 length, u64 capacity }  arena triples
 *
 *  (>= 128 bytes; the arenas are module-owned and stay valid until the
 *  next render/dispose on this module — the same discipline as the shm
 *  result slot). This is the direct-call plane of the FFM transport. */
JCM_BRIDGE_EXPORT int32_t jcm_module_render(int64_t handle, const char* key, int32_t key_len,
                                             int64_t snap_addr, int64_t snap_len,
                                             int32_t kind, uint8_t* out_desc) {
    auto* mod = reinterpret_cast<BridgeModule*>(static_cast<uintptr_t>(handle));
    if (!mod || !key || key_len <= 0 || snap_addr == 0 || snap_len <= 0
            || !out_desc || kind < 0 || kind > MTR_RESOURCE_LIFT) {
        return JCM_SHM_ERR_REQUEST;
    }
    if (snap_len > 0x7fffffffLL) return JCM_SHM_ERR_SNAPSHOT;
    const std::string owned(key, static_cast<size_t>(key_len));
    std::lock_guard<std::mutex> lock(mod->mutex);
    JcmFrameOutput out{};
    const int32_t status = run_frame_locked(*mod, owned.c_str(),
            reinterpret_cast<const void*>(static_cast<uintptr_t>(snap_addr)),
            snap_len, kind, out);
    std::memcpy(out_desc + 0, &status, sizeof(status));
    int32_t count = status == JCM_SHM_OK ? out.record_count : 0;
    std::memcpy(out_desc + 4, &count, sizeof(count));
    if (status == JCM_SHM_OK) {
        struct ArenaDesc { uint64_t addr, len, cap; };
        const ArenaDesc arenas[5] = {
            { reinterpret_cast<uintptr_t>(out.records),      static_cast<uint64_t>(out.records_len),      static_cast<uint64_t>(out.records_capacity) },
            { reinterpret_cast<uintptr_t>(out.string_arena), static_cast<uint64_t>(out.string_arena_len), static_cast<uint64_t>(out.string_arena_capacity) },
            { reinterpret_cast<uintptr_t>(out.pixel_arena),  static_cast<uint64_t>(out.pixel_arena_len),  static_cast<uint64_t>(out.pixel_arena_capacity) },
            { reinterpret_cast<uintptr_t>(out.matrix_arena), static_cast<uint64_t>(out.matrix_arena_len), static_cast<uint64_t>(out.matrix_arena_capacity) },
            { reinterpret_cast<uintptr_t>(out.float_arena),  static_cast<uint64_t>(out.float_arena_len),  static_cast<uint64_t>(out.float_arena_capacity) },
        };
        std::memcpy(out_desc + 8, arenas, sizeof(arenas));
    }
    return status;
}

/** Invoke one module api callback (the JS-proxy re-entry downcall):
 *  `function`/`user` are the C function pointer + user payload a module
 *  handed to Java (mtr::Api::Callback), `params` is the JSON request
 *  and the reply is returned as a raw address whose byte length is written
 *  to *(int32_t*)out_len_addr before returning. Reply lifetime: the
 *  callback's thread_local buffer — stable until the next call on the
 *  same thread, which is exactly how the Java caller consumes it. */
JCM_BRIDGE_EXPORT int64_t jcm_module_invoke_api(int64_t function, int64_t user,
                                                 const char* params, int32_t params_len,
                                                 int64_t out_len_addr) {
    using Callback = const char* (*)(void*, const char*, int32_t, int32_t*);
    if (!function || !params || params_len < 0 || !out_len_addr) return 0;
    int32_t length = 0;
    const char* result = reinterpret_cast<Callback>(static_cast<uintptr_t>(function))(
        reinterpret_cast<void*>(static_cast<uintptr_t>(user)), params, params_len, &length);
    std::memcpy(reinterpret_cast<void*>(static_cast<uintptr_t>(out_len_addr)), &length, sizeof(length));
    return reinterpret_cast<int64_t>(result);
}

/** Dispose one instance. `snap_addr`/`snap_len` carry the LIVE driver
 *  buffer when the caller has one (raw address + written length);
 *  snap_addr 0 makes the bridge substitute the module's zeroed fallback
 *  snapshot. */
JCM_BRIDGE_EXPORT void jcm_module_dispose_instance(int64_t handle, const char* key, int32_t key_len,
                                                   int64_t snap_addr, int64_t snap_len) {
    auto* mod = reinterpret_cast<BridgeModule*>(static_cast<uintptr_t>(handle));
    if (!mod || !key || key_len <= 0) return;
    const std::string owned(key, static_cast<size_t>(key_len));
    const void* live = snap_addr ? reinterpret_cast<const void*>(static_cast<uintptr_t>(snap_addr)) : nullptr;
    dispose_instance_core(mod, owned.c_str(), live, live ? snap_len : -1);
}

/* ---- shared-memory channel control (FFM plane, synchronous kick) ------- */

JCM_BRIDGE_EXPORT int32_t jcm_shm_init(int64_t base, int64_t size) {
    return shm_init_core(base, size);
}

JCM_BRIDGE_EXPORT int32_t jcm_shm_kick(void) {
    return shm_kick_core();
}

JCM_BRIDGE_EXPORT void jcm_shm_register_instance(int64_t handle, int64_t instance_id,
                                                 const char* key, int32_t key_len) {
    if (!key || key_len <= 0) return;
    BridgeModule* mod = find_module(static_cast<uint64_t>(handle));
    if (!mod) return;
    std::lock_guard<std::mutex> lock(mod->mutex);
    mod->shm_ids[instance_id] = std::string(key, static_cast<size_t>(key_len));
}

JCM_BRIDGE_EXPORT void jcm_shm_shutdown(void) {
    g_shm.base = nullptr;
    g_shm.size = 0;
}

/* ---- background reclaimer (diagnostics + deterministic drain) ---------- */

JCM_BRIDGE_EXPORT int64_t jcm_reclaimer_pending(void) {
    return g_reclaimer.pending();
}

JCM_BRIDGE_EXPORT void jcm_reclaimer_quiesce(void) {
    g_reclaimer.quiesce();
}

/* ---- poller plane diagnostics (tests; harmless in production) --------- */

JCM_BRIDGE_EXPORT int32_t jcm_poller_attached(void) {
    /* Boot check too: a runtime entry point is a always-safe place to
       ensure the poller exists (self-healing for exotic loaders that
       never ran the constructor; a no-op once it already has). */
    poller_boot();
    return poller().svc_available() ? 1 : 0;
}

/* ---- FFM host vtable installation -------------------------------------- */

/** Installs (or clears) the Panama FFM host upcall table. `vtable_bytes`
 *  is the caller-side sizeof(JcmFfmHostVTable) so a smaller bridge-side
 *  prefix never reads past the caller's buffer; an `abi` the bridge does
 *  not know is REJECTED (never partially trusted). Passing a null table
 *  atomically clears the plane. */
JCM_BRIDGE_EXPORT int32_t jcm_ffm_init(const JcmFfmHostVTable* vtable, int32_t vtable_bytes) {
    poller_boot();   /* runtime entry: cheap idempotent boot check */
    if (!vtable) {
        g_ffm_host = JcmFfmHostVTable{};
        return 0;
    }
    if (vtable_bytes < static_cast<int32_t>(sizeof(JcmFfmHostVTable))) {
        return JCM_SHM_ERR_BAD_LAYOUT;
    }
    if (vtable->abi != JCM_FFM_HOST_ABI) {
        return JCM_SHM_ERR_BAD_LAYOUT;
    }
    JcmFfmHostVTable copy;
    std::memcpy(&copy, vtable, sizeof(copy));
    g_ffm_host = copy;
    return 0;
}

/* ------------------------------------------------------------------ */
/* poller plane bootstrap: run at library load on EVERY platform        */
/*                                                                     */
/* The only "entry point" the poller plane needs. On the FFM plane      */
/* (libraryLookup) the constructors run too, but Java never writes a    */
/* handshake there, so the thread idles in its cheap 250 ms scan and    */
/* never attaches (a poller-mode region also requires HDR_MODE == 1).   */
/*                                                                     */
/* INIT-ORDER SAFETY: this runs BEFORE the TU's _GLOBAL__sub_I_*        */
/* static constructors (the linker orders unprioritized constructor    */
/* attributes ahead of them), so it must not touch ANY C++ static —    */
/* see poller_boot() above. Only pthread_once/InitOnceExecuteOnce and   */
/* pthread_create/CreateThread run here, all C primitives whose own     */
/* static state lives inside libpthread/kernel32, initialized long       */
/* before any image's initializers.                                    */
/*                                                                     */
/* Windows: the thread is created inside DllMain WITHOUT waiting for    */
/* it (the documented loader-lock contract); its DLL_THREAD_ATTACH      */
/* completes after DllMain returns.                                    */
/* ------------------------------------------------------------------ */

#if defined(_WIN32)
extern "C" BOOL WINAPI DllMain(HINSTANCE instance, DWORD reason, LPVOID) {
    if (reason == DLL_PROCESS_ATTACH) {
        DisableThreadLibraryCalls(instance);
        poller_boot();
    }
    return TRUE;
}
#else
__attribute__((constructor)) static void jcm_bridge_autostart() {
    poller_boot();
}
#endif
