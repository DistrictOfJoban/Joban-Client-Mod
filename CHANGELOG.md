## JCM (v2.0.0-prerelease.1) for (MTR 4.0.0-prerelease.2) has been released!

**Changes:**
- Update Japanese translations (Thanks **CinderaceOTS**!)
- To adapt to the new train driver key changes in MTR 4, **Operator Button** now allows choosing which driver keys are allowed.
- - Configurable via brush
- The JCM **Standing Station Name Block** has been moved to the official MTR Mod!
- - The JCM variant will be kept for backward compatibility, but will no longer be obtainable in the inventory.
- **Scripting**
- - Reverse stance on backward compatibility, JCM will try to facilitate with migration of existing scripts. Functions to check for MTR & NTE version has been re-added.
- - Add work-in-progress vehicle scripting, *not useful to developer at the minute as it's still missing many functions*. If you see more error than usual in your console log, well you know why.
- - `Matrices.rotateX`, `Matrices.rotateY`, `Matrices.rotateZ` now uses radian instead of degrees, just like how they were in NTE.
- - - Use `Matrices.rotateXDegrees`, `Matrices.rotateYDegrees`, `Matrices.rotateZDegrees` to continue rotating with degrees.

**Fixes:**
- Fix some block disappearing when migrating to JCM v2.
- Fix JSON PIDS ETA text potentially not following the destination's language
- Fix Scripted PIDS Preset not working reliably
- Fix some textures not being power-of-two resolution. The mipmap level can now be raised to 2 with solely MTR and JCM installed.
- Fix block with multiple parts still dropping item in creative when broken

**MTR 4.0.0-prerelease.2 may crash with older JCM releases (i.e. beta.13), please update to this version instead!**

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)
