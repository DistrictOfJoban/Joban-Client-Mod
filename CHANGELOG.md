# JCM v2.3.0-prerelease.3 for MTR 4.0.5 has been released!

This is a prerelease, focused on bug-fixes and preparation before the official release.

## Technical

### Scripting
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
Thanks to the following people who have contributed translations for this release! (No particular order):

Translations for JCM is now hosted on [ZiYue's Weblate](https://weblate.ziyuesinicization.site/projects/joban-client-mod/)

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm), [CurseForge](https://curseforge.com/minecraft/mc-mods/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)