# Code
**Note: Work In progress, many things are probably subject to change**

## Project Structure
Due to our rather complex requirements of having to support Fabric/Forge 1.16.5 - 1.20.3, we uses the Minecraft-Mappings project to facilitate building lots of variants with a single codebase.

The primary development happens on Fabric, as such almost all the assets and codes are located in the `fabric` folder.
The main mod's code are located in `fabric/src/main/java/com/lx862/jcm/mod`, these files will be compiled by both fabric and forge.
The assets are located in `fabric/src/main/resources/assets/jsblock`, these files are also used by both fabric and forge.
Anything other than that is modloader specific.

When compiling Forge builds, the common code part (`fabric/src/main/java/com/lx862/jcm/mod`) will be copied over from the fabric folder to the forge folder for compilation.

## Minecraft Mappings
**Minecraft Mappings** is a project that allows cross version (1.16.5+) and modloader compatibility with 1 single codebase.  
The very basic idea is that **you don't use Minecraft's code**, instead use all the wrapper classes/method provided by **Minecraft Mappings**. (Which *usually* has the same name as Yarn mapping, under the `org.mtr.mapping` package)

**Minecraft Mappings** will generate artifacts as a jar for each version and modloader, they all have the same classes/method, and only the underlying implementation is changed to adapt to different Minecraft versions, as such nothing needs to be changed when compiling to each Minecraft version.

Note that not all method and classes can be mapped, for example one method could exist in 1.18.2 but not 1.17.1.  
As such, method that are guarenteed to exist across all version will be annotated with `@MappedMethod`, whereas other method that may not exist across all versions and modloaders will be annotated with `@Deprecated` (In which your IDE should give you a warning), don't use anything that's marked as deprecated as they may fail to compile acoss all versions and platform.

Also when overriding method on classes provided by **Minecraft Mappings**, you should be overriding and calling methods named like `methodName2` or `methodName3`, these are methods provided by **Minecraft Mappings**, whereas `methodName` is for example, the underlying method provided by Minecraft.

### Ok but Minecraft Mappings does not contain the code I want
To be done. TL;DR use the manifold preprocessing, or contribute the code mappings directly to upstream.

### What about RenderSystem?
Not all Minecraft's Code are obfuscated, for example some of the code in `com.mojang.blaze3d` including RenderSystem is not obfuscated, a mapping like Yarn or Mojmap is not needed and there's a much less chance that the method name will differ across modloaders due to them using different mappings.

If you really find yourself not being able to compile across all versions and modloaders when using code that's not provided by Minecraft Mappings, again follow the steps above.

## Code Guidelines
### Naming Convention
Use Java naming convention (TLDR: InterfaceName, ClassName, fieldName, methodName)

### Error handling
Don't just try/catch e.printStackTrace() and move on, not everyone stares at the console when playing.

At best user will report the stacktrace in the console to you, you found and fixed the issues.  
At worst, you'll run into state issues leading to crash that are entirely unrelated to the things you should be looking for, have part of your functonality simply "broken" and potentially dropping the client performance if the code just errors out every frame.

To be continued...