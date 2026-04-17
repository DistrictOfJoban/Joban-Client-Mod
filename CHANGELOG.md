# JCM v2.2.0-beta-2 for MTR 4.0.4 has been released!

This release brings back the MTR 3 lift arrival sound (The ding sound), enhance compatibility with NTE scripts and fix some issue.

**Client & Server Update is required**

Unless otherwise noted, all changes below pertains to the Scripting feature in JCM.

## Additions
- **(Non-scripting) MTR Patch**
  - Revive the **lift ding** sound feature from MTR 3, now they should function mostly equivalently.
- Add `RawMeshBuilder` and `RawMesh`, both are *mostly* API-compatible with NTE.
- Add `generateNormal()`, `distinct()` and `triangulate()` to `RawModel`.
- Add `customDestination` to `Stop`, which exposes the raw custom destination value.
- Add `TickableSoundInstance`, allowing sound parameters (e.g. volume/pitch) to be continuously adjusted throughout the playback, as well as the ability to stop the sound midway.
  - `MinecraftClient.stopSound()` has been removed, you should use a TickableSoundInstance instead and stop with `SoundManager`.
- **PIDS Scripting**
  - Add `PIDSBlockEntity.getTargetPlatformIds()` to obtain the detected/selected platform id for ETA monitoring.

## Changes
- `ModelData` has been renamed back to `RawModel`
- Expose the `RawModel` alias to scripts, allowing a `RawModel` to be constructed without referencing package name. (Same as NTE)
- DisplayHelper is now driven by `RawMeshBuilder` instead of `QuadDrawCall`, this fixes an issue where the display may appear dimmer as intended.
- **Breaking: The `DisplayHelper.drawCall()` method of drawing is removed, a DisplayHelper now contains a true 3D Model, you should use the drawModel/drawCarModel functions to render it out.**

## Fixes
- Address a known issue in beta.1, where the stops data are incorrectly fetched across routes.
- Fix **BackgroundWorker** not getting interrupted on script reload, allowing submitted task to continue indefinitely.

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm), [CurseForge](https://curseforge.com/minecraft/mc-mods/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)