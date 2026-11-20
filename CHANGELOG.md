# JCM v2.3.1 for MTR 4.0.5 has been released!

This is a minor release focused on bug-fixes.

## General
- Fix typing an alphabet into a numeric GUI text field could crash the client
- Fix inappropriately configured Sound Looper could repeatedly crash the server
- Fix Fare Saver's "Prefix"/"Currency" feature not working.
- Fix inconsistent behavior and bugs on Forge version, as compared to Fabric.

## Technical

### Scripting
- Add `QuadDrawCall.light()` for rendering the quad with a custom light value.
- Revise Scripting Restriction list, most scripts should be unaffected.
- **Vehicle Scripting:**
  - Fix `isRendered()` and `isCarRendered()` sometimes returning false when onboard a train, even though it is rendered.
- **Eyecandy Scripting:**
  - Fix`EyecandyWrapper.isCrosshairTarget()` not returning a consistent value with vanilla in adventure mode.
- **PIDS Scripting:**
  - Fix `TextWrapper.shadowed()` having the shadow rendered in-front of the text instead of behind.

## Translations
Thanks to the following people who have contributed translations for this release! (No particular order):
- vernacular7 (ko_kr)

Translations for JCM is now hosted on [ZiYue's Weblate](https://weblate.ziyuesinicization.site/projects/joban-client-mod/)

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm), [CurseForge](https://curseforge.com/minecraft/mc-mods/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)