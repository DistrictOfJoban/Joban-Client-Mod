# Changelog

## 2.3.0-beta.1-yanyang.7

- Add RouteInterchange.getTransportMode(), preserving types across server data and native snapshots.
- Execute C++ PIDS, lift and eyecandy scripts through their real rendering entry points.
- Add direct C++ access to the shared Java scripting APIs, including resources, models, graphics, sounds, Minecraft, files, networking and callbacks.
- Preserve ABI 6/7 compatibility while introducing ABI 8.
- Release per-instance resources on removal/reload and cache JNI method/class bindings.
