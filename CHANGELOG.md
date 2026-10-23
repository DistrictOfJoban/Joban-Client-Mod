# JCM v2.3.0-beta.3 for MTR 4.0.5 has been released!

> **Beta Notice**
> 
> This is a beta release and is not recommended for stable deployment, nor is it intended for normal players to use. Please report any mod behaviors you feel could be improved.
> 
> Feature/implementation details may change throughout the beta lifecycle, any content you have made for this beta release may or may not break in the next version.


> This release contains breaking changes to JCM's internal codebase. Common addons have been tested against, however some addon depending on JCM may break.

## General
### Blocks
- Fix: Wall-attached blocks may now be attached to the backside of stairs, which is fully solid.

## Technical

### Scripting
- Perf Fix: Skip executing scripts during shadow render pass. (BugQuantumCore)
  - Some misc refactoring is also made to reduce performance impact of non-scripted elements.
- Add `MinecraftClient.getCameraMode()` to return the perspective of the player's camera. (F5)
- Add `GraphicsTexture.fullAA()` and `GraphicsTexture.noAA()` for configuring anti-aliasing behaviour.
- Add light parameter overload for `RenderManager.drawModel()`, for rendering a model with a particular light.
  - light value can be obtained via the newly added `MinecraftClient.lightAt(pos: Vector3f)`, which returns an integer value consisted of both the block & sky light level.
- Add the `rateLimit` property to script entries, which applies a global rate limit per-instance.
- Script instance whose execution time took more than 5s is now enlisted as **(Not Responding...)** in the Script Debug Overlay.
- Script that entered a loop spanning more than 10 seconds will now get aborted, for recovery from infinite loop.
- In-game script parsing error now contains the exception message when hovering over it, instead of requiring to view it in the Console.
- Fix `VanillaText.withHoverText()` using an erroneous type (MutableText instead of VanillaText) as its argument. Now it should behave according to the docs.

## Translations
Thanks to the following people who have contributed translations for this release! (No particular order):
- DelphoxOTS (ja_jp)
- Smile-Wood (zh_cn)
- Paaaaddy (ga_ie)

Translations for JCM is now hosted on [ZiYue's Weblate](https://weblate.ziyuesinicization.site/projects/joban-client-mod/)

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm), [CurseForge](https://curseforge.com/minecraft/mc-mods/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)