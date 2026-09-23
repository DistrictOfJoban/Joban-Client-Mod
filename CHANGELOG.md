# JCM v2.3.0-beta.1 for MTR 4.0.5 has been released!

## Additions
- **Scripting**
  - Added `ctx.setDebugInfo(value: any)` shorthand for temporary, single-value on-screen debug info, without requiring a key.
    - Same as calling `ctx.setDebugInfo("<Untitled>", value)`
  - Added `isScriptRendered` field for eyecandy and lift entry, which allows script to fully take over the rendering, without MTR's default renderer.
- Add **Emergency Train Stop Button (Wall mounted, TML)** & URL variant (Thanks **LX9702**!)

## Changes
- **Scripting**
  - The `create()` function is changed to be re-invoked again after an execution error, instead of continuing towards `render()` function, where not all variable may be initialized, obscuring the original error in the create function.
- **PIDS Scripting**
  - Add `PIDSWrapper.isPlatformAutoDetected()`, returning whether the selected PIDS platform is manually picked by the user, or automatically detected.
- When script debug mode is enabled, in-game script parsing error messages will now display the first-time you join the game.
- UI:
  - Numeric field no longer have the arrow button, as it's rarely used
  - Numeric field scrolling now allows finer increment when holding Shift (0.05), or Ctrl+Shift (0.025)
  - Numeric field now allows you to enter negative sign directly (`-`) after Ctrl+A.

## Fixes
- **PIDS Projector** can now render even further away before disappearing.
- Fix Automatic Iron Door accounting for players in spectator mode as well.

### PIDS Textures
Please note that several textures used by PIDS (`rv_door_cls_apg.png`, `rv_door_cls_psd.png`, `rv_door_cls_train.png`, `thumbnail/pids_1a.png`) has been relocated from `jsblock:textures/block/pids` to `jsblock:textures/pids`.

It is done this way to avoid Minecraft including the texture to the block texture atlas, resulting in unnecessary overhead/enlarging of the block texture size.

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm), [CurseForge](https://curseforge.com/minecraft/mc-mods/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)