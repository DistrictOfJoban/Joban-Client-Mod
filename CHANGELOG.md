# JCM v2.3.0-prerelease.2 for MTR 4.0.5 has been released!

This is a prerelease, focused on bug-fixes and preparation before the official release.

## General
### MTR Patch
- Attempt to fix the "infamous" BVE Motor Sound bug in MTR 4, where an unrendered vehicle would result in the sound getting stuck.

## Technical

### Scripting
- Add `Resources.ANTE_FLAG` so scripts do not error out, though it is set to false.
- Fix `ConcurrentModificationException` when reading resources during Runtime Stage in previous beta release.

## Translations

Translations for JCM is now hosted on [ZiYue's Weblate](https://weblate.ziyuesinicization.site/projects/joban-client-mod/)

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm), [CurseForge](https://curseforge.com/minecraft/mc-mods/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)