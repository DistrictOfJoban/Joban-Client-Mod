# JCM v2.2.0-beta.3 for MTR 4.0.4 has been released!

## Additions
- Add `RenderManager.drawModel(model: Model | DynamicModelHolder, matrices: Matrices?)` as an alternative way to draw model with RenderManager.
- Add `VehicleScriptContext.drawCarModel` / `EyecandyScriptContext.drawModel` with `DynamicModelHolder` overload instead of `Model`.
- Add `VehicleScriptContext.getCarBogieRenderManager()` for rendering with transformation of the bogie.
- Add more overload for `GraphicsTexture.upload()`, allowing for uploading selected portion of image to conserve performance.

## Changes
- Scripted PIDS now also shows an in-game error in the chat, with script debug mode enabled.

## Fixes
- **(Non-scripting)** Fix JCM PIDS auto platform detection range, now the platform detected should behave the same as MTR PIDS.

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm), [CurseForge](https://curseforge.com/minecraft/mc-mods/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)