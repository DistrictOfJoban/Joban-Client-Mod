# JCM v2.2.0-beta-1 for MTR 4.0.3 has been released!

Unless otherwise noted, all changes below pertains to the Scripting feature in JCM.

## New
- **(Non-scripting)** To improve compatibility with NTE, JCM now ships with several patches for MTR in order to replicate NTE behaviors.
- - OBJ model material now recognizes `exteriortranslucent`, `lighttranslucent` and `interiortranslucent` again, which is used by NTE but does not get parsed correctly in MTR 4.
- - For OBJ texture with path traversal (Like `../` / `..\`), it is once again recognized, solving some missing texture issues when loading NTE packs.
- [[[something something vehicle scripting]]]
- The **Rhino** JavaScript Engine has been updated to **1.9**, bringing some performance improvements and more modern JS features.
- - See the [Rhino Compatibility Table](https://mozilla.github.io/rhino/compat/engines.html) for a list of supported features.
- Add async variant of `Networking.fetchString() and Networking.fetchImage()`
- **Add Console API**
- - Provide a slightly more rich API for logging, similar to the web's console API seen in browsers. (console.log, console.warn etc.)
- - The `print` function will be retained, redirects to `console.log` under the hood.
- - The source/line no. of the print/console log statements may be viewed by enabling **View Script Log Source** in JCM's settings.
- Add `MinecraftClient#spawnParticleInWorld()` to spawn vanilla particles in the current world via scripts.
- Add `MinecraftClient#renderDistance()` to obtain the currently configured world render distance.

## Changes
- For script contexts (`ctx` parameter), functions now conforms to the `get`/`set` prefix.
- - `ctx.renderManager()` -> `ctx.getRenderManager()`
- - `ctx.soundManager()` -> `ctx.getSoundManager()`
- - The existing non-get functions will be retained for backward compatibility reasons.
- **Breaking Changes: MTR 4 Eyecandy registration**
- - To conform better to the newly added vehicle scripting, the `scripting` object will no longer be read for eyecandy objects in the MTR 4 format. Please see [documentation LINK ME] for the new registration format.
- Scripts in `mtr_custom_resources_pending_migration.json` will now also be recognized by JCM.
- **Breaking Changes: Model Loading**
- - In previous versions of JCM, model loading in scripts are very limited, and can only load a single OBJ file. JCM v2.2 revises model loading to allow part of a model to be loaded, and allow limited amount of preprocessing before uploading the final model.
- - This introduces the distinction between `RawModel` (A model part) and `ModelCluster` (The finalized model uploaded to the GPU). Those who had been using NTE should feel home to these changes.
- - `ModelManager.loadModel` now only returns `RawModel`. To effectively use it in rendering functions, you have to obtain a `ModelCluster` by using `ModelManager.upload(model: RawModel)`.
- **Breaking Changes: Matrices**
- - All Matrices created by scripts are now flipped 180 degree along the X-axis when applying.
- For `Resources.getSystemFont`, the font name `Noto Sans CJK TC Medium` can now be used instead of `Noto Sans` for Noto Sans CJK (Deprecated in JCM v2.2)
- - Added font name `Noto Sans SemiBold`, which returns MTR's Noto Sans SemiBold font (Non-CJK variant).
- **Breaking Changes: TextUtil**
- - `TextUtil.getNonExtraParts()` now returns the original string if no extra part is found, instead of an empty string.
- - `TextUtil.getNonCjkAndExtraParts()` now actually returns the Non-CJK + extra part as documented, instead of "Non-extra Non-CJK part + Non-extra part".
- The JCM config has been remade
- - Added **Script Debug Mode** (Script-specific debug mode), and **Show log source**
- - Existing config are migrated over to the new config. If you have enabled **Debug Mode** previously, Script Debug Mode will also be enabled by default.
- - New config file is located under `.minecraft/config/jsblock/client.toml` (Not relevant to most users)

## Fixes
- Fix QuadDrawCall `INTERIOR` / `INTERIOR_TRANSLUCENT` render type not applying full brightness.
- Fix counter-intuitive `Matrices` push/poping logic, which breaks many rendering stuff when used.
- Fix `Networking.fetchString()` being possible to hang indefinitely.

## Removal
- JCM build for Minecraft 1.16 has been fully dropped in the v2.2 series. Please update to a newer Minecraft version shall you wish to use JCM v2.2.

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm), [CurseForge](https://curseforge.com/minecraft/mc-mods/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)