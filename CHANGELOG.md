# JCM v2.3.0-beta.1 for MTR 4.0.5 has been released!

## Additions
- Add **Emergency Train Stop Button (Wall mounted, TML)** & URL variant (Thanks **LX9702**!)

## Changes
- **PIDS Scripting**
  - Add `PIDSWrapper.isPlatformAutoDetected()`, returning whether the selected PIDS platform is manually picked by the user, or automatically detected.
- When script debug mode is enabled, in-game script parsing error messages will now display the first-time you join the game.

### PIDS Textures
Please note that several textures used by PIDS (`rv_door_cls_apg.png`, `rv_door_cls_psd.png`, `rv_door_cls_train.png`, `thumbnail/pids_1a.png`) has been relocated from `jsblock:textures/block/pids` to `jsblock:textures/pids`.

It is done this way to avoid Minecraft including the texture to the block texture atlas, resulting in unnecessary overhead/enlarging of the block texture size.

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm), [CurseForge](https://curseforge.com/minecraft/mc-mods/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)