# JCM v2.1.0-beta.1 for MTR 4.0.2 has been released!

## New (Player features)
- Add **Help Line (Tiu Keng Leng Station Type)**
- The **Script Debug Overlay** now allows switching between sources (MTR/JCM) to provide a more clutter-free view.
- - Default keybind is `[` and `]`, can be changed in Minecraft keybind settings.
- **More scripting progress, implementation additions are outlined below.**
- - **Major milestone reached:**
- - - [v2.0.0] PIDS Preset Scripting
- - - **[v2.1.0]** Scripted Eyecandy displays (e.g. LCD screens) now possible with the use of **QuadDrawCall**.
- - - **[v2.1.0]** MTR 4 version of `mtr_custom_resources.json` may now register scripts!

## New (Scripting Implementation)
### Registration
- Eyecandy/Decoration Object scripts in the MTR 4 custom resource format can now be registered!
- - See [documentation](https://jcm.joban.org/latest/dev/scripting/eyecandy/#registering-scripts-to-decoration-object) for details!
- Added **Script Input** feature
- - `scriptInput` (MTR 3/NTE/JCM) / `scripting`.`input` (MTR 4) field can now be supplied in the json entry alongside `scriptFiles` and `scriptText`.
- - Anything within the field (JSON Object/Array/String) will be made accessible to the script via the `SCRIPT_INPUT` variable.
- - This is intended as a successor to `scriptTexts` (w/ only variable declaration) and ANTE's `CONFIG_INFO` feature.

### Scripting Engine (Rhino)
- **For Minecraft 1.17+**, the Rhino JS engine has been updated to **1.8.0** which added more support for modern JS syntax. See [this compatibility table](https://mozilla.github.io/rhino/compat/engines.html) for details.
- Minecraft 1.16.5 will remain on Rhino 1.7.15 due to the Java 8 requirement.

### Sound
- Added a new `SoundManager` class for all types of scripting.
- - Provide generic ways to play sound onto the Minecraft world.
- `ctx.playSound()`, `ctx.playCarSound()` & `ctx.playAnnSound()` has been deprecated in favor of SoundManager.

### Rendering
- Added a new `RenderManager` class for all types of scripting.
- Provide generic ways to draw 3D models or quads onto the Minecraft world
- `ctx.drawModel()`, `ctx.drawCarModel()` and `ctx.drawConnModel()` has been deprecated in favor of RenderManager.

### Eyecandy Scripting
- Adapt RenderManager & SoundManager, can be accessed by `ctx.renderManager()` & `ctx.soundManager()`, see above.
- Add `EyecandyBlockEntity.pos()` and `EyecandyBlockEntity.blockPos()` to determine where the eyecandy block is placed.
- Allow setting custom outline shape with `ctx.setOutlineShape(shape: VoxelShape)`
- Add block use events for eyecandy
- - Call `ctx.events().onBlockUse.occurred()` to check, and `ctx.events().onBlockUse.detail()` for event detail.
- - **Important:** Acknowledge events with `ctx.events().handled()` afterwards at suitable interval so the internal event state is reset.

### PIDS Scripting
- Adapt RenderManager & SoundManager, can be accessed by `ctx.renderManager()` & `ctx.soundManager()`, see above.
- - This grants PIDS scripts the ability to render 3D OBJ model. It is advised to use this in conjunction with PIDS Projector, which is an invisible block.
- Add `PIDSBlockEntity.blockPos()` to determine where the PIDS block is placed.
- Add `PIDSBlockEntity.isKeyBlock()` to determine if the current block is a unique block within a PIDS pair (e.g. Identify 1 side of a dual-sided PIDS)

### Misc. Scripting Additions
- Add `display_helper.js` based on NTE's implementation. This should provide full backward compatibility for eyecandy using DisplayHelper, without manual interference needed for upgrade.
- Add `Vector3f` for performing position-related operation.
- Add `VanillaText` for creating and styling minecraft-based text. (Bold/Italic/Colors etc.)
- - Add `MinecraftClient.displayMessage(text: VanillaText, actionBar: boolean)` overload to display these text in chat or action bar, in addition to the `string` variant.
- - This can be considered a substitute of `ComponentUtil` in ANTE.
- Add `MinecraftClient.localPlayer(): PlayerEntity` to retrieve more detailed info of the client player.
- Add `MinecraftClient.getScoreboardScore(objective: string, playerName: string)` to obtain the scoreboard score of a player.
- Add `MinecraftClient.blockLightAt(pos: vector3f)`, `MinecraftClient.skyLightAt(pos: vector3f)`, `MinecraftClient.lightLevelAt(pos: vector3f)` to determine light level in the world.
- Add `MinecraftClient.gamePaused(): boolean` to determine if the game has been paused.
- Add `StateTracker.changedTo(state: any)` and `StateTracker.changedFromTo(fromState: any, toState: any)`

## Changes
- **Scripting:**
- - `StateTracker` now accepts non-string values.
- - `GraphicsTexture.upload()` are now more optimized, should be mostly on-par with NTE.
- - The `Networking` class in JCM will no longer overwrite the User-Agent header if scripting restrictions is disabled.

## Fixes
- Fix script not disposed after leaving the game
- Fix `Timing.elapsed()` only returning in-game tick values (i.e. in at least 50ms increments)
- - This also fix `RateLimit` only allowing a max update rate of 20 times per second caused by the above.

## Documentations
The scripting documentation has been slightly restructured. For existing PIDS Scripting users, this may take some time to get used to!

Content changes (Excluding restructuring):

- Add [Scripting Guidelines](https://jcm.joban.org/latest/dev/scripting/articles/script_guidelines) page
- Add [Migration from NTE](https://jcm.joban.org/latest/dev/scripting/articles/nte_migration) page
- Add `Vector3f` to [Math Utilities](https://jcm.joban.org/latest/dev/scripting/math) page
- `ResourceLocation` has now been renamed to `Identifier` across the documentations.
- Remove the `wrapper` suffix for class names, as they should be transparent to script developers
- - `PIDSWrapper` has been renamed to `PIDSBlockEntity`
- - `ArrivalsWrapper` has been renamed to `ArrivalEntries`
- - `ArrivalWrapper` has been renamed to `ArrivalEntry`

## Important Note
### For Minecraft 1.16 players
Minecraft 1.16 has already been released for well over 4 years. While there are many efforts in the codebase to make JCM available for 1.16, it also puts a burden to maintain them.
- Minecraft 1.16 will no longer receive first-class support like other versions.
- Feature parity will no longer be maintained. New script features/capabilities (Especially scripting engine related) may not be available on 1.16.
- Players who are still using Minecraft 1.16 should consider upgrading to any newer version.
- When demands have become low enough, we may consider dropping all 1.16 builds and CI all-together.

### For other players
This is a beta release primarily for script developers to play around and give feedback on.  
Normal players are *strongly* advised to wait until the official release of v2.1.0.

### For Script Developers
Breaking changes may occur for all *new features* introduced during the v2.1.0 beta. JSON fields and scripting APIs may be moved, renamed, redesigned or scrapped at any point during beta releases.

You are more than encouraged to provide feedback for the newly introduced features (Yes, down to the naming!). Otherwise, no new breaking changes will be introduced after the official release, and any imperfections/annoyance that you perceive will stay with you forever :D

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm), [CurseForge](https://curseforge.com/minecraft/mc-mods/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)
