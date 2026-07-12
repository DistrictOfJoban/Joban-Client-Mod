# JCM v2.3.0 for MTR 4.0.5 has been released!

## Additions
- Add `MinecraftClientWrapper.getRedstoneLevel(pos: BlockPos)` to obtain the emitted redstone level in the world at a specific BlockPos.
- Add `Resources.hasSystemFont()` to check if a font is available in the system
- `EyecandyBlockWrapper.redstoneLevel()` now returns the true redstone level, instead of only 0 and 15.
- Add an extra `soundCategory: String` argument overload for `SoundManager.playSound` and `SoundManager.playLocalSound` to play sound other than in the "Master" category.
- **PIDS Scripting:**
  - Add `TextWrapper.renderType` to specify a custom render type for text, alongside TextureWrapper.
  - Add `TextWrapper.naturalLight()` and `TextureWrapper.naturalLight()` to allow rendering PIDS elements at world light. 

## Changes
- Incorrect invocation of `ModelManager.upload()` will now throw an error instead of crashing the game.
- **Vehicle Scripting:**
- `ctx.setDataFetchMode()` is now deprecated. You should now configure it in the script entry via the `dataFetchMode` field.

## Fixes
- Fix a bug where changing an eyecandy scripted model will not dispose the previous script.

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm), [CurseForge](https://curseforge.com/minecraft/mc-mods/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)