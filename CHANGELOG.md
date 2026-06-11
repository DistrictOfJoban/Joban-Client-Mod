# JCM v2.2.1 for MTR 4.0.4 has been released!

This is a minor release. Server owners are advised to update to this version.

## Additions
- MTR Patch: Rail Improvements
  - Improve culling performance and avoid errors for large rail
  - Lower rail culling precision to help with lag spikes when creating large rail.
  - Improve rail render distance check, which should result in smoother rail visibility.
- Scripting:
  - PIDS: Add `TextWrapper.lineHeight()` to specify a line height factor (Default: 1) use alongside `TextWrapper.wrapText()`.
  - Add overload for `GraphicsTexture.upload()` with BufferedImage argument.

## Changes
- Scripting:
  - The `include()` script function will now hard fail if the script file to be included is not found.
- Add some extra permission guarding against block configuration packet.

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm), [CurseForge](https://curseforge.com/minecraft/mc-mods/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)