## JCM v2.1.0-beta.1 for MTR 4.0.0/4.0.1 has been released!

This is an interim release with many scripting-related changes in order to to provide more possibilities for script developers.

**[IMPORTANT NOTE] This is a beta release primarily for script developers to play around and give feedback on. Normal players are advised to wait until the final release of v2.1.0.**

**New:**
- Add **Help Line (Tiu Keng Leng Station Type)**
- More scripting changes, implementation details are outlined below.
- - **Major milestone reached:**
- - Scripting for PIDS (As it always worked since JCM v2)
- - Eyecandy LCD displays is now possible with the use of QuadDrawCall
- - It is now possible to register scripts in the MTR 4 version of `mtr_custom_resources.json`

**New (Scripting Implementation):**
- **Script Registration:**
- - The `scriptInput` field can now be supplied in the json entry alongside `scriptFiles` and `scriptText`.
- - Anything within the field (JSON Object/Array/String) will be made accessible to the script via the `SCRIPT_INPUT` variable.
- **Scripting Engine (Rhino)**
- - **For Minecraft 1.17+**, the Rhino JS engine has been updated to **1.8.0** which added more support for modern JS syntax. See [this compatibility table](https://mozilla.github.io/rhino/compat/engines.html) for details.
- - Minecraft 1.16.5 will remain on Rhino 1.7.15 due to the Java 8 requirement.
- **Sound**
- - Added a new `SoundManager` class for all types of scripting.
- - - Provide generic ways to play sound onto the Minecraft world.
- - `ctx.playSound()`, `ctx.playCarSound()` & `ctx.playAnnSound()` has been deprecated in favor of SoundManager.
- **Rendering**
- - Added a new `RenderManager` class for all types of scripting.
- - Provide generic ways to draw 3D model onto the Minecraft world
- - `ctx.drawModel()`, `ctx.drawCarModel()` and `ctx.drawConnModel()` has been deprecated in favor of RenderManager.
- - **GraphicsTexture.upload()** are now more optimized, should be mostly on-par with NTE.
- **Eyecandy Scripting**
- - Adapt RenderManager & SoundManager, can be accessed by `ctx.renderManager()` & `ctx.soundManager()`, see above.
- - Add `EyecandyBlockEntity.pos()`, `EyecandyBlockEntity.blockPos()` and `EyecandyBlockEntity.offsetPos()` to determine where the eyecandy block is placed.
- **PIDS Scripting**
- - Adapt RenderManager & SoundManager, can be accessed by `ctx.renderManager()` & `ctx.soundManager()`, see above.
- - - This grants PIDS scripts the ability to render 3D OBJ model. It is advised to use this in conjunction with PIDS Projector, which is an invisible block.
- - Add `PIDSWrapper.blockPos()` to determine where the PIDS block is placed.
- - Add `PIDSWrapper.isKeyBlock()` to determine if the current block is a unique block within a PIDS pair (e.g. Identify 1 side of a dual-sided PIDS)
- **Misc. Scripting Additions**
- - Add **Vector3f** for performing position-related operation.
- - Add `MinecraftClient.getScoreboardScore(objective: string, playerName: string)` to obtain the scoreboard score of a player.
- - Add `MinecraftClient.isHoldingItem(id: string)` to determine if the current player is holding an item with the corresponding item id.
- - Add `MinecraftClient.blockLightAt(pos: vector3f)`, `MinecraftClient.skyLightAt(pos: vector3f)`, `MinecraftClient.lightLevelAt(pos: vector3f)` to determine light level in the world.
- - Add `MinecraftClient.playerPos()` and `MinecraftClient.playerBlockPos()` to determine the position of the player.
- - Add `MinecraftClient.playerName()` to obtain the account name of the current player.
- - Add `MinecraftClient.gamePaused()` to determine if the game has been paused.

**Notice for 1.16.5 players**  
Minecraft 1.16.5 has already been released for well over 4 years. While there are many efforts in the codebase to make JCM available for 1.16.5, it also puts a burden to maintain them. 
- **For now** we will maintain the build for Minecraft 1.16.5, however feature parity cannot be guarenteed.
- Players who are still using Minecraft 1.16.5 should consider upgrading to any newer version.
- If demand are low enough, we may consider dropping 1.16.5 support all-together.

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm), [CurseForge](https://curseforge.com/minecraft/mc-mods/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)
