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

Set `JAVA_HOME` to a JDK for the JNI bridge and JVM tests. Python 3 is required
for the JVM tests. CMake produces Windows x64 DLLs, Linux x64 shared objects,
and macOS x64/arm64 dylibs. Select a macOS target with
`-DCMAKE_OSX_ARCHITECTURES=x86_64` or `arm64`.

Only `jcm_native_bridge` belongs in the mod's `assets/jcm/natives/<platform>/`.
Scripts belong in their authors' resource packs. Example modules and test
modules are never included in JCM jars.

## Script types and state

Include `<mtr/script.hpp>`, inherit the appropriate base and export it using
one registration macro:

| Resource | Base | Registration | Wrapper |
| --- | --- | --- | --- |
| Vehicle | `mtr::VehicleScript<State>` | `MTR_REGISTER_VEHICLE_SCRIPT` | `mtr::Train` |
| PIDS | `mtr::PidsScript<State>` | `MTR_REGISTER_PIDS_SCRIPT` | `mtr::Pids` |
| Lift | `mtr::LiftScript<State>` | `MTR_REGISTER_LIFT_SCRIPT` | `mtr::Lift` |
| Eyecandy | `mtr::EyecandyScript<State>` | `MTR_REGISTER_EYECANDY_SCRIPT` | `mtr::EyeCandy` |

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
wrapper supplied to JavaScript. PIDS supports Text, Texture and Rectangle,
fonts, overflow, lighting, matrices, z ordering, hidden rows, custom messages,
target platforms, station and complete arrivals/car data. Lift supports its
render/sound managers, visibility, offsets, angles, dimensions, doors,
direction, riding state and floors. Each floor exposes its index, number,
description, position and current/target flags.

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

## Interchange transport mode

JavaScript now supports `interchange.getTransportMode()`. C++ can use:

```cpp
auto mode = interchange.call("getTransportMode");
if (!mode.is_null()) {
    auto name = mode.enum_name(); // TRAIN, BOAT, CABLE_CAR, AIRPLANE
    auto maxLength = mode.get("maxLength").as<int>();
}
```

The compact ABI 8 vehicle snapshot also provides
`mtr::Interchange::getTransportMode()`, returning `mtr::TransportMode`.
Full remote stop data includes actual in-station and connecting route modes.
An older server that does not send route mode metadata yields null in the Java
API and `UNKNOWN` in the compact C++ view. Install the updated JCM on the server
as well to obtain full metadata for remote stations. ABI 6/7 libraries keep
their original 12-byte interchange entry.

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
reads, and upload textures only when their content changes. JNI bindings and
Java method lists are cached, buffers are reused, snapshot copies use the written
length, and native draw records are aligned and bounded. These reduce concrete
overhead; no universal FPS multiplier is claimed.

Tests cover JNI execution for all four script kinds, UTF-8, 64-bit IDs, enums,
Java2D, synchronous/asynchronous callbacks, state isolation, disposal/reload,
PIDS/lift/eyecandy snapshot fields, create-time shapes, and ABI 6/7 loading.
Game-specific visibility, user content and final appearance still require
testing in a running Minecraft instance.
