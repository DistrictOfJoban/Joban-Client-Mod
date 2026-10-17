JCM Yanyang.11 — MAGIC MTR vehicle types for `getTransportType()` and no more native-script GC stutter.

**兼容 MAGIC MTR Addon / MAGIC MTR Addon compatibility**

- `getTransportType()` now returns MAGIC MTR Addon vehicle types — `train_metro`, `train_light_rail`, `train_high_speed`, `train_bus`, `train_tram`, `train_sbahn`, `train_normal` — as well as the four base modes `train` / `boat` / `cable_car` / `airplane`. Available from JavaScript and Java (`vehicle.getTransportType()`, `stop.routeInterchanges[i].getTransportType()`) and natively from C++ (`train.getTransportType()`, `stop.interchange(i).getTransportType()`, ABI 9).
- MAGIC is integrated via optional reflection: **no hard dependency**. Without MAGIC installed, everything keeps working and base ids are returned.
- `routeId` + `transportType` travel through the server→client script dataset, so mixed old/new JCM versions still resolve types; `getTransportMode()` is unchanged.

**C++ 脚本不再每 500ms 卡一下 / the native-script GC stutter is gone**

- The built StopsData graph is now reused across frames (invalidated by dataset revision or synced-payload identity) — previously hundreds of Stop/RouteInterchange/list objects were allocated per vehicle per frame, driving periodic Minor GC pauses.
- Snapshot marshalling uses a per-vehicle reusable Builder: persistent string pool, identity exit memo, grow-only offset arrays.
- The JNI bridge no longer memcpy's a 256 KB snapshot per vehicle per frame; the five arena `ByteBuffer`s are cached per module as global-ref views and simply rewound each frame (explicit snapshot length replaces slicing).
- Limited (non-full) stop data is cached with a 1s TTL + route signature; module lists iterate without per-frame copies.
- Native memory: the per-module pixel arena starts at 1 MB and grows on demand (was a flat 16 MB per module).

**ABI 9 与旧模块 / compatibility notes**

- ABI 9 appends type fields to the vehicle snapshot and interchange layouts; ABI 6/7/8 modules keep loading with their exact original layouts (per-ABI-group snapshots). New ABI 9 scripts require this version or later.
- Full interchange type metadata from other players' servers benefits from an updated server JCM, but old/new mixes are handled.

Supported Minecraft versions: 1.17.1, 1.18.2, 1.19.2, 1.19.4, 1.20.1 and 1.20.4, for Fabric and Forge — **12 jars**, each bundling the four desktop JNI bridges (Windows x64, Linux x64, macOS x64, macOS ARM64). A separate `jcm-native-sdk` zip carries the SDK sources, build files and prebuilt example modules.

The C++ core follows "APIs are for data, native helpers are for rendering": rendering, state and draw-record encoding stay on the C++ side with zero per-frame Java allocation, which is why the stutter fix holds. The core is portable to Android (NDK) and to iOS/iPadOS hosts without a JVM.

Validation includes four-platform CMake/CTest, real JNI tests for every script kind, an API declaration audit and real MTR OpenGL model upload. Automated tests do not replace final visual testing in Minecraft.

Native SDK documentation: https://github.com/BugQuantumCore/Joban-Client-Mod-Yanyang/blob/main/native/README.md
Changelog: https://github.com/BugQuantumCore/Joban-Client-Mod-Yanyang/blob/main/CHANGELOG.md
