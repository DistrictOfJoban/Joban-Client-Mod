## JCM v2.1.0 for MTR 4.0.0/4.0.1 has been released!

**New:**
- Add **Help Line (Tiu Keng Leng Station Type)**

**New (Scripting):**
- **Script Registration:**
- - The `scriptInput` field can now be supplied in the json entry alongside `scriptFiles` and `scriptText`.
- - Anything within the field (JSON Object/Array/String) will be made accessible to the script via the `SCRIPT_INPUT` variable.
- **Sound Management**
- - Added a new `SoundManager` class for all types of scripting.
- - - Provide generic ways to play sound onto the Minecraft world.
- - `ctx.playSound()`, `ctx.playCarSound()` & `ctx.playAnnSound()` has been deprecated as SoundManager can now achieve the same effect (And are consistent across different scripting type)
- **Rendering Management**
- - Added a new `RenderManager` class for all types of scripting.
- - Provide generic ways to draw 3D model onto the Minecraft world
- - `ctx.drawModel()`, `ctx.drawCarModel()` and `ctx.drawConnModel()` has been deprecated as RenderManager can now achieve the same effect.
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

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm), [CurseForge](https://curseforge.com/minecraft/mc-mods/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)
