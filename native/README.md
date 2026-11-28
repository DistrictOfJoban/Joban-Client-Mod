# JCM C++ scripting SDK

ABI 8 supports vehicle, PIDS, lift and eyecandy scripts with `create`, `render`
and `dispose`, persistent state per instance, rendering, sound and resource
reload. JCM also accepts ABI 6 and 7 modules and supplies their original vehicle
snapshot layout. ABI 8 libraries require JCM Yanyang.7 or later.

## Build

```sh
cmake -S native -B native/build -DCMAKE_BUILD_TYPE=Release
cmake --build native/build --config Release --parallel 4
ctest --test-dir native/build -C Release --output-on-failure
```

Set `JAVA_HOME` to a JDK for the JVM integration tests (the bridge itself is
JDK-free since layout v2 — no jni.h anywhere). Python 3 is required
for the JVM tests. CMake produces Windows x64/arm64 DLLs, Linux x64/arm64
shared objects, and macOS x64/arm64 dylibs. Select a macOS target with
`-DCMAKE_OSX_ARCHITECTURES=x86_64` or `arm64`; build for Windows ARM64 on an
ARM64 host (MSVC Arm64 tools) or cross with the matching toolchain file.

Bundled bridge platforms (the jar ships all of them under
`assets/jcm/natives/`): `windows-x64`, `windows-arm64`, `linux-x64`,
`linux-arm64`, `macos-x64`, `macos-arm64`, `android-arm64` (NDK arm64-v8a,
16 KB page-aligned), `ios-arm64` (Mach-O, iOS/iPadOS shared). Module
libraries in resource packs use the same directory names — an ARM64 Windows
JVM cannot load an x64 DLL in-process, so packs must ship a
`natives/windows-arm64/` copy for Windows on ARM machines.

Only `jcm_native_bridge` belongs in the mod's `assets/jcm/natives/<platform>/`.
Scripts belong in their authors' resource packs. Example modules and test
modules are never included in JCM jars.

### Declaring a module in a resource pack

Put the compiled libraries under `natives/<platform>/` and list the module in
`minecraft_custom_resources.json`. No `language` field is needed — the mod
classifies each `scriptLocations` entry (`*.js` is JavaScript, anything else
is a C++ module), and any number of both may be mixed in one entry:

```json
"vehicleScripts": [{
    "id": "cst_sp1900_lcd",
    "prependExpressions": ["print('Hello world')"],
    "scriptLocations": [
        "mtr:my_pack/js/sp1900_lcd/main.js",
        "mtr:my_pack/natives/sp1900_train_num"
    ],
    "input": { "yearEra": 2004 },
    "dataFetchMode": "ALL"
}]
```

An entry may also be a full library path (`.so`/`.dll`/`.dylib`, bound to the
OS its extension implies) or a per-platform map
(`{"windows": ..., "linux": ..., "macos": ...}`; arch-specific keys such as
`macos-arm64` win over the generic key). The pre-beta-2 declaration
(`"language": "cpp"` + `"nativeLibrary"`/`"nativeLibraries"`) keeps working
and may be combined with `.js` entries. Lifts, eyecandies and PIDS presets
support the same mixed declarations.

## Script types and state

Include `<mtr/script.hpp>`, inherit the appropriate base and export it using
one registration macro:

| Resource | Base | Registration | Wrapper |
| --- | --- | --- | --- |
| Vehicle | `mtr::VehicleScript<State>` | `MTR_REGISTER_VEHICLE_SCRIPT` | `mtr::Vehicle` (alias: `mtr::Train`) |
| PIDS | `mtr::PidsScript<State>` | `MTR_REGISTER_PIDS_SCRIPT` | `mtr::Pids` |
| Lift | `mtr::LiftScript<State>` | `MTR_REGISTER_LIFT_SCRIPT` | `mtr::Lift` |
| Eyecandy | `mtr::EyecandyScript<State>` | `MTR_REGISTER_EYECANDY_SCRIPT` | `mtr::EyeCandy` |
| PSD (platform screen door glass / top) | `mtr::PsdScript<State>` | `MTR_REGISTER_PSD_SCRIPT` | `mtr::Psd` |

The vehicle data wrapper follows MTR 4 terminology (`VehicleWrapper`,
`vehicleScripts`); the MTR 3-era name `mtr::Train` is kept as a source alias.

PSD scripts (ABI 12; station info since ABI 13) are bound per-block in-game with
the PSD Script Editor item (屏蔽门脚本编辑器) — declare them under the `psdScripts`
category of `mtr_custom_resources.json` with `"type": "glass"` or `"top"` (see the
main README). The `mtr::Psd` wrapper carries the platform, the wall's door
units, arrivals with door-open predictions, every route calling at the platform
with its full interchange-rich stop list, and (ABI 13, `psd.stop()`) the
complete info of the station itself — fare zones, interchange routes, walkable
connecting stations, exits and platform numbers; drawing uses the same
`Text` / `Texture` / `Rect` records as PIDS. Example:
`examples/psd_glass.cpp`.

Place textures, models, callbacks and mutable instance data in `State`.
Members of the script class are shared by every instance of that module.
The host constructs `State` before `create` and destroys it after `dispose`.
Commands issued in `create` are retained for the first render. Instances that
disappear or are no longer rendered are retired after five seconds; reload
disposes all instances. Keep background callbacks in `State` until disposal.

## Complete Java API access

`mtr::Api` calls the same public Java API objects that JavaScript uses. It does
not evaluate JavaScript. The compact C++ snapshot and drawing helpers provide
an additional path for frequently used operations; they are not the complete
API surface. Methods or fields absent from a compact wrapper are available
through `Api`.

```cpp
#include <mtr/script.hpp>

struct State { mtr::Api::Object tracker; };
struct Example : mtr::PidsScript<State> {
    static constexpr auto ID = "example:arrivals";
    void create(mtr::PidsContext& ctx, State& state, const mtr::Pids&) override {
        mtr::Api api(ctx.input());
        state.tracker = api.create("StateTracker");
        api.context().call("setZOrderStep", {0.0002});
    }
    void render(mtr::PidsContext& ctx, State& state, const mtr::Pids&) override {
        mtr::Api api(ctx.input());
        auto arrivals = api.wrapper().call("arrivals");
        if (arrivals.call("size").as<int>() == 0) return;
        auto arrival = arrivals.call("get", {0});
        if (arrival.is_null()) return;
        auto text = api.call("Text", "create");
        text.call("text", {arrival.call("destination")});
        text.call("pos", {2.0, 3.0});
        text.call("draw", {api.context()});
    }
};
MTR_REGISTER_PIDS_SCRIPT(Example)
```

| JavaScript operation | C++ operation |
| --- | --- |
| `Resources.id("example:file")` | `api.call("Resources", "id", {"example:file"})` |
| `new Matrices()` | `api.create("Matrices")` |
| `ctx.getRenderManager()` | `api.context().call("getRenderManager")` |
| `obj.method(a, b)` | `obj.call("method", {a, b})` |
| `obj.field` | `obj.get("field")` |
| `obj.field = value` | `obj.set("field", value)` |
| `TransportMode.BOAT` | `api.get("TransportMode", "BOAT")` |
| `SCRIPT_INPUT` | `api.input()` (JSON value) |
| `console.log(...)` / `print(...)` | `api.root("console").call("log", {...})` |
| `Timing.delta()` | `api.root("Timing").call("delta").as<double>()` |
| `array.length` / `array[i]` | `array.call("length")` / `array.call("get", {i})` |
| `include(...)` | C++ `#include` at compile time |

Use `.as<T>()` for primitive results, `.is_null()` for nullable values and
`.enum_name()` for Java enum names. Java objects are reference counted by the
session and handles use decimal strings. Numeric IDs retain all 64 bits.
Objects are reusable; fields and methods are resolved using their actual Java
types, including inherited public members and standard Java collections.

Common APIs cover console, Resources, ModelManager/Model/RawModel/RawMeshBuilder/
DynamicModelHolder, Vector3f, Matrices, GraphicsTexture and Java2D,
RenderManager/QuadDrawCall, SoundManager/TickableSoundInstance, MinecraftClient,
PlayerEntity/ItemStack/VanillaText/VoxelShape, Files, Networking, BackgroundWorker,
TextUtil/Timing/StateTracker/CycleTracker/RateLimit, DataReader, MTRClientData,
TSC data classes, and DisplayHelper. AWT classes use their fully qualified names,
such as `java.awt.Color`, `java.awt.Font`, `java.awt.BasicStroke` and
`java.awt.geom.AffineTransform`. GraphicsTexture's public `graphics` and
`bufferedImage` fields expose actual Java2D objects.

`Resources.idr` resolves against the declared native library's resource path.
With multiple library declarations each module uses its own declaration path.
`Resources.manager()` remains null, as in JavaScript on MTR 4. MTRClientData's
internal fields have the same version stability limits as their JavaScript
counterparts. Existing deprecated methods remain accessible. The upstream
documentation explicitly labels vehicle connector `drawConnModel` and
`drawConnStretchTexture` unimplemented; this release does not invent connector
behavior for them.

The callable Common APIs and PIDS/Eyecandy/vehicle members are recorded in
`bench/api-coverage.json`. Lift coverage comes from its shipped Java wrappers,
because the referenced documentation has no Lift API page. Check the compiled
classes with:

```sh
python native/bench/api_coverage.py --classes fabric/build/classes/java/main --jar path/to/mtr.jar
```

## PIDS, Lift and Eyecandy

`api.context()` and `api.wrapper()` refer to the same type-specific context and
wrapper supplied to JavaScript. PIDS supports Text, Texture and **Rect**
(the JS class was renamed from `Rectangle` in upstream v2.3.0-prerelease.2 —
`mtr::Rect` draws the shared `mtr:white.png` quad exactly like
`RectangleWrapper`), fonts, overflow, lighting, matrices, z ordering, hidden
rows, custom messages, target platforms, station and complete arrivals/car
data. Lift supports its render/sound managers, visibility, offsets, angles,
dimensions, doors, direction, riding state and floors. Each floor exposes its
index, number, description, position and current/target flags.

All three PIDS draw builders (Text/Texture/Rect) carry the JS wrappers'
`naturalLight()` (world-light rendering, in the record-header flags byte —
records from older SDKs predate the flag and render with full brightness as
before) and `renderType("LIGHT_2")` (QueuedRenderLayer name; unknown names
throw like `valueOf`). Eyecandy exposes `facing_angle()` — the upstream
`facingAngle()` port (`Direction.asRotation()` degrees).

The `Gfx2D` rasterizer mirrors `GraphicsTexture.fullAA()/noAA()` (upstream
v2.3.0-prerelease.2): `full_aa()` enables one-pixel coverage
anti-aliasing on ovals, rounded rects, strokes and polygon fills, `no_aa()`
restores the default hard edges; text remains host-TTF rendered.
`mtr::StateTracker<T>` ports `StateTrackerJS` including the upstream change
that `setState` returns whether this is the first frame in the new state —
pass the snapshot's `game_time_millis()` as the clock.

Eyecandy supports its model, transform, facing, full brightness, redstone,
crosshair status, custom configuration, render/sound managers, outline and
collision shapes, and block-use event details:

```cpp
auto event = api.context().call("events").get("onBlockUse");
if (event.call("occurred").as<bool>()) {
    auto detail = event.call("detail");
    auto player = detail.call("player");
    auto item = detail.call("item");
}
```

Shapes persist across frames. Empty collision shapes are supported; nonempty
collision shapes retain the Java limit of 1.5 blocks in height. Native drawing
records use the same in-world transformations as the JavaScript renderer.

## Door-opening direction (ABI 11)

`vehicle.getDirectionOfDoorOpening()` reports which side(s) of the vehicle the
doors can open on, as one of `"not"`, `"left"`, `"right"`, `"all"`. While the
vehicle dwells at a platform the value is phase-independent (not-yet-open,
partially open, closing and fully-closed doors all report the fully-open
answer); while it runs on the mainline it *predicts* the next stop by laying
the vehicle out at its stopping point and running MTR's own
`RenderVehicleHelper#canOpenDoors` block scan there, per car. As soon as one
car on a side finds an openable platform block, that whole side counts as
openable. Left/right are relative to the vehicle head (bogie-1 end), so
neither travel direction nor reversing flips them. The call is purely
informational — MTR still owns which doors actually open.

A client only holds blocks for loaded chunks, so the predictive scan alone
would only start answering once the next stop enters view distance — roughly
when the train enters the platform track. JCM therefore keeps a **server-side
per-route memory** ("scan once, answer anywhere"): the first time a server
starts with this JCM version on a save, it scans every platform that at least
one route calls at — unloaded chunks included, which is exactly the
capability a client never had — resolving each platform's rail the same way
MTR's own core does (the `positionsToRail` lookup behind
`SavedRailBase#isInvalidSavedRail`; beware — `Rail.getHexId()` and
`Platform.getHexId()` are unrelated methods that merely share a name: the
former encodes the rail's two endpoint coordinates, the latter the platform's
numeric id, so no hex-id matching between the two can ever succeed) and
testing the same blocks MTR's own `RenderVehicleHelper#canOpenDoors` would
look at, storing the two openable sides per route under
`<world>/mtr/<dimension>/routes-door-directions/` (mirroring MTR's routes
sharding; a `scan-complete-v2` marker in that folder prevents any later boot
from rescanning the whole save; a scan that finds no scannable platform
writes no marker, so a later fixed environment rescans automatically). Sides
are stored in a frame canonicalized to the platform rail's intrinsic
direction, so forward, reversed and opposite-direction trains all read the
same entry. When platform blocks are placed, removed or replaced later —
WorldEdit and Axiom batches included — a mixin on the server's block setter
notices, coalesces the edits by chunk and platform, and re-scans a bounded
number of platforms per tick, so the stored answers stay fresh without ever
spiking a tick. When `getDirectionOfDoorOpening()` needs an answer the local
scan can't see, the client asks the server, which replies from the stored
data; until the first answer arrives (or on a server without this JCM
version) the client keeps its own in-view scan as the fallback. Note for
server operators: install the JCM jar on the server for mid-journey
predictions; a client-only install falls back to the in-view-scan behavior.

```cpp
switch (vehicle.doorDirection()) {                       // typed (mtr::DoorDirection)
    case mtr::DoorDirection::LEFT:  rotateLeftLcd();  break;
    case mtr::DoorDirection::RIGHT: rotateRightLcd(); break;
    case mtr::DoorDirection::ALL:   rotateBothLcds(); break;
    default: /* NOT */ showFullRouteMapOnBoth();
}
if (vehicle.getDirectionOfDoorOpening() == mtr::door_direction::ALL) { /* ... */ }
```

The string form matches the JS wrapper verbatim; JavaScript calls the same
method directly: `vehicle.getDirectionOfDoorOpening()`. Snapshot field:
`JcmVehicleSnapshot.door_direction` (byte, ABI 11; values 0..3 as in
`MtrDoorDirection`) — it replaces the former `_pad0`, so older modules and
the ABI 9/10 layouts are unaffected.

## Stop status / 暂缓开通 (ABI 14)

`stop.getStopStatus()` reports how the train serves a stop, as one of
`"normal"`, `"disabled"`, `"skipped"`. The vocabulary is the FangSu (方速)
addon's 暂缓开通 / RouteStopStatus model: `"disabled"` means the station is in
deferred-opening state (暂缓开通) or the platform is crossed out — trains pass
through; `"skipped"` means the train takes the station's passing (越行)
track. `stop.isStationDeferred()` answers the station-level question
directly, and `stop.isDeferredStop()` is the display convenience ("strike
this stop through") — `stopStatus() != NORMAL || isStationDeferred()` —
exactly the verdict FangSu's own displays use.

The integration is strictly optional and reflection-based: with FangSu
installed the values come from its data (the `fangsu_stop_status` /
`fangsu_station_deferred` fields that ride along MTR's own client sync);
without it every stop reports `"normal"` / not deferred — the same return
values as the non-deferred case, so scripts written against the API work
unchanged on any mod list. FangSu takes care of the JavaScript surface
itself (its server-side integration also filters non-normal stops out of
the vehicle stop lists, so vehicle scripts mostly see `"normal"`); the PSD
route-strip stop lists are **not** filtered and carry live statuses — a
glass/top-panel route strip is the natural consumer.

```cpp
const mtr::PsdStopList stops = route.stops();
for (int32_t i = 0; i < stops.size(); i++) {
    const mtr::PsdStop stop = stops.at(i);
    const uint32_t color = stop.isDeferredStop() ? 0x555555 : routeColor;
    /* ... or compare the string form: */
    if (stop.getStopStatus() == mtr::stop_status::DISABLED) drawStrikethrough(i);
}
```

Snapshot fields: `JcmStop.stop_status` and `JcmStop.station_deferred`
(bytes, ABI 14) — they replace the former `_pad0[2]` in every layout group,
so older modules and the ABI 6–13 layouts are unaffected.

## Weather (ABI 15)

Every snapshot now carries the client world's weather: `weather()` returns
the typed `mtr::Weather` (`CLEAR` / `RAIN` / `THUNDER`) and `getWeather()`
returns the string id `"clear"` / `"rain"` / `"thunder"` — the exact string
the JS wrappers' `getWeather()` returns, so JS and C++ scripts share layout
logic. Without a client world the value is always `"clear"`.

```cpp
if (psd.getWeather() == mtr::weather::RAIN) drawWetFloorNotice();
switch (vehicle.weather()) {
    case mtr::Weather::RAIN:    drawRainOverlay(); break;
    case mtr::Weather::THUNDER: drawThunderOverlay(); break;
    default: break;
}
```

Snapshot fields (bytes, ABI 15): `JcmVehicleSnapshot.weather` (offset 165,
former `_pad1[0]`), `JcmPidsSnapshot.weather` (63, former `_pad0`),
`JcmEyecandySnapshot.weather` (67, former `_pad0`),
`JcmLiftSnapshot.weather` (127, former `_pad0`); `JcmPsdSnapshot` — the
only struct with no spare byte — grows 128 → 136 with `weather` at offset
128. All are zeroed padding in the ABI ≤ 14 layouts, so older modules and
the vehicle group-9 blob sharing are unaffected.

JavaScript calls the same-named method directly (`pids.getWeather()`,
`vehicle.getWeather()`, `psd.getWeather()`, `lift.getWeather()`,
`eyecandy.getWeather()`); the value comes straight from the client world
(`isThundering()` beats `isRaining()`, mirroring MTR's own weather PIDS
components).

## Express / hidden-route flags (ABI 16)

The FangSu (方速) service model feeds every arrival, every PSD route strip
and the vehicle header with two flag bits — `mtr::service_flags::EXPRESS`
(the arrival runs a ServiceStop express service, it overtakes non-express
stations) and `HIDDEN_ROUTE` (the route only exists in FangSu's
hidden-route client cache):

```cpp
if (arrival.isExpress()) drawExpressStyle();
if (vehicle.isHiddenRoute()) drawHiddenRouteHint();
```

Without the FangSu addon both bits are always 0 — the exact
non-express / non-hidden answers (same soft-dependency contract as the
ABI 14 stop statuses). Fields: `JcmArrival.flags` (offset 59, former
padding), `JcmPsdRoute.route_flags` (13), `JcmVehicleSnapshot.service_flags`
(166, former `_pad1[1]`).

## Signal state (ABI 16)

`JcmVehicleSnapshot.signal_state` (offset 167, former `_pad1[2]`) carries
the signal state ahead of a vehicle: `vehicle.signalState()` returns
`mtr::SignalState::UNKNOWN / CLEAR / CAUTION / DANGER` and
`getSignalState()` the string id. The host side resolves it through the
`NativeSignalState` provider registry — a public extension point
(`com.lx862.jcm.nativeapi.NativeSignalState.set(vehicleId, state)`) that
signal mods (mtr-nte bridges and friends) push through; without a
provider every vehicle reports "unknown", byte-identical to the
pre-ABI-16 padding.

```cpp
if (vehicle.signalState() == mtr::SignalState::DANGER) brakeLightsOn();
```

## Styled model draws (ABI 16)

`JcmDrawModel` grows 24 → 28 bytes with a trailing ARGB `tint` field plus
two new header flag bits: `MTR_RECORD_FLAG_EMISSIVE` (draw at the host's
full-brightness light — night headlights, lit signs) and
`MTR_RECORD_FLAG_TRANSLUCENT` (draw through the translucent material
pass — window glass; alpha tints below 0xFF force it automatically):

```cpp
ctx.draw_car_model(model, car, mats,
                   mtr::VehicleContext::ModelStyle()
                       .tint(0x80FF8000)
                       .emissive()
                       .translucent());
ctx.draw_model(lamp, nullptr, 0, true, false);          /* eyecandy / crossing */
```

Records emitted by ABI ≤ 15 modules stay 24 bytes and keep rendering
byte-for-byte — hosts accept both sizes (walked by `record_size`, tint
read only when present).

## Block scripts: crossing / fare machine / announcement (ABI 16)

Three new script kinds anchor on FangSu blocks through client-local
bindings (edited with the PSD Script Editor item — right-click the
block, pick a script):

| Kind | Resource id | Anchor | Registration |
|---|---|---|---|
| Level crossing | `MTR_RESOURCE_LEVEL_CROSSING` (5) | FangSu `level_crossing` | `MTR_REGISTER_LEVEL_CROSSING_SCRIPT` |
| Fare machine | `MTR_RESOURCE_FARE_MACHINE` (6) | FangSu `ticket_barrier` / `ticket_machine` | `MTR_REGISTER_FARE_MACHINE_SCRIPT` |
| Announcement | `MTR_RESOURCE_ANNOUNCEMENT` (7) | FangSu `announcement_speaker` | `MTR_REGISTER_ANNOUNCEMENT_SCRIPT` |

- The crossing snapshot carries the geometry summary (primary track yaw)
  plus a `JcmCrossingApproach[]` pool (vehicle id, route, distance,
  speed, arrival estimate) and host-derived `warning_state` /
  `barrier_target` — the script animates its barrier arms itself.
- The fare machine snapshot carries the barrier state (open flag +
  the host's time-derived wing animation), the fares, the client
  player's `mtr_balance` and the nearest platform's arrivals.
- The announcement snapshot carries the speaker's route/platform
  bindings, lead seconds and an arrival pool; the script plays audio
  through the unified sound records — `ctx.play_sound` (positional) and
  `ctx.play_local_sound` (riding players only) — the same
  ScriptSoundManager the PIDS path uses.

Declared in `mtr_custom_resources.json` under `levelCrossingScripts` /
`fareMachineScripts` / `announcementScripts`, same native declaration
forms as every other kind. Without FangSu the kinds are inert.

## Developer tooling (ABI 16 era)

- **Hot reload** (`scripting.nativeHotReload` config, off by default): a
  watcher fingerprints `resourcepacks/` (and the dev `resources/`
  folder); a changed `.so`/`.js` triggers the same reload F3+T performs
  — rebuild your module, see it without leaving the game.
- **CMake scaffold** (`template/` in the SDK):
  `cmake -B build -DJCM_SDK=<sdk>` builds a ready pids/vehicle/crossing
  skeleton project.
- **Perf panel**: the debug overlay's native section now shows the
  per-module average `mtrRender` time, host crossings (Java upcalls)
  per second, snapshot marshal bytes per second and render calls per
  second.
- **JS → C++ migration**: `docs/js-to-cpp.md` maps the wrapper APIs
  one-to-one; `tools/js2cpp.py` performs the mechanical renames
  (forEach loops, Text chains, property→method) and leaves TODO
  markers where a human must decide.

## Persistent script data store ("数据存储")

`context.getDataStore()` returns a JSON-file-backed key-value store scoped
to the **script id** — shared by every instance of the script and persisted
across game restarts under `<run dir>/data/mtrscripting/stores/`. Use it
for counters, remembered destinations, or values an administrator edits by
hand. JS scripts call `ctx.getDataStore()` and get a plain Java object;
C++ scripts reach the very same object through the Api bridge:

```cpp
mtr::Api api(ctx.input());
mtr::Api::Object store = api.context().call("getDataStore");
double counted = store.call("getNumber", {"trains.counted", 0.0}).as<double>();
store.call("set", {"trains.counted", counted + 1});
const std::string dest = store.call("getString", {"last.destination", ""}).as<std::string>();
bool known = store.call("has", {"setup.done"}).as<bool>();
store.call("delete", {"last.destination"});
```

Members: `get(key)` (decoded JSON: string/number/boolean/null/list/map),
typed getters `getString(key, def)` / `getNumber(key, def)` /
`getBoolean(key, def)`, `set(key, value)`, `has(key)`, `delete(key)`
(returns whether the key existed), `size()`, `keys()`, `clear()`. Values
are JSON data — strings, numbers, booleans, null, arrays and nested
objects (JS scripts may pass JS objects/arrays directly). The store is
size-capped (256 keys / 64 KB serialized / 128-char keys / 32-level
nesting) so a runaway script cannot grow the data directory unchecked;
violations throw to the script's error path.

Writes are debounced (one flush per store per ~1.6 s from the client tick,
plus flushes on resource reload and JVM shutdown) and atomic
(temp file + move), so a crash mid-write degrades to the previous good
file. Per-**instance** state (per PIDS block, per PSD panel) is addressed
by keying with the identity the wrapper already exposes:

```cpp
std::snprintf(key, sizeof(key), "x%ld.y%ld.z%ld", psd.block_pos(0), psd.block_pos(1), psd.block_pos(2));
```

The store lives in the run directory (like `Files.readData/saveData`
data), not in the world save — the same script id shares data across
worlds and servers.

## Interchange transport mode and type

JavaScript now supports `interchange.getTransportMode()` plus
`interchange.getTransportType()` (and `vehicle.getTransportType()`). C++ can
use either:

```cpp
auto mode = interchange.call("getTransportMode");
if (!mode.is_null()) {
    auto name = mode.enum_name(); // TRAIN, BOAT, CABLE_CAR, AIRPLANE
    auto maxLength = mode.get("maxLength").as<int>();
}
auto type = interchange.call("getTransportType").as<std::string>();
```

The compact vehicle snapshot provides the same data natively with zero JNI:

```cpp
if (train.getTransportType() == mtr::transport_type::TRAIN_METRO) { /* ... */ }
for (int i = 0; i < stop.interchange_count(); i++) {
    mtr::Stop::Interchange ic = stop.interchange(i);
    if (ic.getTransportType() == mtr::transport_type::TRAIN_HIGH_SPEED) { /* ... */ }
}
```

`getTransportMode()` covers the four base modes; `getTransportType()`
additionally resolves the **MAGIC MTR Addon** route types. Base ids are the
lowercase mode names (`"train"`, `"boat"`, `"cable_car"`, `"airplane"`, plus
`"unknown"` for older data); MAGIC contributes `train_normal`,
`train_light_rail`, `train_high_speed`, `train_metro`, `train_bus`,
`train_tram` and `train_sbahn` (see the `mtr::transport_type` constants).
Java scripts see the exact same strings through
`VehicleWrapper.getTransportType()` and
`Stop.RouteInterchange.getTransportType()`, so JS and C++ scripts can share
layout logic.

MAGIC types are resolved wherever the route id is known: locally against
`config/MAGIC/configs/route_types.json` when the addon is installed on the
client, and through the synced script dataset (`routeId` + `transportType`
fields) when the server has MAGIC and the client does not. Type conflicts
inside one (color, name) group fall back to the agreed base mode; mixed base
modes stay `UNKNOWN`/`"unknown"` exactly as before. Full remote stop data
includes actual in-station and connecting route modes and types. An older
server that does not send route metadata yields null in the Java
`getTransportMode()` and `UNKNOWN` in the compact C++ view. Install the
updated JCM on the server as well to obtain full metadata for remote
stations. ABI 6/7 libraries keep their original 12-byte interchange entry;
ABI 8 keeps 16 bytes without the type fields; ABI 9 uses 24 bytes.

## Background work and resources

`Api::Callback` wraps a C++ function as a Java functional interface, such as
Runnable, Consumer or Function. Retain it for as long as Java may call it:

```cpp
state.task = std::make_unique<mtr::Api::Callback>([api](mtr::Json) {
    auto response = api.call("Networking", "fetch", {"https://example.org/data"});
    // Publish data through your own synchronized state.
    return mtr::Json();
});
api.root("BackgroundWorker").call("submit", {*state.task});
```

Render and asynchronous callbacks can run concurrently; synchronize shared
mutable C++ state. Reload stops callbacks and waits for active calls before
destroying state or unloading code. Use `DynamicModelHolder.uploadLater` for
GPU models prepared in background work. Direct `ModelManager.upload` retains
the Java render-thread requirement. Close textures, models and other owned
resources in `dispose`; session cleanup closes remaining constructed resources.

Avoid resource reads, HTTP requests and unnecessary Java crossings in every
render. Cache resource objects in state, use the compact snapshot for frequent
reads, and upload textures only when their content changes.

### Keeping the hot path native (GC hygiene)

The snapshot marshal is the ONLY mandatory Java crossing per frame; drawing
goes through the record stream, and the SDK's native helpers
(`mtr::Text`, `mtr::GraphicsTexture`, `mtr::gfx2d` canvas, `mtr::Matrices`)
do the rest without touching the JVM. `mtr::Api` is intended for DATA that is
not in the snapshot — call it from `create()`, from background tasks, or at
most a few times per second, never per draw call: every `Api` call is a
JSON encode + reflection + JSON decode round-trip, and doing that per frame
per vehicle is what used to produce a periodic GC stutter.

The host does its part: host services travel the FFM upcall vtable or the
poller's shared-page SVC requests (never a JVM-internal invocation path),
the five arena ByteBuffers are created once per module and rewound per frame,
the snapshot is marshalled by a reusable builder whose string pool and exit
memo persist across frames, the built stop graph is cached until the
underlying data revision changes, the bridge passes the live direct snapshot
buffer (retaining a copy only for dispose) instead of memcpy'ing 256 KB per
vehicle per frame, and the pixel arena starts at 1 MB and grows on demand
instead of a flat 16 MB per module. These reduce concrete overhead; no
universal FPS multiplier is claimed.

Tests cover BOTH JNI-free invocation planes (Panama FFM and the
shared-memory poller) for all four script kinds, UTF-8, 64-bit IDs, enums,
Java2D, synchronous/asynchronous callbacks, state isolation,
disposal/reload, PIDS/lift/eyecandy snapshot fields, create-time shapes,
and ABI 6/7 loading.
Game-specific visibility, user content and final appearance still require
testing in a running Minecraft instance.

## Java ↔ C++ transports ("Java 与 C++ 通信方式")

Since 2.3.0-beta-2.yanyang.6 (layout v2) the mod contains **no JNI at all**.
The JVM-side host exchanges per-frame data with the script modules through
three selectable transports (Mod Menu → Joban Client Mod Config →
Scripting, config key `scripting.javaCppCommunication`) — and EVERY one of
them runs JNI-free on EVERY JVM:

| Option | What it does | Config description |
| --- | --- | --- |
| **共享内存** (default) | Snapshot marshalling writes into a window of a file-backed shared region (`FileChannel#map` → `MappedByteBuffer`); C++ reads Java's data straight out of that mapping. The per-frame trigger is an empty-argument **FFM downcall** on JDK 22+, or — on JDK 16..21 — not even that: the bridge's constructor-started **poller thread** discovers the region through a filesystem handshake, opens the very same file and serves the request from the shared pages. The output arenas are published as raw descriptors, which Java re-binds into *reused* `ByteBuffer` views each frame. Zero copies, zero hot-path allocations, zero native-call APIs on old JVMs. | 性能最好，但小概率出现稳定性问题 |
| **直连调用** (config name `JNI`, kept for old config files) | The snapshot and the five output arenas ride the per-frame call: the `jcm_module_render` FFM downcall on JDK 22+. Older JVMs degrade to the shared-memory path above. | 快照随每次调用传输（JDK 22+ 走 FFM 调用）—— 旧 Java 自动降级共享内存轮询平面，同样全程零 JNI |
| **Panama FFM** | The everything-through-FFM selection on JDK 22+: `SymbolLookup.libraryLookup` loading (no `System.load`), `jcm_module_*` lifecycle downcalls, the per-frame `jcm_shm_kick` trigger and the script→Java host services through the `JcmFfmHostVTable` upcall stubs (`Linker.upcallStub`). Older JVMs use the JNI-free shared-memory poller plane automatically. | JDK 22+ 上全程零 JNI；旧 JDK 使用零 JNI 的共享内存轮询平面 |

The two **invocation planes** behind every transport:

* **Panama FFM plane** (JDK 22+; 21 with `--enable-preview` for the test
  harness): `libraryLookup` + `jcm_*` downcalls + the upcall vtable.
* **Shared-memory poller plane** (any JDK ≥ 16 — the production
  configuration of Minecraft 1.17.1-1.20.4 on stock Java 16/17/21):
  the bridge is `System.load`ed purely to run its CONSTRUCTOR, which
  starts a background poller thread. The poller finds the region through
  handshake pointer files (`jcm_shm_hs_<pid>`) in the temp directories,
  maps the region file itself, and then serves **everything** from the
  shared pages: module lifecycle commands, per-frame renders, the
  script→Java host services (posted back as SVC requests that the
  waiting Java thread answers through the very same raw-address adapters
  the FFM vtable uses) and the Java→C++ api re-entry (a small CBQ ring
  that the poller drains in every wait loop). No native call crosses in
  either direction — see `shm_channel.h` (layout v2).

The channel is fully self-contained: the region lives in the system temp
directory, is unmapped (`invokeCleaner`) and deleted on every resource
reload (the shutdown command poisons the region header first so the
poller can never re-attach a stale mapping), driver snapshot windows are
freed on instance dispose/tick-out/reset, and any failure (missing bridge
exports on an old binary, a full region, a JVM without the required
internals) degrades transparently with a one-line log instead of failing
scripts.

Transport layout: `native/include/mtr/shm_channel.h` (mirrored by
`com.lx862.jcm.nativeapi.NativeShm`).
