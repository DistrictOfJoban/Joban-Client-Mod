JCM Yanyang `2.3.0-beta-2.yanyang.1` — upstream 2.3.0-beta.2 integrated into the Yanyang fork, with Yanyang-side optimizations and all Yanyang features (MAGIC vehicle types, native C++ scripting, the GC-stutter fix) retained.

**整合原版 2.3.0-beta.2 / Upstream 2.3.0-beta.2 integrated**

MTR Patch:
- Rail preview of long-running rails outside render distance is preserved (rail node block states cached per tick).
- The multi-item selection screen supports `Shift` for exclusive selection.
- **Experimental frustum culling** for vehicles, lifts and rails, layered on top of MTR's occlusion culling — scripts keep being invoked, and each layer is individually toggleable in JCM Settings.
- The MTR 3 camera-lowering-by-sneaking feature is back (`allowCameraLowering`, default on; revert to MTR 4 behaviour by turning it off).

Scripting API:
- `MinecraftClient.getCameraMode()` (F5 perspective), `MinecraftClient.lightAt(pos)` (packed block+sky light), `RenderManager.drawModel(model, matrices, light)`.
- `VehicleWrapper.isDoorOpenable(car, pos)` / `isDoorOpen(...)`.
- `LiftWrapper.isDoorway1/2Openable()` / `isDoorway1/2Open()`.
- Lift scripts that control rendering now move the riding player (`VehicleRidingMovement`).
- Czech & German language stubs added upstream.

**晏阳整合优化 / Yanyang integration optimizations**

- `isScriptRendered` → `isScriptControlled` rename is **backwards compatible**: both field names are accepted, so 2.3.0-beta.1 resource packs keep working.
- `LiftWrapper` doorway openability is **lazily computed** on first query instead of eagerly per lift per frame in the constructor — scripts that never ask pay zero world-query cost.
- C++ lift scripts that control rendering now ride the player with the same `VehicleRidingMovement` semantics as JS lift scripts.
- Upstream's frustum culling (MTR built-in rendering) and Yanyang's `VehicleCulling` (native script execution) are complementary layers; both keep scripts running.

**保留的晏阳能力 / Retained Yanyang features**

- `getTransportType()` returns MAGIC MTR Addon vehicle types (`train_metro`, `train_light_rail`, `train_high_speed`, `train_bus`, `train_tram`, `train_sbahn`, `train_normal`) plus the four base modes — from JS, Java and C++ (ABI 9); MAGIC integrated via optional reflection, no hard dependency.
- No more native-script GC stutter: StopsData graph reuse, reusable snapshot builders, cached JNI arena views, 1 MB-on-demand pixel arenas (from yanyang.11).
- ABI 6/7/8 modules keep loading alongside ABI 9.

Supported Minecraft versions: 1.17.1, 1.18.2, 1.19.2, 1.19.4, 1.20.1 and 1.20.4, for Fabric and Forge — **12 jars**, each bundling the four desktop JNI bridges (Windows x64, Linux x64, macOS x64, macOS ARM64). A separate `jcm-native-sdk` zip carries the SDK sources, build files and prebuilt example modules.

Validation includes four-platform CMake/CTest, real JNI tests for every script kind, an API declaration audit and real MTR OpenGL model upload. Automated tests do not replace final visual testing in Minecraft.

Native SDK documentation: https://github.com/BugQuantumCore/Joban-Client-Mod-Yanyang/blob/main/native/README.md
Changelog: https://github.com/BugQuantumCore/Joban-Client-Mod-Yanyang/blob/main/CHANGELOG.md
