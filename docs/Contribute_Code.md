# Code
**Note: Work In progress, many things are probably subject to change**

## Project Structure
Due to our rather complex requirements of having to support Fabric/Forge 1.16.5 - 1.20.*, we use the Minecraft-Mappings project to facilitate building lots of variants with a single codebase.

The primary development happens on Fabric, as such almost all the assets and codes are located in the `fabric` folder.
The main mod's code are located in `fabric/src/main/java/com/lx862/jcm/mod`, these files will be compiled by both fabric and forge.
The assets are located in `fabric/src/main/resources/assets/jsblock`, these files are also used by both fabric and forge.
Anything other than that is mod-loader specific.

When compiling Forge builds, the common code part (`fabric/src/main/java/com/lx862/jcm/mod`) will be copied over from the fabric folder to the forge folder for compilation.

## Introduction to Minecraft Mappings
**Minecraft Mappings** is a project that allows cross version (1.16.5+) and mod-loader compatibility with 1 single codebase.  
The basic idea is that *you don't use Minecraft's code*, instead use all the wrapper classes/method provided by **Minecraft Mappings**. (Which *usually* has the same name as Yarn mapping, under the `org.mtr.mapping` package)

**Minecraft Mappings** are generated as a jar for each Minecraft version and mod-loader, where they all have the same classes/method, and only the underlying implementation is changed to adapt to different Minecraft versions, as such nothing needs to be changed when compiling to each Minecraft version.

Note that not all method and classes can be mapped, for example one method could exist in 1.18.2 but not 1.17.1.  
As such, method that are guaranteed to exist across all version will be annotated with `@MappedMethod`, whereas other method that may not exist across all versions and mod-loaders will be annotated with `@Deprecated` (In which your IDE should give you a warning), don't use anything that's marked as deprecated as they may fail to compile across all versions and platform.  
When overriding method on classes provided by **Minecraft Mappings**, you should be overriding and calling methods made by Minecraft-Mappings (Sometimes named like `methodName2` or `methodName3`), these are methods provided by **Minecraft Mappings**, whereas `methodName` is usually the underlying method provided by Minecraft.

**Minecraft Mappings** also provide helper classes (For example the Vanilla Registry) on components with radical changes between versions.
### What if Minecraft Mappings doesn't contain the code I want
In general there are 2 options:
1. Contribute to [Minecraft-Mappings](https://github.com/Minecraft-Transit-Railway/Minecraft-Mappings) by following the instruction there.
2. A backup solution is using manifold preprocessing to conditionally apply code (To be continued....)

### What about RenderSystem?
Not all Minecraft's code are obfuscated, for example some of the code in `com.mojang.blaze3d` including RenderSystem is not obfuscated, a mapping like Yarn or Mojmap is not needed and there's a much less chance that the method name will differ across mod-loaders due to them using different mappings.  
If you really find yourself not being able to compile across all versions and mod-loaders when using code that's not provided by Minecraft Mappings, you can follow the section above.

## Code Guidelines
### Naming Convention
Use Java naming convention (TLDR: InterfaceName, ClassName, fieldName, methodName)

### Exception handling
If you do not have any plan to handle the exception appropriately (Other than logging), you should just not try/catch and let the parent class handle the exception.