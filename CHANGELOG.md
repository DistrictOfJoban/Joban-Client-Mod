# JCM v2.2.0-beta-1 for MTR 4.0.3 has been released!

## New
- **Scripting**
- - something something vehicle scripting

## Changes
- **Scripting**
- - For Minecraft 1.17+, the **Rhino** JavaScript Engine has been updated to **1.9**, bringing some performance improvements and more modern JS features.
- - - See the [Rhino Compatibility Table](https://mozilla.github.io/rhino/compat/engines.html) for a list of supported features.
- - **Breaking Changes: MTR 4 Eyecandy registration**
- - - To conform better to the newly added vehicle scripting, the `scripting` object will no longer be read for eyecandy objects in the MTR 4 format. Please see [documentation LINK ME] for the new registration format.
- - Scripts in `mtr_custom_resources_pending_migration.json` will now also be recognized by JCM.
- - **Breaking Changes: Model Loading**
- - - In previous versions of JCM, model loading in scripts are very limited, and can only load a single OBJ file. JCM v2.2 revises model loading to allow part of a model to be loaded, and allow limited amount of preprocessing before uploading the final model.
- - - This introduces the distinction between `RawModel` (A model part) and `ModelCluster` (The finalized model uploaded to the GPU). Those who had been using NTE should feel home to these changes.
- - - `ModelManager.loadModel` now only returns `RawModel`. To effectively use it in rendering functions, you have to obtain a `ModelCluster` by using `ModelManager.upload(model: RawModel)`.
- - **Breaking Changes: Matrices**
- - - All Matrices created by scripts are now flipped 180 degree along the X-axis when applying.

## Fixes
- **Scripting**
- - Fix QuadDrawCall `INTERIOR` / `INTERIOR_TRANSLUCENT` render type not applying full brightness.
- - Fix counter-intuitive `Matrices` push/poping logic, which breaks many rendering stuff when used.
- - Fix `Networking.fetchString()` being possible to hang indefinitely.

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm), [CurseForge](https://curseforge.com/minecraft/mc-mods/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)