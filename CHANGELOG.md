# JCM v2.2.0 for MTR 4.0.4 has been released!

## MTR Enhancements
This release brings several enhancements for the MTR mod. Notable additions including the lift "ding" sound from MTR 3, basic car auto-filling in sidings and more rendering options.

See the [documentation](https://jcm.joban.org/v2.2/players/features/mtr-patch/) for details.

## Vehicle Scripting
This release added Vehicle Scripting, allowing scripts to be used on vehicles. (For players, note that not all MTR 3 scripts will work out of the box)

This is paired with other scripting-related changes, such as the re-introduction of a more complete [model loading API](https://jcm.joban.org/v2.2/dev/scripting/model).

Please check the [Vehicle Scripting docs](https://jcm.joban.org/v2.2/dev/scripting/type/vehicle/) and the [MTR-NTE Compatibility](https://jcm.joban.org/v2.2/dev/scripting/articles/nte_migration/) page for more details.

## Other scripting changes
- The Rhino JS Engine is updated to use 1.9.
- **Eyecandy Scripting:** Improved MTR-NTE Compatibility.
- **PIDS Scripting:** Allow manual z-ordering.
- Added Console API, allowing use of `console.log` and alike functions.
- The Java class restriction has been revised, it is expected most scripts to remain unaffected.
  - `java.lang.System` is now restricted by default.
    - For `currentTimeMillis()` and `nanoTime()`, please use the counterpart from `Timing` instead, which functions equivalently.
  - `java.lang.Class` and other misc classes has been restricted.
  - As usual, you may still turn off the scripting restriction in settings.

For the full list of changes, please refer to the changelogs of the previous v2.2 beta releases.

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm), [CurseForge](https://curseforge.com/minecraft/mc-mods/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)