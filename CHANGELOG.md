## JCM v2.1.0 for MTR 4.0.0/4.0.1 has been released!

**New:**
- Scripting:
- - Add `MinecraftClient.getScoreboardScore(objective: string)` to obtain the scoreboard score of the current player.
- - Add `MinecraftClient.isHoldingItem(id: string)` to determine if the current player is holding an item.
- - Add `MinecraftClient.blockLightAt(pos: vector3f)`, `MinecraftClient.skyLightAt(pos: vector3f)`, `MinecraftClient.lightLevelAt(pos: vector3f)` to determine light level in the world.
- - Add `MinecraftClient.playerPos()` and `MinecraftClient.playerBlockPos()` to determine the position of the player.
- **Eyecandy Scripting**
- - Add `EyecandyBlockEntity.pos()` and `EyecandyBlockEntity.blockPos()` to determine where the eyecandy block is placed.
- **PIDS Scripting**
- - Add `PIDSWrapper.pos()` and `PIDSWrapper.blockPos()` to determine where the PIDS block is placed.

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm), [CurseForge](https://curseforge.com/minecraft/mc-mods/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)
