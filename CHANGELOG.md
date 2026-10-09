# JCM v2.3.0-beta.2 for MTR 4.0.5 has been released!

> **Beta Notice**
> 
> This is a beta release and is not recommended for stable deployment, nor is it intended for normal players to use. Please report any mod behaviors you feel could be improved.
> 
> Feature/implementation details may change throughout the beta lifecycle, any content you have made for this beta release may or may not break in the next version.


> This release contains breaking changes to JCM's internal codebase. Common addons have been tested against, however some addon depending on JCM may break.


## Technical

### Lift Scripting
- Fix `isScriptRendered` not allowing player to board the lift.

### Eyecandy Scripting
- Add `EyecandyWrapper.facingAngle()`, which returns the block facing angle directly.
  - Please use this instead of relying on `EyecandyWrapper.facing().asRotation()`, which will no longer work in MTR 4.1.

## Translations
Thanks to the following people who have contributed translations for this release! (No particular order):
- TODO

Translations for JCM is now hosted on [ZiYue's Weblate](https://weblate.ziyuesinicization.site/projects/joban-client-mod/)

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm), [CurseForge](https://curseforge.com/minecraft/mc-mods/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)