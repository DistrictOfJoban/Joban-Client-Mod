# JCM v2.3.0-beta.1 for MTR 4.0.5 has been released!

## General
### New Blocks
- Add **Emergency Train Stop Button (Wall mounted, TML)** and **URL variant** (Thanks **LX9702**!)

### Blocks Slab Support
- The **Spot Lamp** block in JCM will now descend/ascend in accordance to slab blocks attached.
- The **Railway Sign Poles** in MTR now gained the ability to extend the pole according to the slab above.

### Fixes
- Fix JCM having an overly-long keybinding description, causing the Minecraft keybind page to shift outside the game window.
- Previously **Spot Lamp** will always prefer attaching to the top block if available. 
  - Now it will respect attaching to the side the player clicked on.
- **PIDS Projector** can now render further away before disappearing.
- Fix **Automatic Iron Door** detecting players in spectator mode as well.
- The playing mechanism for **Sound Looper** has been revised.
  - In JCM v1, the sounds are played to everyone across the server, which makes it a reliable source for playing all sorts of audio.
  - In JCM v2, it would only play to nearby players. Which means player may not be able to hear anything after getting in-range, until another loop occurs.
  - In this update, the range is now changed dynamically based on the duration. This allows short-form looping audio to play for nearby players, while having further range for long-form audio.
    - This should hopefully make sound looper more reliable.

## Technical

### PIDS Projector UI Improvement
It now supports real-time position/rotation preview, and you may now type the negative sign (-) as the first character.

### Railway Sign Text Coloring
- Allow coloring text with the `textColor` field in the mtr_custom_resources.json "sign" section. Format is the same as `backgroundColor`.

### Lift Scripting
Lift Scripting has been added for review and feedback by the public.

See [JCM Docs](https://jcm.joban.org/v2.3/dev/scripting/type/lift/) for documentation.

### PIDS Scripting
- Add `TextWrapper.measureWidth()` to return the actual text width. Note that this cannot be chained for further usage, and must be invoked separately.
- Add `PIDSWrapper.isPlatformAutoDetected()`, returning whether the selected PIDS platform is manually picked by the user, or automatically detected.
- Add `RectangleWrapper`, which is similar to `TextureWrapper` with the texture id pointing to `mtr:textures/block/white.png`
  - PIDS relying on a white texture previously should change to use `RectangleWrapper` (`Rectangle.`), as it is guaranteed the output will be a solid color, even if such texture is moved/no longer available in future Minecraft/MTR versions.

### General Scripting
- Added `ctx.setDebugInfo(value: any)` shorthand for temporary, single-value on-screen debug info, without requiring a key.
- Same as calling `ctx.setDebugInfo("<Untitled>", value)`
- Added `isScriptRendered` field for eyecandy and lift entry, which allows script to fully take over the rendering, without MTR's default renderer.
- Added `PlayerEntity.displayName()` to return the player's name with team prefixes.
- Added `PlayerEntity.isSpectator()` and `PlayerEntity.isCreative()`
- `Vector3f` now accepts TSC's `Position` class as a constructor
- The `create()` function is changed to be re-invoked again after an execution error, instead of continuing towards `render()` function, where not all variable may be initialized, obscuring the original error in the create function.
- When script debug mode is enabled, in-game script parsing error messages will now display on the first-time you join the game.

#### MTR Class Wrapper
- Added the `MTRWrapper` class (Referenced using `MTR` in scripts).
  - This is a wrapper for various MTR utilities and data obtaining functions. (`MTR.ClientConfig`, `MTR.Data`)
  - It aims to reduce dependency on `MTRClientData` and other internal MTR class access, as there may be breaking changes made in MTR 4.1.
  - Unlike `MTRClientData`, backward compatibility will be considered in a best-effort basis, to ensure existing scripts do not break badly.
    - Note: Returned type from TSC is still vulnerable, however from observations it is more stablized than the MTR Mod's code.

### Changes
- **Eyecandy**
  - The custom config NBT tags by ANTE will now be preserved.
  - The eyecandy model select UI is now overwritten by JCM in preparation for eyecandy custom config.
    - Though I have other plans for the UI, so it will likely be removed in future releases.


### PIDS Textures
Please note that several textures used by PIDS (`rv_door_cls_apg.png`, `rv_door_cls_psd.png`, `rv_door_cls_train.png`, `thumbnail/pids_1a.png`) has been relocated from `jsblock:textures/block/pids` to `jsblock:textures/pids`.

It is done this way to avoid Minecraft including the texture to the block texture atlas, resulting in unnecessary overhead/enlarging of the block texture size.

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm), [CurseForge](https://curseforge.com/minecraft/mc-mods/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)