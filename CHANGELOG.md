# Changelog

## 2.3.0-beta-2.yanyang.3

Full-codebase bug & performance audit (Java + JNI boundary + C++ bridge/SDK),
fixing everything found — including two bugs that also exist upstream.

### Critical fixes

- **F3+T never reloaded C++ scripts** (`NativeScriptManager.reload()` was
  dead code — never called from any reload path): updated `.so`/`.dll` files
  with the same script id were silently ignored, `SKIPPED`/`INPUTS` entries
  from removed packs leaked forever, and after any resource reload
  `NativeHost` kept drawing models `ModelManagerJS.reset()` had already
  freed — a use-after-free on the render thread for every native draw call.
  Reload now disposes the modules and re-parses both resource managers.
- **A malformed record could crash the JVM**: the bridge's
  `patch_frame_handles` walked the module-provided record stream without
  bounds validation (a bogus `record_count`/`record_size` read/memmoved past
  the records arena, in bridge code, outside the module crash guard). Every
  step is now validated against `records_len`, like the Java replay.
- **One malformed MTR 4 script entry aborted the whole pack**: the
  vehicle/eyecandy/lift `parseMtrResources` loops had no per-entry try/catch,
  so a bad entry dropped every remaining script of that kind AND all eyecandy
  and lift scripts of the pack file. Every loop is now isolated per entry,
  and `NativeScriptManager.load()` also catches invalid library identifiers
  (uppercase paths etc.) instead of escaping into the pack parse.
- **`Text`/`Texture` C++ draws were invisible unless every field was set**:
  the fluent builders zero-initialised their record and the struct copy in
  `draw()` clobbered the recorder defaults — unset `color` drew fully
  transparent, unset `scale` drew zero-size, unset `uv` degenerated the quad,
  unset `align` centred instead of left-aligning, and the shipped
  `examples/pids_arrivals.cpp` was affected. Defaults now come from one
  shared source of truth (`default_text_record`/`default_texture_record`).
- **Forge: native vehicle scripts captured but never drawn** — the Forge
  `VehicleResourceMixin` was never updated for the native replay, so
  `NativeVehicleDriver.invokeCar` never ran there; Forge now shares the
  Fabric handler.
- **Forge: rail culling was inverted** (`!shouldSkipCullingTask`) — rails
  within the culling limit were ALWAYS rendered (occlusion never consulted)
  and only the oversized case was tested. Also present upstream; now matches
  the Fabric semantics.
- **Vehicle record replay had no exception guard**: a truncated/malformed
  module record escaped the only try/catch in `renderOne` and killed the
  render thread; it now breaks out of the record walk safely, and a
  persistent failure enters the same 4-second backoff `NativeContentDriver`
  has always had (no more per-frame stack traces).
- **`mtrDispose` received the instance's FIRST frame forever**: with the
  reusable ABI-9 snapshot buffers the bridge's retained copy never refreshed
  (address never changed). `nDisposeInstance` now takes the live buffer from
  the driver, so dispose sees the snapshot the module actually rendered last.

### Rendering & correctness

- Shadow passes (Iris/OptiFine) drove vehicle and lift scripts TWICE per
  frame — both `RenderVehiclesMixin`/`RenderLiftsMixin` now skip when
  `MainRenderer` runs its shadow pass.
- `hideRidingVehicle` now also suppresses the script-drawn PIDS/LCD content
  of the ridden car (the mtrscripting `VehicleResourceMixin` replay ignored
  the mtrpatch cancellation).
- Lift frustum culling no longer freezes a riding player who looks away in
  third person (`movePlayer` is gated on `shouldRender`; vehicles always had
  this escape hatch, lifts did not).
- The rail-node ghost-preview cache no longer serves stale state for broken
  rails — it only answers when the chunk is actually unloaded.
- `DepartureTimerRenderer` leaked one matrix-stack entry per frame on each of
  its three early returns (data lookups moved before the `push()`).
- `PlayerEntityMixin` (shift-to-sit) now only applies to the LOCAL player —
  remote players' eye heights used to drop whenever the local player sat.
- `RenderRailwaySignMixin` no longer overrides the sign text color MTR passed
  when the JCM `textColor` is unset (`-1` passes the original through).
- Lift sound positions are floats, not truncated to block corners; JS and
  native paths now match.
- `PIDSProjectorRenderer` no longer NPEs on a null player during disconnect.
- `PIDSManager` no longer NPEs on a pack without a `pids_images` section;
  `McMeta` no longer NPEs on a missing `animation` object, treats an empty
  `frames` array as ascending order and defaults a missing frame `index`.
- `scriptLocationStack` is popped in a finally, so a throwing script no
  longer corrupts the next script's `idr()`/`include()` base path.

### Performance

- One car-position tree per vehicle per frame, shared by the native and the
  JS replay paths (previously `getVehicleCarsAndPositions()` was rebuilt per
  car — a 12-car train paid 2N+1 full tree builds per frame).
- The frustum culling box is only computed for vehicles that actually have a
  native script; script-less trains skip it entirely.
- Zero-allocation one-shot traces: `traceOnce` call sites no longer build
  reason concats, varargs arrays or whole-module-list copies per frame.
- `rasterizeText` reads the glyph mask through its `int[]` raster instead of
  per-pixel `getRGB` color-model calls.
- gfx2d `draw_round_rect`/`fill_polygon` use reusable scratch buffers
  instead of per-call `std::vector` heap churn.
- `SCRIPT_IDS`/`TRACED`/`CAR_POSITIONS` memos are pruned when their vehicle
  disappears (they used to grow forever); stable temp files per script id
  instead of one new extraction + `deleteOnExit` registration per reload.
- Failed native/font/mcmeta loads are negatively cached instead of re-read
  (and re-logged) on every query; `clearDeadInstance` no longer copies the
  whole instance map 40x/second when nothing is dead.
- `NativeApiSession`: cached `JsonParser`, O(1) class whitelist lookup,
  volatile `scriptInput`/`resourceBase` (read on the background worker),
  `Throwable` handling so Errors degrade to the JSON error contract.
- `ComplexModelStorage` dedupes requests, closes the replaced GL model and
  clears its request list; `RenderDRLAPGDoor` uses constant texture
  identifiers instead of `String.format` per door per frame.
- font5x7 now includes lowercase a-z (lowercase letters used to render
  blank on the hostless bitmap/LCD paths).

### Hardening

- `host_acquire_model`/`host_release_model` cache their `jmethodID`s and
  null-guard them (a bridge/jar mismatch used to crash the JVM with a null
  method id); `host_rasterize_text` clears its pending exception;
  `host_invoke_api` null-checks the API class/method before calling.
- C++ SDK accessors (`Stop::interchange/exit`, `Exit::destination`,
  `StopList::at`, `Train::car`, `Train::rail_progress`) bounds-check their
  indices — one script off-by-one used to read past the snapshot
  direct-ByteBuffer and SIGSEGV the JVM.
- `-Wall -Wextra` enabled for GCC/Clang module builds.
- Vehicle/eyecandy/lift script-entry loops report the failing entry id and
  skip it, instead of a generic pack-file error.

## 2.3.0-beta-2.yanyang.2

`minecraft_custom_resources.json` script entries no longer need a `language`
field: every `scriptLocations` entry is classified by the mod itself — `*.js`
(case-insensitive) is JavaScript, anything else is a C++ module
(platform-agnostic stem, full `.so`/`.dll`/`.dylib` path, or a per-platform
map). Any number of both kinds may be mixed in ONE entry, and vehicle, lift,
eyecandy and PIDS scripts all support it.

- **Mixed JS + C++ entries run both sides**: the JS parts (plus
  `prependExpressions` and `input`) concatenate into one ParsedScript while
  the C++ parts all load under the same script id — each side captures into
  its own replay registry, so draw calls never collide and a failed part
  degrades gracefully instead of breaking the other.
- **PIDS presets** (`joban_custom_resources.json`) gain the `scriptLocations`
  key with the same auto-detection, alongside the legacy `scriptFiles` key.
- A single entry may combine `.js` locations with `nativeLibrary` /
  `nativeLibraries`; `language: "cpp"` keeps the legacy native-only
  semantics (JS parts ignored with a hint) and `language: "javascript"` /
  `"js"` forces every location down the JS path (escape hatch for
  extensionless JS paths). Legacy `script_files` / `scriptFiles` keys keep
  their JS semantics for extensionless entries.
- Mixed lift rendering suppresses the built-in renderer and applies the
  riding movement exactly once per lift per frame (no double `movePlayer`);
  eyecandy block-use events now reach both the JS and the C++ side.
- Translations stay aligned with upstream JCM (also maintained by
  BugQuantumCore): all values already match upstream `2.3.0-beta.2`/`main`;
  the Irish (`ga_ie`) stub and the two new Czech keys are adopted from
  upstream `main`.

## 2.3.0-beta-2.yanyang.1

Upstream `2.3.0-beta.2` integrated into the Yanyang fork, with Yanyang-side
optimizations and the full native/MAGIC feature set retained.

### Upstream 2.3.0-beta.2 (integrated)

- MTR patch: rail preview of long-running rails outside render distance is
  preserved (rail node block states are cached per tick); the multi-item
  selection screen supports `Shift` for exclusive selection; experimental
  frustum culling for vehicles, lifts and rails layered on top of MTR's
  occlusion culling (config: `enableFrustumCullingVehicle` / `Lift` / `Rails`);
  the MTR 3 camera-lowering-by-sneaking feature is back (config:
  `allowCameraLowering`).
- Scripting API: `MinecraftClient.getCameraMode()` (F5 perspective),
  `MinecraftClient.lightAt(pos)` and a `RenderManager.drawModel(model,
  matrices, light)` overload; `VehicleWrapper.isDoorOpenable(car, pos)` /
  `isDoorOpen(...)`; `LiftWrapper.isDoorway1/2Openable()` and
  `isDoorway1/2Open()`; lift scripts that control rendering now move the
  riding player (`VehicleRidingMovement`); Czech and German language stubs.

### Yanyang integration notes & optimizations

- `isScriptRendered` was renamed upstream to `isScriptControlled`; Yanyang
  accepts **both** field names, so 2.3.0-beta.1 resource packs keep working.
- `LiftWrapper` doorway openability is now **lazily computed** on first query
  instead of eagerly in the constructor (which runs per lift per frame);
  scripts that never ask for doorway state pay zero world-query cost.
- C++ lift scripts that control rendering now ride the player with the same
  `VehicleRidingMovement` semantics as JavaScript lift scripts.
- Upstream's frustum culling (MTR built-in rendering) and Yanyang's
  `VehicleCulling` (native script execution) remain complementary layers —
  scripts keep being invoked under both, matching occlusion-culling behaviour.
- Everything from yanyang.11 retained: `getTransportType()` for MAGIC MTR
  vehicle types (JS/Java/C++), the native-script GC-stutter fix, ABI 9 with
  ABI 6/7/8 coexistence.

## 2.3.0-beta.1-yanyang.11

- Add `getTransportType()` to `VehicleWrapper` and `Stop.RouteInterchange` (JS/Java) and to `mtr::Train` / `mtr::Stop::Interchange` (C++, ABI 9). It returns the canonical transport type id — the four base modes ("train", "boat", "cable_car", "airplane") or the MAGIC MTR Addon route types ("train_metro", "train_light_rail", "train_high_speed", "train_bus", "train_tram", "train_sbahn", "train_normal") — so both script languages finally see MAGIC's vehicle types. MAGIC is integrated by optional reflection (no hard dependency); types also travel through the server→client script dataset (`routeId`, `transportType`).
- Fix the C++-script GC stutter: reuse the built StopsData graph across frames (invalidated by dataset revision or synced-payload identity), marshal snapshots with a per-vehicle reusable Builder (persistent string pool + exit memo, pooled offset arrays), cache the limited (non-full) stop data with a 1s TTL + route signature, and stop the JNI bridge from copying the 256 KB snapshot and allocating five arena ByteBuffers per vehicle per frame (one cached view per arena, rewound per frame; explicit snapshot length replaces slicing).
- Cut native memory: the per-module pixel arena now starts at 1 MB and grows on demand (previously a flat 16 MB per module).
- Keep ABI 6/7/8 modules loading alongside ABI 9 (per-ABI snapshot groups, 12/16/24-byte interchange entries).

## 2.3.0-beta.1-yanyang.10

- Fix the in-game `NoSuchMethodError` from a stale bundled JNI bridge: all four bridges rebuilt from the current `jni_bridge.cpp`, failed JNI lookups now clear and report immediately (disabling the native host instead of leaking the throwable into the script's `render()`), and a new `checkNativeBridges` build task fails the build when a bundled bridge does not embed the `NativeFrame` constructor descriptor the jar's classes declare.
- Frustum culling for native vehicle scripts: vehicles whose car envelope is outside the camera frustum no longer pay a full snapshot marshal, canvas repaint and texture upload per car.
- Render-thread allocation: the string pool returns a packed `long` instead of a new `int[2]` per entry; station exits are memoised per build pass; the driver's per-frame `IntStream` filter became a plain loop over a reused buffer.

## 2.3.0-beta.1-yanyang.9

- Copy native texture pixels directly from the frame arena into existing image storage, removing large temporary byte arrays and per-byte color conversion during vehicle and content-script uploads.
- Preserve partial-rectangle clipping, ARGB values, upload order and legacy JNI compatibility.
- Report vehicle setup, native execution, frame replay and JVM collection counters once per minute to diagnose render-thread stalls.

## 2.3.0-beta.1-yanyang.8

- Resolve interchange transport modes using the same route-name normalization as MTR simplified platform data, preserving language separators and route colors.
- Apply the fix to both fully fetched and nearby vehicle stop data. Keep missing or conflicting transport modes unknown.

## 2.3.0-beta.1-yanyang.7

- Add RouteInterchange.getTransportMode(), preserving types across server data and native snapshots.
- Execute C++ PIDS, lift and eyecandy scripts through their real rendering entry points.
- Add direct C++ access to the shared Java scripting APIs, including resources, models, graphics, sounds, Minecraft, files, networking and callbacks.
- Preserve ABI 6/7 compatibility while introducing ABI 8.
- Release per-instance resources on removal/reload and cache JNI method/class bindings.
