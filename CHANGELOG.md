# JCM v2.3.0-beta.1 for MTR 4.0.5 has been released!

## New: Lift Scripting
Lift Scripting has been added for review and feedback by the public.

See [JCM Docs](https://jcm.joban.org/v2.3/dev/scripting/type/lift/) for documentation.

## New: Blocks
- Add **Emergency Train Stop Button (Wall mounted, TML)** and **URL variant** (Thanks **LX9702**!)

## Other Changes

### Additions
- **Scripting**
  - Added `ctx.setDebugInfo(value: any)` shorthand for temporary, single-value on-screen debug info, without requiring a key.
    - Same as calling `ctx.setDebugInfo("<Untitled>", value)`
  - Added `isScriptRendered` field for eyecandy and lift entry, which allows script to fully take over the rendering, without MTR's default renderer.
  - Added `PlayerEntity.displayName()` to return the player's name with team prefixes.
  - Added `PlayerEntity.isSpectator()` and `PlayerEntity.isCreative()`
  - `Vector3f` now accepts TSC's `Position` class as a constructor
- **PIDS Scripting**
  - Add `TextWrapper.measureWidth()` to return the actual text width. Note that this cannot be chained for further usage, and must be invoked separately. 
  - Add `PIDSWrapper.isPlatformAutoDetected()`, returning whether the selected PIDS platform is manually picked by the user, or automatically detected.
- **Railway Signs (MTR 4 format)**
  - Allow coloring text with the `textColor` field. Format is the same as `backgroundColor`.

### Changes
- **Eyecandy**
  - The custom config NBT tags by ANTE will now be preserved.
  - The eyecandy model select UI is now overwritten by JCM in preparation for eyecandy custom config.
    - Though I have other plans for the UI, so it will likely be removed in future releases.
- **Scripting**
  - The `create()` function is changed to be re-invoked again after an execution error, instead of continuing towards `render()` function, where not all variable may be initialized, obscuring the original error in the create function.
  - When script debug mode is enabled, in-game script parsing error messages will now display on the first-time you join the game.
- **UI Changes:**
  - PIDS Projector will now reflect the position offset and rotation as you change the field, so you can precisely position it in real-time.
  - Numeric field no longer have the arrow button, as it's rarely used
  - Numeric field scrolling now allows finer increment when holding Shift (0.05), or Ctrl+Shift (0.025)
  - Numeric field now allows you to enter negative sign directly (`-`) after Ctrl+A.

### Fixes
- **PIDS Projector** can now render further away before disappearing.
- Fix **Automatic Iron Door** detecting players in spectator mode as well.

#### PIDS Textures
Please note that several textures used by PIDS (`rv_door_cls_apg.png`, `rv_door_cls_psd.png`, `rv_door_cls_train.png`, `thumbnail/pids_1a.png`) has been relocated from `jsblock:textures/block/pids` to `jsblock:textures/pids`.

It is done this way to avoid Minecraft including the texture to the block texture atlas, resulting in unnecessary overhead/enlarging of the block texture size.

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm), [CurseForge](https://curseforge.com/minecraft/mc-mods/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)