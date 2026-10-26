# JCM v2.3.0-prerelease.2 for MTR 4.0.5 has been released!

This is a prerelease, focused on bug-fixes and preparation before the official release.

## General
### MTR Patch
- Attempt to fix the "infamous" BVE Motor Sound bug in MTR 4, where an unrendered vehicle would result in the sound getting stuck.

## Technical

### Scripting
- **PIDS Scripting - The newly added "Rectangle" has been renamed to "Rect"**
  - This is to avoid name conflict of existing scripts using  AWT's Rectangle.
- Add `Resources.ANTE_FLAG` so scripts do not error out, though it is set to false.
- Add basic compat for `SoundHelper`/`TickableSound` in ANTE.
- Added the dummy `Resources.mgr()` alias that previously existed in NTE.
- Add `Resources.id(id: Identifier, suffix: String)` to obtain a new identifier with `suffix` being appended to the path.
  - Can be used with a "base path" via `idr()`, then resolving it in runtime with this function.
- If you invoke a Java method with an "undefined" as one of its parameter, it will show a very long `Can't find method...` error, since it cannot match the method signature.
  - The Rhino Undefined class will now be replaced with `UNDEFINED` in the error message, instead of the full class name.
  - An extra warning will be shown in console, notifying that you have passed an undefined object as one of its param.
- Fix `ConcurrentModificationException` when reading resources during Runtime Stage in previous beta release.

## Translations

Translations for JCM is now hosted on [ZiYue's Weblate](https://weblate.ziyuesinicization.site/projects/joban-client-mod/)

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm), [CurseForge](https://curseforge.com/minecraft/mc-mods/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)