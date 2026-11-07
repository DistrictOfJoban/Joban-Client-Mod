# JCM v2.3.0 for MTR 4.0.5 has been released!

> Note: This release contains a large amount of internal changes. Some existing addons dependent on JCM may be incompatible until updated.

A non-exhaustive highlight of this release are as follows.
For more details, please review the changelog for previous betas/prereleases.

## General
- Add **Emergency Train Stop Button** (Wall mounted, TML/URL) (Thanks LX9702!)
- **Spot Lamp** & **Railway Sign Pole (MTR)** may now adapt to slab blocks.
- Added several optimizations (For both MTR & JCM), which is hoped to improve frame-rate and reduce memory usage.
- Fixes the "infamous" BVE Sound getting stuck bug.
- Cache rail node so that rail preview no longer disappear after long distances.
- Re-added the "sitting" feature in MTR 3.

## Technical

### Sign Coloring
- The text for MTR Railway Sign may now be colored using the `textColor` property.

### Scripting
- Add **Lift Scripting**, refer to the [documentation](https://jcm.joban.org/v2.3/dev/scripting/type/lift/) for details.
- A non-stablized method of adding custom config for Eyecandy has been added. Some ANTE eyecandy scripts which passively read config may now work.
- Various new APIs and changes for convenience of scripting. Refer to the docs & changelog for details.
- Add a new global `rateLimit` property for script entries.
- Fix a potential bug causing vehicle stops data to not be fetched.

### PIDS Scripting
This release introduces a somewhat breaking change for PIDS Projector:
- By default, PIDS Projector can no longer render out of bound.
  - This is done to conserve performance, in particular vanilla text rendering (`Text.create`) can be very heavy for frame-rate. (This is not related to JS execution speed)
  - Please manually add a `"outOfBound": true` property for the relevant preset in `joban_custom_resources.json` to true if:
    - Your script requires constant background running.
    - You experience your PIDS disappearing midway. (Note: There's some leniency for the out-of-bound rules, so e.g. a 2x1 is still acceptable)
    - You think the frame-rate impact of your PIDS is negligible. (e.g. AWT-drawn text)
- Added `Rect` for rendering a solid color quad, instead of needing `Texture`.

## Translations
Thanks to the following people who have contributed translations for this release! (No particular order):
- DelphoxOTS (ja_jp)
- vernacular7 (ko_kr)
- mathewofficial (nl_nl)

Translations for JCM is now hosted on [ZiYue's Weblate](https://weblate.ziyuesinicization.site/projects/joban-client-mod/)

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm), [CurseForge](https://curseforge.com/minecraft/mc-mods/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)