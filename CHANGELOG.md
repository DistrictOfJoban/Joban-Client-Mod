# JCM v2.1.2 for MTR 4.0.3 has been released!

## New
- **Scripting**
- - Add script quick reload, allowing reloading script without reloading the entire resource pack.
- - - JCM Debug Mode must be enabled before using.
- - - Shift+R: Reload JCM Scripts
- - - Ctrl+R: Reload MTR Scripts

## Fixes
- **Scripting**
- - Add `java.text.*` package to whitelist, allowing `AttributedString` to be used on AWT Graphics-based PIDS.
- - Fix internal timer possibly rolling into the negatives after rejoining world, causing script with `RateLimit` to not work properly for a short while.
- Fix breaking incomplete/broken multipart block crashing the game.

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm), [CurseForge](https://curseforge.com/minecraft/mc-mods/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)