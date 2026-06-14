# JCM v2.2.1 for MTR 4.0.5 has been released!

This is a minor release. Server owners are advised to update to this version.

**For MTR 4.0.5 only!**

## Additions
- **MTR Patch: Rail Improvements**
  - Attempt to improve culling performance and avoid errors for large rail
  - Lower rail culling precision to help with lag spikes when creating large rail.
- **Scripting:**
  - Add support for loading MQO (Metasequoia) model via ModelManager, following MTR's update to bring MQO model support.
    - Note: You should explicitly pass `flipTextureV` as `false` for these type of models.
  - PIDS: Add `TextWrapper.lineHeight()` to specify a line height factor (Default: 1) use alongside `TextWrapper.wrapText()`.
  - Add overload for `GraphicsTexture.upload()` with BufferedImage argument.

## Changes
- Scripting:
  - The `include()` script function will now hard fail if the script file to be included is not found.
- Add some extra permission guarding against block configuration packet.

## Fixes
- Fix various transformation errors when using legacy NTE eyecandy's `translation`/`rotation`/`scale` field. They should now behave identically to NTE.

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm), [CurseForge](https://curseforge.com/minecraft/mc-mods/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)