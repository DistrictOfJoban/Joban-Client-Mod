/**
 * mtr_native.h — Minecraft Transit Railway 4.0 Native Scripting C ABI
 *
 * Mirrors the Joban Client Mod v2.3 (Rhino/JavaScript) scripting surface
 * for vehicle / eye_candy / pids resources, but executes user code as a
 * native shared library — loaded and driven WITHOUT any JNI since layout
 * v2 (the bridge's Panama FFM plane on JDK 22+, or its shared-memory
 * poller plane on any JVM).
 *
 * Design goals (vs. the JS pipeline):
 *   1. ONE host crossing per frame per instance:
 *      the host marshals a POD snapshot in, the script returns a flat
 *      array of draw-call records out. (JS: hundreds of reflective
 *      NativeJavaObject getter calls per frame.)
 *   2. Zero per-frame allocations on the script hot path:
 *      draw calls are bump-allocated inside a double-buffered arena.
 *   3. Identical lifecycle semantics: create -> render -> dispose,
 *      persistent per-instance state, capture-then-replay rendering.
 *
 * ABI stability: everything below is plain C. All structs are versioned
 * via MTR_NATIVE_ABI_VERSION; the host refuses to load modules that
 * report an incompatible version (same behaviour as JCM's script
 * reload-on-mismatch strategy).
 *
 * License: MIT (aligned with Joban Client Mod's LICENSE)
 */
#ifndef MTR_NATIVE_H
#define MTR_NATIVE_H

#include <stdint.h>
#include <stddef.h>

#ifdef _WIN32
#  define MTR_NATIVE_EXPORT __declspec(dllexport)
#else
#  define MTR_NATIVE_EXPORT __attribute__((visibility("default")))
#endif

#ifdef __cplusplus
extern "C" {
#endif

/* ------------------------------------------------------------------ */
/* Version / module identity                                           */
/* ------------------------------------------------------------------ */

/* ABI 11 promotes the vehicle snapshot's _pad0 flags byte (offset 83) to
   door_direction — the JCM door-opening-side extension mirroring
   VehicleWrapper.getDirectionOfDoorOpening(): 0 "not", 1 "left",
   2 "right", 3 "all" (see JcmVehicleSnapshot). The byte was already
   reserved padding in every ABI≤10 layout and no older module reads it, so
   the struct size/offsets and the group-9 blob sharing are unchanged;
   the bump exists so an ABI 11 module is refused by ABI 10 hosts, which
   would otherwise feed it a constant 0 ("not"). ABI 10 modules are
   unaffected and still served (their header declares the same offset as
   _pad0).
   ABI 10 appends JcmHostServices.defer_free: a background reclamation
   hook so scripts can hand DEAD native blocks to the host's off-thread
   reclaimer instead of freeing them synchronously inside mtrRender/mtrDispose
   (which runs on a JVM-owned thread — see the ABI 10 comment on defer_free).
   ABI 10 modules compiled against the new header call a host function slot
   that does not exist in an ABI 9 host (the struct grew), so an old host
   must refuse them — hence the bump. ABI 9 modules are unaffected: the
   bridge still serves their layout and feeds their own ABI number back.
   ABI 9 appends the canonical transport TYPE ids to the vehicle header
   (vehicle_type_offset/len) and to every interchange (type_offset/len),
   exposing the MAGIC MTR Addon's route types alongside the four base modes.
   ABI 9 hosts also stop copying the snapshot per frame and reuse the arena
   views on the host boundary (see bridge.cpp).
   ABI 8 appends invoke_api, Lift snapshots and typed interchange modes.
   ABI 6/7 modules retain their original host prefix and snapshot layouts.
   ABI 7 appends resource-font metrics and baseline text rasterization to
   JcmHostServices. The ABI 6 prefix and snapshot layouts are unchanged.
   ABI 6 — mtrInit(): the host must construct the per-instance state
   object instead of assuming a zeroed block is usable (see mtrStateSize).
   ABI 5 added JcmStop.route_circular_state (per-stop route
   CircularState).  ABI 4 added JcmHostServices.acquire_quad_model
   (host-built textured quad) — bumped because scripts compiled against
   the newer header call a host function slot that does not exist in an
   older host (the struct grew), so an old host must refuse the module
   instead of reading past it. */
#define MTR_NATIVE_ABI_VERSION 11

/* Resource kinds, identical to JCM script contexts. */
enum MtrResourceKind {
    MTR_RESOURCE_VEHICLE  = 0,
    MTR_RESOURCE_PIDS     = 1,
    MTR_RESOURCE_EYECANDY = 2,
    MTR_RESOURCE_LIFT     = 3
};

/* ------------------------------------------------------------------ */
/* Geometry (mirrors com.lx862.mtrscripting.core.util.Matrices)        */
/* ------------------------------------------------------------------ */

/* Column-major 4x4, matches MC GraphicsHolder stack order. */
typedef struct JcmMat4 {
    float m[16];
} JcmMat4;

/* A compiled pose: an array of matrices applied in order. */
typedef struct JcmPose {
    int32_t   matrix_count;
    int32_t   matrix_offset;   /* into the frame matrix arena */
} JcmPose;

/* ------------------------------------------------------------------ */
/* Draw call records (mirrors ScriptRenderManager draw calls)          */
/*                                                                     */
/* Text / Texture records mirror PIDSDrawCall + TextWrapper /          */
/* TextureWrapper field-for-field so the host can replay them with     */
/* the exact same rendering code paths used for JS scripts.            */
/* ------------------------------------------------------------------ */

/* TextWrapper overflow modes. */
enum MtrTextOverflow {
    MTR_TEXT_OVERFLOW_NONE     = 0,
    MTR_TEXT_OVERFLOW_STRETCH  = 1,  /* .stretchXY() */
    MTR_TEXT_OVERFLOW_SCALE    = 2,  /* .scaleXY()   */
    MTR_TEXT_OVERFLOW_WRAP     = 3,  /* .wrapText()  */
    MTR_TEXT_OVERFLOW_MARQUEE  = 4   /* .marquee()   */
};

/* TextWrapper alignment. */
enum MtrTextAlign {
    MTR_TEXT_ALIGN_LEFT   = -1,
    MTR_TEXT_ALIGN_CENTER = 0,
    MTR_TEXT_ALIGN_RIGHT  = 1
};

/* Render layers (mirrors org.mtr.mod.render.QueuedRenderLayer). */
enum MtrRenderLayer {
    MTR_LAYER_TEXT   = 0,  /* TEXT        */
    MTR_LAYER_LIGHT  = 1,  /* LIGHT_1..3  */
    MTR_LAYER_EXTERIOR = 2,
    MTR_LAYER_INTERIOR = 3
};

enum MtrDrawKind {
    JCM_DRAW_TEXT           = 1,  /* PIDS / common text            */
    JCM_DRAW_TEXTURE        = 2,  /* PIDS / common textured quad   */
    JCM_DRAW_MODEL          = 3,  /* vehicle car / eyecandy model  */
    JCM_DRAW_SOUND          = 4,  /* positional sound              */
    JCM_DRAW_LOCAL_SOUND    = 5,  /* announcement (riding player)  */
    JCM_DRAW_TEXTURE_UPLOAD = 6,  /* GraphicsTexture blit          */
    JCM_DRAW_OUTLINE_SHAPE  = 7,  /* eyecandy collision/outline    */
    JCM_DRAW_BLOCK_EVENT    = 8   /* eyecandy onBlockUse handshakes*/
};

/* Per-record flags (JcmRecordHeader.flags). Zero for records emitted by
   modules compiled against older SDKs — FrameRecorder::push_record() zeroes
   the whole record, so old binaries simply report "no flags" and hosts
   predating this definition ignore the byte entirely. Both directions stay
   binary compatible: no struct size or field offset changes. */
#define MTR_RECORD_FLAG_NATURAL_LIGHT 0x01u  /* Text/Texture/Rect: take the
                                                world light value instead of
                                                MAX_RENDER_LIGHT (mirrors the
                                                JS wrappers' naturalLight()) */

/* Every frame record starts with this header. Records are laid out
   SEQUENTIALLY (variable size): walk with pos += header.record_size.
   This keeps the arena compact regardless of per-kind struct size. */
typedef struct JcmRecordHeader {
    uint8_t  kind;            /* MtrDrawKind */
    uint8_t  flags;           /* MTR_RECORD_FLAG_* */
    uint8_t  _pad0[2];
    int32_t  record_size;     /* total bytes, header included */
} JcmRecordHeader;

typedef struct JcmDrawText {
    JcmRecordHeader header;     /* JCM_DRAW_TEXT */
    uint8_t  shadow;
    uint8_t  bold;
    uint8_t  italic;
    uint8_t  _pad1;
    int32_t  layer;
    int32_t  color;           /* ARGB */
    int32_t  align;           /* MtrTextAlign */
    int32_t  overflow;        /* MtrTextOverflow */
    int32_t  z_order;         /* -1 = auto z-ordering (PIDSScriptContext) */
    double   x, y, w, h;      /* screen-space (PIDS) */
    double   scale;
    double   marquee_duration;  /* -1 = default */
    double   marquee_progress;  /* -1 = time-based */
    int32_t  font_id_offset;  /* into frame string arena, len 0 = MC font */
    int32_t  font_id_len;
    int32_t  text_offset;     /* into frame string arena (UTF-8) */
    int32_t  text_len;
} JcmDrawText;

typedef struct JcmDrawTexture {
    JcmRecordHeader header;     /* JCM_DRAW_TEXTURE */
    int32_t  layer;
    int32_t  color;           /* tint, ARGB */
    double   x, y, w, h;
    float    u1, v1, u2, v2;
    int32_t  z_order;
    int32_t  texture_offset;  /* into frame string arena, e.g. "example:textures/screen.png" */
    int32_t  texture_len;
} JcmDrawTexture;

/* Model draw: vehicle car body / bogie or eyecandy model.
   Mirrors VehicleScriptContext.drawCarModel / EyeCandyScriptContext.drawModel. */
typedef struct JcmDrawModel {
    JcmRecordHeader header;     /* JCM_DRAW_MODEL */
    uint8_t  car;             /* vehicle car index (eyecandy: 0) */
    uint8_t  bogie;           /* 0xFF = body, else bogie index */
    uint8_t  _pad1;
    int32_t  model_handle;    /* host-side ModelJS handle (see host acquire_model) */
    int32_t  pose_offset;     /* matrix index into frame matrix arena, -1 = identity */
    int32_t  pose_count;
} JcmDrawModel;

typedef struct JcmDrawSound {
    JcmRecordHeader header;     /* JCM_DRAW_SOUND or JCM_DRAW_LOCAL_SOUND */
    uint8_t  car;
    uint8_t  _pad1[3];
    float    x, y, z;         /* local offset from car/be origin */
    float    volume;
    float    pitch;
    int32_t  sound_offset;    /* into frame string arena */
    int32_t  sound_len;
} JcmDrawSound;

/* GraphicsTexture upload: BGRA8 pixels, row-major. */
typedef struct JcmDrawTextureUpload {
    JcmRecordHeader header;     /* JCM_DRAW_TEXTURE_UPLOAD */
    int32_t  texture_handle;  /* host GraphicsTexture handle (-1: no host texture) */
    int32_t  width;
    int32_t  height;
    int32_t  dirty_x, dirty_y, dirty_w, dirty_h;  /* dirty rect */
    int64_t  pixel_data_offset; /* into frame pixel arena (BGRA8) */
    int64_t  pixel_data_len;   /* dirty_w * dirty_h * 4 */
} JcmDrawTextureUpload;

typedef struct JcmDrawShape {
    JcmRecordHeader header;     /* JCM_DRAW_OUTLINE_SHAPE */
    uint8_t  is_collision;    /* 1 = collision (max 1.5 blocks), 0 = outline */
    uint8_t  _pad1[3];
    int32_t  box_count;       /* VoxelShapeWrapper boxes, AABB triples */
    int32_t  box_offset;      /* byte offset into frame float arena: 6 floats per box */
} JcmDrawShape;

/* ------------------------------------------------------------------ */
/* Data snapshots (marshalled once per frame, direct ByteBuffer)       */
/*                                                                     */
/* OFFSET CONVENTION (ABI-critical): every *_offset field inside the   */
/* sub-structs below is a byte offset relative to the START of the     */
/* snapshot blob — i.e. relative to the pointer passed as              */
/* JcmFrameInput.snapshot. string_pool_offset/len describe where the  */
/* shared UTF-8 string pool lives inside the blob (bookkeeping + host  */
/* marshalling).                                                       */
/* ------------------------------------------------------------------ */

/* Mirrors VehicleWrapper.Stop. */
typedef struct JcmStop {
    int64_t  route_id;
    int64_t  station_id;
    int64_t  platform_id;
    double   distance;          /* rail progress, -1 if unavailable */
    double   dwell_time_millis;
    int32_t  name_offset;       /* UTF-8, into snapshot string pool */
    int32_t  name_len;
    int32_t  destination_offset;
    int32_t  destination_len;
    int32_t  custom_destination_offset;  /* -1 = null */
    int32_t  custom_destination_len;
    int32_t  interchange_count;
    int32_t  interchange_offset; /* into snapshot JcmInterchange pool */
    /* v3 (ABI 3): station exits (JS: station.getExits()). exit_offset
       points at a JcmExit[] pool inside the snapshot blob; 0 = none. */
    int32_t  exit_count;
    int32_t  exit_offset;
    /* v5 (ABI 5): this stop's ROUTE CircularState
       (JS: stop.route.getCircularState(), 0 NONE / 1 CLOCKWISE /
       2 ANTICLOCKWISE). Per-stop data distinguishes route changes when
       a vehicle continues onto a different route. */
    uint8_t  route_circular_state;
    uint8_t  is_route_switchover;
    uint8_t  _pad0[2];
} JcmStop;

typedef struct JcmInterchange {
    int32_t color;
    int32_t route_name_offset;
    int32_t route_name_len;
    int32_t transport_mode; /* ABI 8: -1 when older server data omits it */
    /* ABI 9: canonical transport TYPE id into the snapshot string pool —
       "train", "boat", "cable_car", "airplane", or a MAGIC MTR Addon
       extension such as "train_metro" / "train_high_speed". Empty when
       the host cannot resolve a type. */
    int32_t type_offset;
    int32_t type_len;
} JcmInterchange;

/* v3 (ABI 3): station exit, mirrors StationExit (JS: station.getExits()
 * -> Exit.getName() / getDestinations()). name is e.g. "A";
 * destinations are the landmark strings shown under the exit letter. */
typedef struct JcmExit {
    int32_t name_offset;         /* UTF-8, into snapshot string pool */
    int32_t name_len;
    int32_t destination_count;   /* JcmStrRef[] at destination_offset */
    int32_t destination_offset;  /* into snapshot blob (str-ref pool) */
} JcmExit;

/* v3: string reference pair used for exit destination lists. */
typedef struct JcmStrRef {
    int32_t offset;
    int32_t len;
} JcmStrRef;

/* Mirrors ArrivalWrapper. */
typedef struct JcmArrival {
    int64_t  arrival_epoch_millis;   /* already server-offset corrected */
    int64_t  departure_epoch_millis;
    int64_t  deviation_millis;
    int64_t  departure_index;
    int64_t  route_id;
    int64_t  platform_id;
    int32_t  route_color;
    int32_t  car_count;
    uint8_t  realtime;
    uint8_t  is_terminating;
    uint8_t  circular_state;       /* 0 none, 1 clockwise, 2 anticlockwise */
    uint8_t  _pad0;
    int32_t  route_number_offset;  /* UTF-8 strings into snapshot pool */
    int32_t  route_number_len;
    int32_t  route_name_offset;
    int32_t  route_name_len;
    int32_t  destination_offset;
    int32_t  destination_len;
    int32_t  platform_name_offset;
    int32_t  platform_name_len;
} JcmArrival;

/* Mirrors VehicleWrapper.getDirectionOfDoorOpening() — which side(s) of
   the vehicle the doors can open on, relative to the vehicle HEAD (bogie-1
   end), unaffected by travel direction or reversing. See
   JcmVehicleSnapshot.door_direction. */
enum MtrDoorDirection {
    MTR_DOOR_DIRECTION_NOT   = 0,
    MTR_DOOR_DIRECTION_LEFT  = 1,
    MTR_DOOR_DIRECTION_RIGHT = 2,
    MTR_DOOR_DIRECTION_ALL   = 3
};

/* Mirrors VehicleWrapper + NTETrainWrapper flattened. */
typedef struct JcmVehicleSnapshot {
    int64_t  vehicle_id;
    int64_t  siding_id;
    int64_t  this_route_id;
    int64_t  departure_index;
    int32_t  car_count;
    int32_t  transport_mode;      /* 0 TRAIN .. see MtrTransportMode */
    double   speed_kmh;
    double   speed_ms;
    double   rail_progress;
    double   door_value;          /* 0..1 */
    int32_t  notch_level;         /* 0..5 */
    uint8_t  reversed;
    uint8_t  on_route;
    uint8_t  door_opening;
    uint8_t  currently_manual;
    uint8_t  manual_allowed;
    uint8_t  client_player_riding;
    uint8_t  any_car_rendered;
    /* v11 (was _pad0): which side(s) the vehicle's doors can open on —
       mirrors VehicleWrapper.getDirectionOfDoorOpening() (JS:
       vehicle.getDirectionOfDoorOpening()). While dwelling at a platform
       this is reported identically regardless of the current door phase;
       while running it predicts the NEXT stop. Left/right are relative to
       the vehicle head (bogie-1 end), unaffected by reversing. Values: 0
       "not", 1 "left", 2 "right", 3 "all" — see MtrDoorDirection and
       mtr::door_direction. Purely informational: MTR still owns which
       doors actually open. */
    uint8_t  door_direction;
    double   total_dwell_time_millis;
    double   elapsed_dwell_time_millis;
    int64_t  game_time_millis;    /* host wall clock (System.currentTimeMillis) */
    int64_t  in_game_time;        /* 0..24000 ticks */
    /* per-car arrays (length = car_count), offsets into snapshot pools */
    int32_t  car_offset;          /* JcmCar[] */
    int32_t  stop_count;          /* allStops */
    int32_t  stop_offset;
    int32_t  this_route_stop_count;
    int32_t  this_route_stop_offset;
    int32_t  next_route_stop_count;
    int32_t  next_route_stop_offset;
    int32_t  next_stop_index;     /* precomputed for this-route stops */
    /* v2 (ABI 2): current-route identity, mirrors what JS reads from
       thisRouteStops.get(0).route (name / color / CircularState).
       The host marshals these values once per frame. */
    int32_t  route_name_offset;   /* UTF-8 into string pool, "" = unknown */
    int32_t  route_name_len;
    int32_t  route_color;         /* ARGB, host-safe default 0xFF009BC0 */
    uint8_t  circular_state;      /* 0 none, 1 clockwise, 2 anticlockwise */
    uint8_t  _pad1[3];
    /* v2: siding display name (JS: vehicle.getSiding().getName()) —
       can contain a user-defined name or description. */
    int32_t  siding_name_offset;
    int32_t  siding_name_len;
    int32_t  string_pool_offset;  /* shared string pool */
    int32_t  string_pool_len;
    /* v9 (ABI 9): the vehicle's canonical transport TYPE id into the string
       pool — the base mode ("train", "boat", "cable_car", "airplane") or a
       MAGIC MTR Addon route type ("train_metro", ...). Mirrors
       VehicleWrapper.getTransportType(); scripts read it through
       mtr::Vehicle::getTransportType() (legacy alias: mtr::Train).
       Empty when unresolved. */
    int32_t  vehicle_type_offset;
    int32_t  vehicle_type_len;
} JcmVehicleSnapshot;

typedef struct JcmCar {
    float    length;
    float    width;
    uint8_t  left_door_open;
    uint8_t  right_door_open;
    uint8_t  rendered;            /* ray-traced visibility */
    uint8_t  _pad0;
    int32_t  vehicle_type_offset; /* e.g. "mtr_custom_train_kcx_cab_1" */
    int32_t  vehicle_type_len;
} JcmCar;

/* Mirrors PIDSWrapper. */
typedef struct JcmPidsSnapshot {
    int64_t  block_pos[3];
    int64_t  station_id;
    int64_t  game_time_millis;
    int64_t  in_game_time;
    int32_t  width;               /* display width  (1.0 unit = 16 px) */
    int32_t  height;
    int32_t  rows;
    int8_t   pids_type;           /* 0 single, 1 double-sided ... */
    uint8_t  key_block;
    uint8_t  platform_number_hidden;
    uint8_t  _pad0;
    int32_t  arrival_count;
    int32_t  arrival_offset;      /* JcmArrival[] */
    int32_t  row_hidden_bits;     /* bit i = row i hidden (mirrors isRowHidden) */
    int32_t  custom_message_count;
    int32_t  custom_message_offset; /* JcmStrRef[] */
    int32_t  target_platform_count;
    int32_t  target_platform_offset; /* int64[] */
    int32_t  string_pool_offset;
    int32_t  string_pool_len;
} JcmPidsSnapshot;

typedef struct JcmLiftFloor {
    int32_t index;
    uint8_t current_floor, target_floor;
    uint8_t _pad0[2];
    double position[3];
    int32_t number_offset, number_len;
    int32_t description_offset, description_len;
} JcmLiftFloor;

typedef struct JcmLiftSnapshot {
    int64_t id, game_time_millis, in_game_time;
    double position[3], offset[3];
    double width, height, depth;
    double angle_degrees, angle_radians, door_value;
    int32_t direction;
    uint8_t should_render, double_sided, player_riding;
    uint8_t _pad0;
    int32_t floor_count, floor_offset;
} JcmLiftSnapshot;

/* Mirrors EyecandyBlockEntityWrapper. */
typedef struct JcmEyecandySnapshot {
    int64_t  block_pos[3];
    int64_t  game_time_millis;
    int64_t  in_game_time;
    float    translate[3];
    float    rotate[3];
    uint8_t  full_brightness;
    uint8_t  facing;              /* 0..5 like MC Direction */
    uint8_t  crosshair_target;
    uint8_t  _pad0;
    int32_t  redstone_level;      /* 0..15 */
    int32_t  model_id_offset;     /* UTF-8 into pool */
    int32_t  model_id_len;
    uint32_t block_use_events;    /* bitmask: pending onBlockUse events */
    int32_t  _pad1;
} JcmEyecandySnapshot;

/* Generic envelope the host passes in; kind selects the snapshot union. */
typedef struct JcmFrameInput {
    uint32_t abi_version;
    int32_t  resource_kind;      /* MtrResourceKind */
    const void* snapshot;        /* one of Jcm*Snapshot */
    void*    state;              /* persistent per-instance block */
    size_t   state_size;
    /* host services callable from the script thread */
    const struct JcmHostServices* host;
} JcmFrameInput;

/* ------------------------------------------------------------------ */
/* Frame output: flat draw-call array + auxiliary arenas               */
/* ------------------------------------------------------------------ */

typedef struct JcmFrameOutput {
    int32_t  record_count;
    int32_t  _pad0;
    int64_t  records_len;     /* total bytes of the sequential record stream */
    int64_t  matrix_arena_len;  /* float stream length, in bytes */
    int64_t  string_arena_len;  /* UTF-8 stream, in bytes */
    int64_t  pixel_arena_len;   /* BGRA8 stream, in bytes */
    int64_t  float_arena_len;   /* float stream (voxel boxes), in bytes */
    /* Actual arena buffers (host reads, then recycles the frame) */
    const void* records;      /* JcmRecordHeader stream, walk by record_size */
    const float* matrix_arena;
    const char* string_arena;
    const uint8_t* pixel_arena;
    const float* float_arena;
    /* ABI 9: full capacities of the arenas behind the pointers above. The
       host builds ONE ByteBuffer per arena and rewinds its limit every
       frame instead of allocating five fresh wrapper objects per frame.
       Entries of 0 mean "unknown" (pre-ABI-9 module); the host then wraps
       at the exact length like before. */
    int64_t  records_capacity;
    int64_t  matrix_arena_capacity;
    int64_t  string_arena_capacity;
    int64_t  pixel_arena_capacity;
    int64_t  float_arena_capacity;
} JcmFrameOutput;

/* ------------------------------------------------------------------ */
/* Host services (functions the script may call while rendering)      */
/* ------------------------------------------------------------------ */

typedef struct JcmFontMetrics {
    double width;
    double ascent;
    double descent;
    double height;
} JcmFontMetrics;

struct JcmHostServices {
    void*    user;
    /* Model management: host-side handles mirror ModelJS handles. */
    int32_t (*acquire_model)(void* user, const char* model_json_path);
    void    (*release_model)(void* user, int32_t handle);
    /* GraphicsTexture lifecycle mirrors GraphicsTexture.java. */
    int32_t (*create_texture)(void* user, int32_t w, int32_t h);
    void    (*release_texture)(void* user, int32_t handle);
    /* Host TTF rasterization fallback (CJK etc.), writes BGRA8 into
       the provided buffer. Returns rows written, or -1 if the host
       font does not cover the codepoints (script should fall back
       to the built-in bitmap font). */
    int32_t (*rasterize_text)(void* user, const char* utf8, int32_t len,
                              int32_t x, int32_t y, int32_t max_w,
                              uint8_t r, uint8_t g, uint8_t b,
                              uint8_t* pixel_out, int32_t out_w, int32_t out_h);
    /* Logging mirrors console.log / console.error. */
    void    (*log)(void* user, int32_t level, const char* utf8, int32_t len);

    /* ---- v4 (ABI 4): host-built textured quad --------------------------
       Ports the JS `new DisplayHelper(slotCfg)` model construction.

       The JS pack declares a slot as a 4-vertex polygon plus a texArea;
       DisplayHelper turns that into a RawMeshBuilder(4) quad whose UVs
       map the texArea onto the polygon, and the host uploads it through
       ModelManagerJS/ModelJS.  Native scripts do the same by handing the
       4 model-space vertices to the host, which owns mesh + texture
       lifetime and returns an opaque model handle usable with
       VehicleContext::draw_car_model().

       Vertices are MODEL SPACE (same numbers as the JS `pos` array, in
       order v0→v1→v2→v3 and counter-clockwise seen from the viewer).
       UVs follow the JS convention: (0,0) top-left of the TEXTURE (not
       of texArea — the script maps texArea itself), y growing DOWN.

       `texture_handle` is a handle from create_texture(); the host
       re-reads that GraphicsTexture every frame, so the script only has
       to paint + upload it.

       Returns a model handle, or -1 when the host cannot build one
       (headless drivers) — scripts must then skip the draw instead of
       passing -1 to draw_car_model(). */
    int32_t (*acquire_quad_model)(void* user, const float* vertices_xyz,
                                  const float* uv, int32_t vertex_count,
                                  int32_t render_stage, int32_t texture_handle);

    /* ABI 7: same resource fonts, FontMetrics and drawString semantics as
       JavaScript Graphics2D. font_resource is a namespaced resource ID.
       Sizes and baseline positions are logical coordinates; the host applies
       canvas scale and optional horizontal compression without fitting glyphs
       into square cells. Returns 0 on success, -1 when unavailable. */
    int32_t (*measure_font_text)(void* user, const char* font_resource,
                                const char* utf8, int32_t len, double size,
                                double scale_x, double scale_y, JcmFontMetrics* out);
    int32_t (*draw_font_text)(void* user, const char* font_resource,
                             const char* utf8, int32_t len, double size,
                             double x, double baseline, double scale_x,
                             double scale_y, double compress_x, uint32_t argb,
                             int32_t clip_x0, int32_t clip_y0,
                             int32_t clip_x1, int32_t clip_y1,
                             uint8_t* pixels, int32_t width, int32_t height,
                             int32_t* dirty_bounds); /* x0,y0,x1,y1, exclusive */

    /* ABI 8: direct access to Java scripting APIs. UTF-8 JSON result remains
       valid on this thread until the next invocation; no Rhino evaluation. */
    const char* (*invoke_api)(void* user, const char* request, int32_t length,
                             int32_t* result_length);

    /* ---- ABI 10: deferred reclamation (off-JVM-thread free) --------------

       Hands a DEAD native block to the host's background reclaimer thread.
       Use this instead of a synchronous free()/operator delete for any
       non-trivial block that dies inside mtrCreate/mtrRender/mtrDispose:
       those entry points run on a JVM-owned thread, and a multi-megabyte
       free (arena shrink, buffer teardown) bills its full cost to the
       frame — and a Java thread that is inside native code additionally
       blocks on any GC safepoint at the call's exit boundary, so long
       native work turns into whole-JVM jitter.

       Contract (the module MUST satisfy all of it):
         - `pointer` was allocated with malloc() and is currently owned by
           the caller (or nullptr, which the hook MUST ignore);
         - the block is trivially destructible (no C++ object with a
           destructor lives in it — run module destructors first);
         - NO thread reads or writes the block after this call, including
           the host's Java-side replay of the CURRENT frame. In practice:
           defer only blocks that became dead BEFORE mtrRender filled the
           output arenas that the host is about to read.
         - `size` is informational (the host may use it for stats).

       NULL hook: host predates ABI 10 — free synchronously, exactly as
       before. The SDK's FrameRecorder follows this rule for its pixel
       arena shrink, so a module only needs this hook to benefit if it
       frees its OWN large buffers inside render(). */
    void (*defer_free)(void* user, void* pointer, size_t size);
};

/* ------------------------------------------------------------------ */
/* Module exports — every native script library defines these four.    */
/* ------------------------------------------------------------------ */

/* Returns MTR_NATIVE_ABI_VERSION. Host refuses mismatches. */
MTR_NATIVE_EXPORT uint32_t mtrNativeAbiVersion(void);

/* "vehicle" | "pids" | "eyecandy". */
MTR_NATIVE_EXPORT const char* mtrScriptType(void);

/* Script id, e.g. "example:vehicle_status". Must match mtr_custom_resources.json. */
MTR_NATIVE_EXPORT const char* mtrScriptId(void);

/* Lifecycle. Return 0 on success, non-zero error code.
   - mtrCreate:  called once per instance before the first render.
   - mtrRender:  called every frame; fill *out (host-owned frame slots).
   - mtrDispose: called when the instance is invalidated.
   The host serializes calls per instance on one background thread,
   mirroring JCM's per-script single-thread executor. */
MTR_NATIVE_EXPORT int32_t mtrCreate(const JcmFrameInput* in);
MTR_NATIVE_EXPORT int32_t mtrRender(const JcmFrameInput* in, JcmFrameOutput* out);
MTR_NATIVE_EXPORT int32_t mtrDispose(const JcmFrameInput* in);

/* Per-instance state block size: the host allocates one block of this
   size per script instance (mirrors the per-instance `state` JS object
   JCM creates for each ScriptInstance).

   ★ The block's contents must be treated as UNINITIALISED. A zero-filled
   block is NOT a valid object for a State type with a non-trivial default
   constructor (std::string, std::vector, ...): libstdc++'s std::string
   stores its buffer pointer inline (SSO), so a zeroed one dereferences
   null and segfaults — MSVC's layout happens to tolerate it, which is why
   this went unnoticed on Windows. The host MUST therefore call mtrInit()
   before the first mtrCreate()/mtrRender(). */
MTR_NATIVE_EXPORT size_t mtrStateSize(void);

/* Construct the State object inside the host's block (placement new).
   Call ONCE per instance, right after allocating the block and before
   any other entry point. Optional: hosts that cannot call it (older
   builds, third-party drivers) may omit it — the module then constructs
   lazily on the first create/render, which is equally correct, just
   slightly later. Added in ABI 6. */
MTR_NATIVE_EXPORT void mtrInit(const JcmFrameInput* in);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* MTR_NATIVE_H */
