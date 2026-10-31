# JCM v2.3.0-prerelease.3 for MTR 4.0.5 has been released!

This is a prerelease, focused on bug-fixes and preparation before the official release.

## General
- Fix PIDS Projector unable to save selected platforms.
- Reduce performance impact when [hiding vehicle display cube](https://jcm.joban.org/latest/dev/scripting/features/vehicle_display_cube/).

## Technical

### Scripting
- Bump Rhino JS Engine to 1.9.2, see their [changelog](https://github.com/mozilla/rhino/releases/tag/Rhino1_9_2_Release) for details.
- Add `MTR.Data.getConnectedRails()` to obtain a list of rails connected to either a path/rail node position.
- Add `MTR.Data.getVehicleMountDetail()` to obtain the player's current car index & position in car.
- **PIDS Scripting:** Add `pids.arrivals().length()` to obtain the total arrivals available.
- **PIDS Scripting: Breaking Changes (Not code-related) (PIDS Projector only)**
  - This update "nerfed" the PIDS Projector such that it will skip rendering if out of the camera's bound (In-line with other PIDS), except:
    - If any of the configured X/Y/Z offset is >= 4m
    - If the configured scale is >= 4x
    - If explicitly configured in the PIDS Preset (Detailed below)
  - This is done to conserve performance, in particular vanilla text rendering (`Text.create`) can be very heavy for frame-rate. (This is not related to JS execution speed)
  - Please manually add a `"outOfBound": true` property for the relevant preset in `joban_custom_resources.json` to true if:
    - Your script requires constant background running.
    - You experience your PIDS disappearing midway. (Note: There's some leniency for the out-of-bound rules, so e.g. a 2x1 is acceptable)
    - You think the frame-rate impact of your PIDS is negligible. (e.g. AWT-drawn text)

## Translations
Translations for JCM is now hosted on [ZiYue's Weblate](https://weblate.ziyuesinicization.site/projects/joban-client-mod/)

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm), [CurseForge](https://curseforge.com/minecraft/mc-mods/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)