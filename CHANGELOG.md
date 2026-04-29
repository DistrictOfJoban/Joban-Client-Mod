# JCM v2.2.0-beta.3 for MTR 4.0.4 has been released!

## Additions
- **(Non-scripting, MC 1.19.4 or above)** Add mouse hover tooltips in the MTR Dashboard, showing more details about a platform/siding.
- Add `RenderManager.drawModel(model: Model | DynamicModelHolder, matrices: Matrices?)` as an easy way to draw a model.
  - This ought to replace `ModelDrawCall` and all variants of `drawModel`/`drawXXXModel` in favor of a unified RenderManager.
  - Of course compatibility is still retained, no code has been removed.
- Allow passing in a `DynamicModelHolder` for all places where a `Model` is accepted as a parameter for rendering.
  - The need to use this over `getUploadedModel` is explained in the [documentation](https://jcm.joban.org/v2.2/dev/scripting/model/#dynamicmodelholder). (*Avoid using getUploadedModel() for rendering*)
- Add `VehicleScriptContext.getCarBogieRenderManager()` for rendering with transformation of the bogie.
- Add `RawMeshBuilder.asRawModel(): RawModel` as a quick way to obtain an instance of `RawModel`.
  - Equivalent to `new RawModel()` and `RawModel.append(rawMesh: RawMesh)`.
- Add more overload for `GraphicsTexture.upload()`, allowing for uploading selected portion of image to conserve performance.

## Changes
- `Resources.getNTEVersion()` now returns the current Minecraft version in use, instead of hard-coded to "1.19.2".
- Scripted PIDS now also shows an in-game error in the chat, with script debug mode enabled.

## Fixes
- Fix Forge version crashing with some mods sharing the same config library as JCM. 
- **(Non-scripting)** Fix JCM PIDS auto platform detection range, now the platform detected should behave the same as MTR PIDS.

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm), [CurseForge](https://curseforge.com/minecraft/mc-mods/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)