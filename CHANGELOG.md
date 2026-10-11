# JCM v2.3.0-beta.2 for MTR 4.0.5 has been released!

> **Beta Notice**
> 
> This is a beta release and is not recommended for stable deployment, nor is it intended for normal players to use. Please report any mod behaviors you feel could be improved.
> 
> Feature/implementation details may change throughout the beta lifecycle, any content you have made for this beta release may or may not break in the next version.


> This release contains breaking changes to JCM's internal codebase. Common addons have been tested against, however some addon depending on JCM may break.


## Technical

### General
- Add `MinecraftClient.getCameraMode()` to return the perspective of the player's camera. (F5)
- Add light parameter overload for `RenderManager.drawModel()`, for rendering a model with a particular light.
  - light value can be obtained via the newly added `MinecraftClient.lightAt(pos: Vector3f)`, which returns an integer value consisted of both the block & sky light level.

### Vehicle Scripting
- Add `VehicleWrapper.isDoorOpenable()` and `VehicleWrapper.isDoorOpen()`, given the car number and a position relative to the center of the car.

### Lift Scripting
- Fix `isScriptRendered` not allowing player to board the lift.
- Add `LiftWrapper.isDoorway1Openable()`, `LiftWrapper.isDoorway2Openable()`, `LiftWrapper.isDoorway1Open()` and `LiftWrapper.isDoorway2Open()` for checking open doorways.

### Eyecandy Scripting
- Add `EyecandyWrapper.facingAngle()`, which returns the block facing angle directly.
  - Please use this instead of relying on `EyecandyWrapper.facing().asRotation()`, which will no longer work in MTR 4.1.

## Translations
Thanks to the following people who have contributed translations for this release! (No particular order):
- TODO

Translations for JCM is now hosted on [ZiYue's Weblate](https://weblate.ziyuesinicization.site/projects/joban-client-mod/)

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm), [CurseForge](https://curseforge.com/minecraft/mc-mods/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)