# JCM v2.2.0-beta-1 for MTR 4.0.3 has been released!

## New
- **Scripting**
- - something something vehicle scripting

## Changes
- **Scripting**
- - **Breaking Changes: MTR 4 Eyecandy registration**
- - - To conform better to the newly added vehicle scripting, the `scripting` object will no longer be read for eyecandy objects in the MTR 4 format. Please see [documentation LINK ME] for details.
- - `mtr_custom_resources_pending_migration.json` will now also be read by JCM for scripting.
- - **Breaking Changes: Model Loading**
- - - In previous versions of JCM, model loading in scripts are very limited, and can only load a single OBJ file. JCM v2.2 revises model loading to allow part of a model to be loaded, and allow limited amount of preprocessing before uploading the final model.
- - - This introduces the distinction between `RawModel` (A model part) and `ModelCluster` (The finalized, uploaded model). Those who had been using NTE should feel home to this changes.
- - - `ModelManager.loadModel` now only returns `RawModel`. To effectively use it, you have to obtain a `ModelCluster` by using `ModelManager.upload(model: RawModel)`.

## Fixes
- **Scripting**
- - Fix QuadDrawCall `INTERIOR` / `INTERIOR_TRANSLUCENT` render type not applying full brightness.

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm), [CurseForge](https://curseforge.com/minecraft/mc-mods/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)