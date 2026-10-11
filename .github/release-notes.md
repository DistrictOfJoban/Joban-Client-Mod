JCM Yanyang.7 adds complete native execution for vehicle, PIDS, Lift and Eyecandy scripts.

- JavaScript `RouteInterchange.getTransportMode()` returns the actual TSC TransportMode. Server stop data includes in-station and connecting interchange types.
- ABI 8 gives C++ direct access to the same public Common API and script-specific Java objects, including Java2D, resource/model/texture loading, rendering, sound, networking, files, callbacks and data classes.
- PIDS, Lift and Eyecandy now execute native create/render/dispose and replay models, text, textures, sounds, matrices, shapes and block-use events.
- JNI lookup caching, reusable buffers, smaller snapshot copies, bounded draw arenas and callback quiescence improve execution and reload behavior.
- ABI 6/7 modules remain accepted with their original snapshot layout. New ABI 8 scripts require this version. Full remote interchange metadata requires updated server JCM.
- Each jar contains only the four platform JNI bridges. Native SDK examples and resource-pack scripts/assets are distributed separately from the mod.

Supported Minecraft versions: 1.17.1, 1.18.2, 1.19.2, 1.19.4, 1.20.1 and 1.20.4, for Fabric and Forge (12 jars).

Validation includes four-platform CMake/CTest, real JNI tests for every script kind, an API declaration audit and real MTR OpenGL model upload. Automated tests do not replace final visual testing in Minecraft.

Native SDK documentation: https://github.com/BugQuantumCore/Joban-Client-Mod-Yanyang/blob/main/native/README.md
