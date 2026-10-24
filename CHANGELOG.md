# JCM v2.3.0-prerelease.1 for MTR 4.0.5 has been released!

> This release contains breaking changes to JCM's internal codebase. Common addons have been tested against, however some addon depending on JCM may break.

## General
- Fix Forge crashing on launch

## Technical

### Scripting
- Compiled scripts are now re-used during parsing stage, which may speed up parsing time for packs with many reference to a single script.
- When passing in JS objects in `print()` or `console` functions, they will now output the corresponding JSON string instead of `[object Object]`.
- When encountering a runtime error, only the JS stacktrace will now be shown to reduce clutter.
- For convenience, all non-deprecated methods with an `Identifier` argument now also has a corresponding `String` variant. Thus you can pass in a string like `"mtr:font"` instead of wrapping it with `Resources.id()`.
  - For `Resources.idr()`, simply prepend your string with `./`.
  - Note: This can still only be used during parsing stage.
- `StateTracker.setState()` would now return a boolean on whether the state is changed or not, instead of needing a separate check.

## Translations
Thanks to the following people who have contributed translations for this release! (No particular order):
TODO

Translations for JCM is now hosted on [ZiYue's Weblate](https://weblate.ziyuesinicization.site/projects/joban-client-mod/)

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm), [CurseForge](https://curseforge.com/minecraft/mc-mods/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)