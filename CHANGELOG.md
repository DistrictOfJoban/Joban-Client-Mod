## JCM v2.1.0 for MTR 4.0.0/4.0.1 has been released!

**New:**
- Add **Help Line (Tiu Keng Leng Station Type)**
- **Scripting:**
- - Add **Vector3f** for performing position-related operation.
- - **Sounds**
- - - Added a new `SoundManager` class for all types of scripting.
- - - - Provide generic ways to play sound onto the Minecraft world.
- - - `ctx.playSound()`, `ctx.playCarSound()` & `ctx.playAnnSound()` is deprecated as SoundManager can now achieve the same effect (And are consistent across different scripting type)
- - **Render**
- - Added a new `RenderManager` class for all types of scripting.
- - Provide generic ways to draw 3D model onto the Minecraft world
- - `ctx.drawModel()`, `ctx.drawCarModel()` and `ctx.drawConnModel()` is deprecated as RenderManager can now achieve the same effect.
- - **Eyecandy Scripting**
- - - Adapt RenderManager, can be accessed by `ctx.renderManager()`, see above.
- - - Adapt SoundManager, can be accessed by `ctx.soundManager()`, see above.
- - - Add `EyecandyBlockEntity.pos()`, `EyecandyBlockEntity.blockPos()` and `EyecandyBlockEntity.offsetPos()` to determine where the eyecandy block is placed.
- - **PIDS Scripting**
- - - Adapt RenderManager, can be accessed by `ctx.renderManager()`, see above.
- - - - This grants PIDS scripts the ability to render 3D OBJ model. It is advised to use this in conjunction with PIDS Projector, which is an invisible block.
- - - Adapt SoundManager, can be accessed by `ctx.soundManager()`, see above.
- - - Add `PIDSWrapper.blockPos()` to determine where the PIDS block is placed.
- - **Others**
- - - Add `MinecraftClient.getScoreboardScore(objective: string, playerName: string)` to obtain the scoreboard score of a player.
- - - Add `MinecraftClient.isHoldingItem(id: string)` to determine if the current player is holding an item with the corresponding item id.
- - - Add `MinecraftClient.blockLightAt(pos: vector3f)`, `MinecraftClient.skyLightAt(pos: vector3f)`, `MinecraftClient.lightLevelAt(pos: vector3f)` to determine light level in the world.
- - - Add `MinecraftClient.playerPos()` and `MinecraftClient.playerBlockPos()` to determine the position of the player.
- - - Add `MinecraftClient.playerName()` to obtain the account name of the current player.
- - - Add `MinecraftClient.gamePaused()` to determine if the game is paused.

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm), [CurseForge](https://curseforge.com/minecraft/mc-mods/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)
