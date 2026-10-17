# Changelog

## 2.3.0-beta.1-yanyang.10

- Add `getTransportType()` to `VehicleWrapper` and `Stop.RouteInterchange` (JS/Java) and to `mtr::Train` / `mtr::Stop::Interchange` (C++, ABI 9). It returns the canonical transport type id — the four base modes ("train", "boat", "cable_car", "airplane") or the MAGIC MTR Addon route types ("train_metro", "train_light_rail", "train_high_speed", "train_bus", "train_tram", "train_sbahn", "train_normal") — so both script languages finally see MAGIC's vehicle types. MAGIC is integrated by optional reflection (no hard dependency); types also travel through the server→client script dataset (`routeId`, `transportType`).
- Fix the C++-script GC stutter: reuse the built StopsData graph across frames (invalidated by dataset revision or synced-payload identity), marshal snapshots with a per-vehicle reusable Builder (persistent string pool + exit memo, pooled offset arrays), cache the limited (non-full) stop data with a 1s TTL + route signature, and stop the JNI bridge from copying the 256 KB snapshot and allocating five arena ByteBuffers per vehicle per frame (one cached view per arena, rewound per frame; explicit snapshot length replaces slicing).
- Cut native memory: the per-module pixel arena now starts at 1 MB and grows on demand (previously a flat 16 MB per module).
- Keep ABI 6/7/8 modules loading alongside ABI 9 (per-ABI snapshot groups, 12/16/24-byte interchange entries).

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
