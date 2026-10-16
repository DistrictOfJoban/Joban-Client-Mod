# Changelog

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
