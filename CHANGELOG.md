# JCM v2.2.0-beta-2 for MTR 4.0.4 has been released!

This enhances compatibility with NTE scripts and fix some issues.

**Client & Server Update is required**

Unless otherwise noted, all changes below pertains to the Scripting feature in JCM.

## Additions
- Add `RawMeshBuilder` and `RawMesh`, both are *mostly* API-compatible with NTE.
- Add `generateNormal()`, `distinct()` and `triangulate()` to `RawModel`.
- Add `customDestination` to `Stop`, which exposes the raw custom destination value.

## Changes
- `ModelData` has been renamed back to `RawModel`
- Expose the `RawModel` alias to scripts, allowing a `RawModel` to be easily constructed without referencing package name.
- DisplayHelper is no longer driven by `QuadDrawCall`, this fixes an issue where the display may appear dimmer as intended.
- **Breaking: The `DisplayHelper.drawCall()` method of drawing is removed, a DisplayHelper now contains a true 3D Model, you should use ModelDrawCall or drawModel functions.**

## Fixes
- Address a known issue in beta.1, where the stops data are incorrectly fetched.

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm), [CurseForge](https://curseforge.com/minecraft/mc-mods/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)