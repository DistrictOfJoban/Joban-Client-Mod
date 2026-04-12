# JCM v2.2.0-beta-1 for MTR 4.0.4 has been released!

Unless otherwise noted, all changes below pertains to the Scripting feature in JCM.

## New (Non-Scripting)
During the development of JCM v2.2, several improvement area (Related to UX/performance/legacy NTE compatibility) in the main MTR mod were identified.

To ensure player gets the best in-game playing experience, several patches for MTR were integrate to JCM, which I will collectively call them the **MTR Patch**.

This allows faster delivery of bug-fixes/optimizations/improvements to MTR 4, while serving as a "playground" for these patches, so hopefully errors could be caught before they are merged into the main mod.

**Applied patch as follows:**
- MTR 3 / NTE Compatibility Improvements:
  - Fix: NTE Eyecandy object now recognizes the "translation"/"rotation"/"scale"/"mirror" fields, which is used by NTE but does not get parsed correctly in MTR 4.
  - Fix: For OBJ texture with path traversal (Like `../` / `..\`), it is once again recognized, solving some missing texture issues when loading NTE packs.
- Enhancements: Improve model loading speed and memory usage for MTR.
- Enhancements: Minor frame-rate boost in complex world.
- Enhancements: Add car auto-filling in siding screen
  - For supported vehicles, you can now hold SHIFT when adding vehicle cars to automatically fill out the entire siding length, with Cab, Trailer and Reversed Cab of the same train type being filled.
  - Vehicles ending with id `cab_1`, `cab_2` and `trailer` are supported.
  - Similarly, you can hold SHIFT when deleting cars from siding to clear all cars.

## New
- **Vehicle Scripting**
- - Vehicle scripting has been added in this beta version.
- - Backward compatibility with existing resource packs is on a best-effort basis. Some loads, some doesn't.
- - API for NTE remains largely unchanged, with some new MTR 4 APIs developers can opt into.
- - Mixed vehicle car operations is supported, alongside sharing script entries across multiple cars.
- - See the [Vehicle Scripting](https://jcm.joban.org/v2.2/dev/scripting/type/vehicle/) documentation for details, or try loading an MTR 3 scripted vehicle and see what needs to be ported.
- The **Rhino** JavaScript Engine has been updated to **1.9**, bringing some performance improvements and more modern JS features.
  - See the [Rhino Compatibility Table](https://mozilla.github.io/rhino/compat/engines.html) for a list of supported features.
- Add `BackgroundWorker` to allow script to run task without blocking the main thread.
- **Add Console API**
  - Provide a slightly more rich API for logging, similar to the web's console API seen in browsers. (console.log, console.warn etc.)
  - The `print` function will be retained, redirects to `console.log` under the hood.
  - The source/line no. of the print/console log statements may be viewed by enabling **View Script Log Source** in JCM's settings.
- `Resources.ensureStrFonts()` now allows you to append an unlimited amount of AWT fallback fonts.
- Add `MinecraftClient.spawnParticleInWorld()` to spawn vanilla particles in the current world via scripts.
- Add `MinecraftClient.renderDistance()` to obtain the currently configured world render distance.
- Add `MinecraftClient.getWorldPlayers()` to obtain all player's `PlayerEntity` within the current render distance.
- Add `EyecandyBlockEntity.isCrosshairTarget()` to indicate whether the player is looking at the the eyecandy.
  - Can be used to provide tooltips or additional information if needed.
- Add `hide_display_cube` / `hideDisplayParts` (MTR 3 / MTR 4) vehicle properties to hide the displays, e.g those in built-in models.
  - Used to allow scripting to override built-in displays such as destination displays.
- Add more methods to get more detail for entities/players.
- **JCM PIDS Scripting**
  - Add override for `Text.marquee(duration: double)`, allowing you to specify the marquee cycle duration in tick.
  - Add `Text.withMarqueeProgress(progress: double)`, allowing you to directly override the marquee progress. (0.0 - 1.0)

## Changes
- A very minor change: JCM now always load scripts & PIDS Preset after MTR mod for consistency reason, I don't expect any user-facing changes.
- Improve speed for `GraphicsTexture.upload()`, now it should be faster.
- For script contexts (`ctx` parameter), functions now conforms to the `get`/`set` prefix.
  - `ctx.renderManager()` -> `ctx.getRenderManager()`
  - `ctx.soundManager()` -> `ctx.getSoundManager()`
  - The existing non-get functions will be retained for backward compatibility reasons.
- Scripts in `mtr_custom_resources_pending_migration.json` will now also be recognized by JCM.
- `Vector3f` now allows method chaining.
- **Breaking Changes: MTR 4 Eyecandy registration**
  - To conform better to the newly added vehicle scripting, the `scripting` object will no longer be read for eyecandy objects in the MTR 4 format. Please see [the new documentation](https://jcm.joban.org/v2.2/dev/scripting/type/eyecandy/#registering-scripts-to-decoration-object) for the new registration format.
  - MTR 3/NTE format remains non-affected
- **Breaking Changes: Model Loading**
  - In previous versions of JCM, model loading in scripts are very limited, and can only load a single OBJ file. JCM v2.2 revises model loading to allow part of a model to be loaded, and allow limited amount of preprocessing before uploading the final model.
  - This introduces the distinction between `ModelData` (The raw model data) and `Model` (The finalized model uploaded to the GPU). Those who had been using NTE should feel home to these changes. (They are RawModel and ModelCluster respectively)
  - `ModelManager.loadModel` now only returns `ModelData`. To effectively use it in rendering functions, you have to obtain a `Model` by using `ModelManager.upload(model: ModelData)`.
- **Breaking Changes: Matrices**
  - All Matrices created by scripts are now flipped 180 degree along the X-axis when applying.
- For `Resources.getSystemFont`, the font name `Noto Sans CJK TC Medium` can now be used instead of `Noto Sans` for Noto Sans CJK (Deprecated in JCM v2.2)
  - Added font name `Noto Sans SemiBold`, which returns MTR's Noto Sans SemiBold font (Non-CJK variant).
- **Breaking Changes: TextUtil**
  - `TextUtil.getNonExtraParts()` now returns the original string if no extra part is found, instead of an empty string.
  - `TextUtil.getNonCjkAndExtraParts()` now actually returns the Non-CJK + extra part as documented, instead of "Non-extra Non-CJK part + Non-extra part".
- **Data Loading Rework**
  - Previously, multiple functions exists for reading string/images in `Resources` and `Networking`, while the `Files` API is only capable of reading string.
  - This has all been unified to `DataReader`, a new class which represents *a sequence of byte data*. It offers option such as `asString()`, `asBufferedImage()`, `asFont()`, `asByteArray()` and `asRawInputStream()`, allowing developers to read them into a variety of formats.
  - `Resources.read()` and `Networking.fetch()` has been added which returns the `DataReader` variant.
  - `Resources.readString()`, `Resources.readBufferedImage()`, `Resources.readFont()`, `Networking.fetchString` and `Networking.fetchImage` has been deprecated in favor of the DataReader functions.
  - Add `Resources.exist()` to determine if a file exist in a resource pack.
- **Breaking Changes: FileUtils**
  - `Files.readData()` and `Files.read()` now returns a `DataReader` instead of a string (See above)
- `Files.saveData()` now allows BufferedImage as an argument for data to save.
- **(Non-Scripting)** The JCM config has been remade, with several more options added:
  - Add **Disable Scripting** to disable parsing JS scripts. Used for performance debugging / low-end devices.
  - Add **Script Debug Mode** for JS scripts-specific debugging. (**Debug Mode** is now reserved for debug within JCM itself)
  - Add **Show log source** for JS scripts, to trace where print/log statements originates from.
  - Add **Disable MTR Rail Rendering** for performance diagnosing & providing a clearer view without rails.
  - Add **Hide currently riding vehicles** for recording purposes, similar to NTE. (e.g. Cab-view recording)
  - Existing config are migrated over to the new config. If you have enabled **Debug Mode** previously, Script Debug Mode will also be enabled by default.
  - New config file is located under `.minecraft/config/jsblock/client.toml` (Not relevant to most users)

## Fixes
- (Non-scripting) Fix fare/ticket transaction log potentially erroring out, causing players to be stuck in a gate.
- Fix Eyecandy icon not being rendered when holding brush for scripted object.
- Fix QuadDrawCall `INTERIOR` / `INTERIOR_TRANSLUCENT` render type not applying full brightness.
- Fix models & QuadDrawCall being rendered in full brightness in Scripted PIDS Preset
- Fix counter-intuitive `Matrices` push/poping logic, which breaks many rendering stuff when used.
- Fix `Networking.fetchString()` being possible to hang indefinitely.
- Fix `Files.saveData()` not working with non-existent subdirectories.

## Removal
- JCM build for Minecraft 1.16 has been fully dropped in the v2.2 series. Please update to a newer Minecraft version shall you wish to use JCM v2.2.

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm), [CurseForge](https://curseforge.com/minecraft/mc-mods/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)