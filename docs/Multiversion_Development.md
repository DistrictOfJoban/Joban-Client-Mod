# Introduction to Multiversion Development

## The Minecraft-Mappings Project
**Minecraft Mappings** is a project made for the MTR Mod that allows cross version (1.16.5+) and mod-loader compatibility with 1 single codebase.  
The basic idea is that *you don't use Minecraft's code*, instead use all the wrapper classes/method provided by **Minecraft Mappings**. (Which *usually* has the same name as Yarn mapping, under the `org.mtr.mapping` package)

**Minecraft Mappings** are generated as a jar for each Minecraft version and mod-loader, where they all have the same classes/method, and only the underlying implementation is changed to adapt to different Minecraft versions, as such nothing needs to be changed when compiling to each Minecraft version.

Note that not all method and classes can be mapped, for example one method could exist in 1.18.2 but not 1.17.1.  
As such, method that are guaranteed to exist across all version will be annotated with `@MappedMethod`, whereas other method that may not exist across all versions and mod-loaders will be annotated with `@Deprecated` (In which your IDE should give you a warning), don't use anything that's marked as deprecated as they may fail to compile across all versions and platform.  
When overriding method on classes provided by **Minecraft Mappings**, you should be overriding and calling methods made by Minecraft-Mappings (Sometimes named like `methodName2` or `methodName3`), these are methods provided by **Minecraft Mappings**, whereas `methodName` is usually the underlying method provided by Minecraft.

**Minecraft Mappings** also provide helper classes (For example the Vanilla Registry) on components with radical changes between versions.

### There's no mapping for RenderSystem but I want to use it
For RenderSystem you can just use the class provided by Minecraft directly. Not all Minecraft's code are obfuscated, for example some of the code in `com.mojang.blaze3d` including RenderSystem is not obfuscated, a mapping like Yarn or Mojmap is not needed and there's a much less chance that the method name will differ across mod-loaders due to them using different mappings.  
If you really find yourself not being able to compile across all versions and mod-loaders when using code that's not provided by Minecraft Mappings, you can follow the section below.

### What if Minecraft Mappings doesn't contain the code I want
In general there are 2 options:
1. Contribute to [Minecraft-Mappings](https://github.com/Minecraft-Transit-Railway/Minecraft-Mappings) by following the instruction there.
2. Use the manifold preprocessor to compile different code conditionally depending on the build configuration (Easy way out :P), see below for more information.

## Manifold Preprocessor
This is a java preprocessor that is integrated to the Joban Client Mod projects for multi-target builds.

### Usage
To conditionally run a code, you can use the `#if` and `#endif` block:
```
    #if MC_VERSION == "11904"
        System.out.println("Now running on 1.19.4");
    #elif MC_VERSION == "11802"
        System.out.println("Now running on 1.18.2");
    #else
        System.out.println("Not running on 1.19.4 nor 1.18.2"); 
    #endif
```

You can also compare the minecraft version:
```
    #if MC_VERSION > "11904"
        System.out.println("Running on 1.19.4 or above (e.g. 1.20)");
    #else
        System.out.println("Running below 1.19.4"); 
    #endif
```

If you have the [Manifold Plugin](https://plugins.jetbrains.com/plugin/10057-manifold) installed for IntelliJ, the code inside the `#if` and `#elif` block would have a distinct gray background, and there will not be any syntax highlighting.

Available variable as follows:

| Variable Name | Description                                                                        | Example |
|---------------|------------------------------------------------------------------------------------|---------|
| MC_VERSION    | The Minecraft version this build is for, in the format MAJOR(1)MINOR(2)PATCH(2)    | 11902   |
| LOADER        | The modloader this build is for, possible values are `fabric`, `forge`, `neoforge` | fabric  |

The preprocessing should target the main development configuration (Fabric, Latest MC Version).

To avoid misleading IDE syntax error caused by code from other versions, the code for the main development configuration (Fabric, latest MC version) should be placed in the `#else` block.