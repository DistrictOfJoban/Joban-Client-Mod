/**
 * script.hpp — native script registration.
 *
 * Usage (vehicle example):
 *
 *   struct MyVehicleState { mtr::CycleTracker dest{"KOWLOON|TSUEN WAN"}; };
 *
 *   struct MyVehicleScript : mtr::VehicleScript<MyVehicleState> {
 *       static constexpr auto ID = "demo:vehicle";
 *
 *       void create(VehicleContext& ctx, MyVehicleState& state,
 *                   const Vehicle& vehicle) override { ... }
 *       void render(VehicleContext& ctx, MyVehicleState& state,
 *                   const Vehicle& vehicle) override { ... }
 *   };
 *
 *   MTR_REGISTER_VEHICLE_SCRIPT(MyVehicleScript)
 *
 * This mirrors the JS lifecycle functions create/render/dispose.
 * Per-instance `state` blocks are allocated by the HOST (one per
 * vehicle / PIDS / block-entity, exactly like JCM creates one
 * `state` JS object per ScriptInstance) and passed through
 * JcmFrameInput.state / state_size.
 */
#pragma once

#include "mtr_native.h"
#include "frame.hpp"
#include "crash_guard.hpp"
#include "vehicle.hpp"
#include "pids.hpp"
#include "eyecandy.hpp"
#include "lift.hpp"
#include "api.hpp"
#include <cstring>
#include <new>
#include <type_traits>
#include <unordered_set>

namespace mtr {

/* Host-side frame buffers: attached once, reused for the whole module
   lifetime. The host serializes render calls per script module on one
   background thread (mirrors JCM's per-script single-thread executor),
   so plain storage is safe. Double buffering between mtrRender calls
   happens host-side: it reads the previous frame while we fill the
   next one. */
struct HostBuffers {
    static constexpr int32_t MAX_RECORDS  = 256;
    static constexpr int32_t MAX_MATRICES = 128;
    static constexpr int32_t MAX_STRING   = 32 * 1024;
    static constexpr int64_t MAX_PIXEL    = 2048LL * 2048LL * 4;
    static constexpr int32_t MAX_FLOAT    = 1024;

    /* Record slots must fit the LARGEST record struct (JcmDrawText). */
    alignas(64) uint8_t records[sizeof(JcmDrawText) * MAX_RECORDS];
    alignas(64) float  matrix_arena[MAX_MATRICES * 16];
    alignas(64) char   string_arena[MAX_STRING];
    /* 1 MB initial pixel arena (512x512 BGRA8) — grows on demand up to
       MAX_PIXEL (see FrameRecorder::push_pixels) and DECAYS back toward
       this floor after the spike has passed (FrameRecorder's idle shrink),
       keeping the per-module footprint small when scripts paint little or
       nothing. This used to be a flat 16 MB per module, which dominated
       the reported native memory usage. */
    alignas(64) uint8_t pixel_arena[512 * 512 * 4];
    alignas(64) float  float_arena[MAX_FLOAT];
};

namespace detail {

inline FrameRecorder& recorder() {
    static FrameRecorder rec;
    return rec;
}

inline void install_frame() {
    /* Heap-backed on purpose: a 16 MB static aggregate is emitted by
       Apple ld64 as *initialized* __DATA (comdat zero aggregates become
       file-backed data), which bloated every macOS dylib to ~17 MB.
       `new` gives demand-zero pages on all three platforms and keeps
       the one-time attach semantics. */
    static std::unique_ptr<HostBuffers> buffers(new HostBuffers());
    static bool attached = false;
    if (!attached) {
        recorder().attach(buffers->records, buffers->matrix_arena,
                          buffers->string_arena, buffers->pixel_arena,
                          sizeof(buffers->pixel_arena), buffers->float_arena);
        attached = true;
    }
    recorder().begin_frame();
}

} /* namespace detail */

/* ------------------------------------------------------------------ */
/* Kind adapters: Context / Wrapper / snapshot construction            */
/* ------------------------------------------------------------------ */

namespace detail {

struct VehicleAdapter {
    static constexpr int RESOURCE_ID = MTR_RESOURCE_VEHICLE;
    static constexpr const char* TYPE = "vehicle";
    using Context = VehicleContext;
    using Wrapper = Vehicle;
    using Snapshot = JcmVehicleSnapshot;
    static VehicleContext make_context(FrameRecorder& f, const JcmFrameInput& in) {
        return VehicleContext(f, in);
    }
    static Vehicle make_wrapper(const void* snap) {
        return Vehicle(static_cast<const JcmVehicleSnapshot*>(static_cast<const void*>(snap)));
    }
};

struct PidsAdapter {
    static constexpr int RESOURCE_ID = MTR_RESOURCE_PIDS;
    static constexpr const char* TYPE = "pids";
    using Context = PidsContext;
    using Wrapper = Pids;
    using Snapshot = JcmPidsSnapshot;
    static PidsContext make_context(FrameRecorder& f, const JcmFrameInput& in) {
        return PidsContext(f, in);
    }
    static Pids make_wrapper(const void* snap) {
        return Pids(static_cast<const JcmPidsSnapshot*>(static_cast<const void*>(snap)));
    }
};

struct LiftAdapter {
    static constexpr int RESOURCE_ID = MTR_RESOURCE_LIFT;
    static constexpr const char* TYPE = "lift";
    using Context = LiftContext;
    using Wrapper = Lift;
    using Snapshot = JcmLiftSnapshot;
    static LiftContext make_context(FrameRecorder& f, const JcmFrameInput& in) { return LiftContext(f, in); }
    static Lift make_wrapper(const void* snap) { return Lift(static_cast<const JcmLiftSnapshot*>(snap)); }
};

struct EyecandyAdapter {
    static constexpr int RESOURCE_ID = MTR_RESOURCE_EYECANDY;
    static constexpr const char* TYPE = "eyecandy";
    using Context = EyeCandyContext;
    using Wrapper = EyeCandy;
    using Snapshot = JcmEyecandySnapshot;
    static EyeCandyContext make_context(FrameRecorder& f, const JcmFrameInput& in) {
        return EyeCandyContext(f, in);
    }
    static EyeCandy make_wrapper(const void* snap) {
        return EyeCandy(static_cast<const JcmEyecandySnapshot*>(static_cast<const void*>(snap)));
    }
};

} /* namespace detail */

} /* namespace mtr */

/* ------------------------------------------------------------------ */
/* Script base classes                                                 */
/* ------------------------------------------------------------------ */

namespace mtr {

template <typename State, typename Adapter>
class ScriptBase {
public:
    using StateType = State;
    using Kind = Adapter;

    virtual ~ScriptBase() = default;

    /* JS: function create(ctx, state, wrapper) {} */
    virtual void create(typename Adapter::Context& ctx, State& state,
                        const typename Adapter::Wrapper& wrapper) {
        (void)ctx; (void)state; (void)wrapper;
    }

    /* JS: function render(ctx, state, wrapper) {} */
    virtual void render(typename Adapter::Context& ctx, State& state,
                        const typename Adapter::Wrapper& wrapper) {
        (void)ctx; (void)state; (void)wrapper;
    }

    /* JS: function dispose(ctx, state, wrapper) {} */
    virtual void dispose(typename Adapter::Context& ctx, State& state,
                         const typename Adapter::Wrapper& wrapper) {
        (void)ctx; (void)state; (void)wrapper;
    }
};

template <typename State>
class VehicleScript : public ScriptBase<State, detail::VehicleAdapter> {};

template <typename State>
class PidsScript : public ScriptBase<State, detail::PidsAdapter> {};

template <typename State>
class LiftScript : public ScriptBase<State, detail::LiftAdapter> {};

template <typename State>
class EyecandyScript : public ScriptBase<State, detail::EyecandyAdapter> {};

} /* namespace mtr */

/* ------------------------------------------------------------------ */
/* Registration: expands the module exports                            */
/* ------------------------------------------------------------------ */

#define MTR_INTERNAL_EXPORTS(ScriptType, TypeString, IdString)                  \
    extern "C" {                                                                \
    MTR_NATIVE_EXPORT uint32_t mtrNativeAbiVersion(void) {                      \
        return MTR_NATIVE_ABI_VERSION;                                          \
    }                                                                           \
    MTR_NATIVE_EXPORT const char* mtrScriptType(void) { return TypeString; }    \
    MTR_NATIVE_EXPORT const char* mtrScriptId(void) { return IdString; }        \
    MTR_NATIVE_EXPORT size_t mtrStateSize(void) {                               \
        return sizeof(typename ScriptType::StateType);                          \
    }                                                                           \
    static ::mtr::detail::ScriptBox<ScriptType> mtr_g_script;                   \
    MTR_NATIVE_EXPORT void mtrInit(const JcmFrameInput* in) {                   \
        mtr_g_script.init_state(in);                                            \
    }                                                                           \
    MTR_NATIVE_EXPORT int32_t mtrCreate(const JcmFrameInput* in) {              \
        MTR_GUARDED_BODY(return mtr_g_script.lifecycle<0>(in, nullptr))         \
    }                                                                           \
    MTR_NATIVE_EXPORT int32_t mtrRender(const JcmFrameInput* in,                \
                                        JcmFrameOutput* out) {                  \
        /* A fault inside a script must disable THAT module, not kill the game. \
           See crash_guard.hpp. */                                             \
        MTR_GUARDED_BODY(return mtr_g_script.lifecycle<1>(in, out))             \
    }                                                                           \
    MTR_NATIVE_EXPORT int32_t mtrDispose(const JcmFrameInput* in) {             \
        MTR_GUARDED_BODY(return mtr_g_script.lifecycle<2>(in, nullptr))         \
    }                                                                           \
    } /* extern "C" */

#define MTR_REGISTER_VEHICLE_SCRIPT(S) \
    MTR_INTERNAL_EXPORTS(S, "vehicle", S::ID)
#define MTR_REGISTER_PIDS_SCRIPT(S) \
    MTR_INTERNAL_EXPORTS(S, "pids", S::ID)
#define MTR_REGISTER_LIFT_SCRIPT(S) \
    MTR_INTERNAL_EXPORTS(S, "lift", S::ID)
#define MTR_REGISTER_EYECANDY_SCRIPT(S) \
    MTR_INTERNAL_EXPORTS(S, "eyecandy", S::ID)

namespace mtr { namespace detail {

/**
 * ScriptBox — adapts a typed ScriptType onto the C exports.
 *
 * Per-instance state: the host owns the state block (JcmFrameInput.state /
 * state_size, one per instance) but its CONTENTS ARE UNINITIALISED — a
 * zero-filled block is not a valid State unless State is trivially copyable.
 * The host therefore calls mtrInit() once after allocating; that is where the
 * placement-new happens, and it is also the ABI-6 answer to the older
 * "assume zeroed memory is fine" contract, which only ever worked by accident
 * on MSVC (libstdc++'s std::string keeps its buffer pointer inline, so a
 * zeroed one dereferences null).
 *
 * Defensive fallbacks, in case a caller skips mtrInit():
 *   - init_state() is idempotent, so a host that calls it twice is harmless;
 *   - lifecycle() also constructs on first use, so an mtrRender() without a
 *     preceding mtrCreate() cannot read an unconstructed object;
 *   - when the host passes no state at all, a module-local block is used.
 */
template <typename ScriptType>
struct ScriptBox {
    ScriptType script;
    typename ScriptType::StateType* fallback_state = nullptr;
    /* ABI 6 state tracking. The host may own SEVERAL blocks over the module's
       lifetime (one per instance, and one at a time per module because the host
       serialises per instance), and it may hand them over in any order. Identity
       of the block is therefore what decides re-construction — not a plain
       "already initialised" flag, which would silently skip construction for a
       second instance and let the script run on unconstructed memory. */
    std::unordered_set<void*> live_states;
    void* create_records_state = nullptr;

    using State = typename ScriptType::StateType;
    using Adapter = typename ScriptType::Kind;

    /* ABI 6: mtrInit — construct the State inside the host's block.
       Called once per instance, right after the host allocates the block.
       Each live block is constructed once, even when vehicles alternate every
       frame. Switching to another live instance must not destroy or reset the
       previous one. Only mtrDispose removes and destroys that instance. */
    void init_state(const JcmFrameInput* in) {
        if (!in) return;
        State* state = resolve_state(*in);
        if (!state) return;
        void* block = static_cast<void*>(state);

        if (live_states.find(block) != live_states.end()) return;
        if constexpr (!std::is_trivially_default_constructible_v<State>) {
            new (static_cast<void*>(state)) State();
        }
        live_states.insert(block);
    }

    template <int Phase> /* 0=create 1=render 2=dispose */
    int32_t lifecycle(const JcmFrameInput* in, JcmFrameOutput* out) {
        if (!in) return -1;
        if (in->abi_version != MTR_NATIVE_ABI_VERSION) return -2;
        if (in->resource_kind != Adapter::RESOURCE_ID) return -3;

        /* ABI 10: publish the host's deferred-reclamation hook BEFORE the
           frame runs — begin_frame() (inside install_frame) may shrink a
           spike-grown pixel arena, and the replaced block's free belongs
           on the host's reclaimer thread, not this JVM-owned one. Hosts
           predating ABI 10 leave host->defer_free null: the recorder then
           keeps the synchronous free and behaves exactly like ABI 9. */
        recorder().set_defer_hook(in->host);

        if constexpr (Phase == 1) {
            if (create_records_state != in->state || in->state == nullptr) install_frame();
            create_records_state = nullptr;
        } else {
            install_frame();
        }

        /* Resolve the state block for this instance. mtrInit() should already
           have constructed it; do it here too so a create/render that arrives
           first (a driver that skips mtrInit, or an out-of-order first frame)
           still operates on a valid object. */
        State* state = resolve_state(*in);
        init_state(in);

        auto ctx = Adapter::make_context(recorder(), *in);
        auto wrapper = Adapter::make_wrapper(in->snapshot);

        if constexpr (Phase == 0) {
            script.create(ctx, *state, wrapper);
            create_records_state = in->state;
        } else if constexpr (Phase == 1) {
            script.render(ctx, *state, wrapper);
            recorder().write_output(*out);
        } else {
            try { script.dispose(ctx, *state, wrapper); }
            catch (...) { destroy_state(state); throw; }
            destroy_state(state);
        }
        return 0;
    }

private:
    void destroy_state(State* state) {
        if (state) state->~State();
        live_states.erase(static_cast<void*>(state));
    }
    State* resolve_state(const JcmFrameInput& in) {
        if (in.state && in.state_size >= sizeof(State)) {
            return static_cast<State*>(in.state);
        }
        if (!fallback_state) {
            static Storage storage;
            fallback_state = storage.addr();
        }
        return fallback_state;
    }

    /* Module-local fallback storage (also used by the benchmark driver). */
    struct Storage {
        alignas(alignof(State)) uint8_t bytes[sizeof(State)];
        State* addr() { return reinterpret_cast<State*>(bytes); }
    };
};

}} /* namespace mtr::detail */
