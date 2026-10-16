# JCM v2.3.0-beta.2 for MTR 4.0.5 has been released!

> **Beta Notice**
> 
> This is a beta release and is not recommended for stable deployment, nor is it intended for normal players to use. Please report any mod behaviors you feel could be improved.
> 
> Feature/implementation details may change throughout the beta lifecycle, any content you have made for this beta release may or may not break in the next version.


> This release contains breaking changes to JCM's internal codebase. Common addons have been tested against, however some addon depending on JCM may break.
 
This is an interim beta release.

## General
### MTR Patch
- (Fabric-only) Preserves rail preview of long-running rail (i.e. Outside the render distance), instead of disappearing.
- For MTR's multi-item selection screen, you can now hold `Shift` to exclusively select one item, instead of needing to add the new item then removing the existing item.
- **Introduce experimental Frustum culling**
  - MTR 4 uses occlusion culling, which would perform tracing to check whether elements behind terrains are visible from the player's position.
  - This update adds Frustum Culling as another layer on top of Occlusion Culling, which simply checks whether the elements are within the camera bound at all. Therefore if you aren't looking at the element, it will skip being rendered. (Scripts will continue to be invoked just like Occlusion Culling.)
  - If you see any visual artifacts caused by this, please report.
- Re-add camera-lowering feature from MTR 3 by sneaking. (Previously branded as "sitting")
  - May be turned off in JCM Settings (Revert to MTR 4 behaviour)

## Technical

### General
- Add `MinecraftClient.getCameraMode()` to return the perspective of the player's camera. (F5)
- Add light parameter overload for `RenderManager.drawModel()`, for rendering a model with a particular light.
  - light value can be obtained via the newly added `MinecraftClient.lightAt(pos: Vector3f)`, which returns an integer value consisted of both the block & sky light level.

### Vehicle Scripting
- Add `VehicleWrapper.isDoorOpenable()` and `VehicleWrapper.isDoorOpen()`, given the car number and a position relative to the center of the car.

### Lift Scripting
- Fix `isScriptRendered` not allowing player to board the lift.
- Rename `isScriptRendered` to `isScriptControlled`
- Add `LiftWrapper.isDoorway1Openable()`, `LiftWrapper.isDoorway2Openable()`, `LiftWrapper.isDoorway1Open()` and `LiftWrapper.isDoorway2Open()` for checking open doorways.

### Eyecandy Scripting
- Rename `isScriptRendered` to `isScriptControlled`
- Add `EyecandyWrapper.facingAngle()`, which returns the block facing angle directly.
  - Please use this instead of relying on `EyecandyWrapper.facing().asRotation()`, which will no longer work in MTR 4.1.

## Translations
Thanks to the following people who have contributed translations for this release! (No particular order):
- BugQuantumCore & Mewzica (zh_cn)
- DelphoxOTS (ja_jp)
- Fed (it_it)

Translations for JCM is now hosted on [ZiYue's Weblate](https://weblate.ziyuesinicization.site/projects/joban-client-mod/)

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm), [CurseForge](https://curseforge.com/minecraft/mc-mods/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)