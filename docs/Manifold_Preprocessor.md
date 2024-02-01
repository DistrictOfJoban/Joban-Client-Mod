## Manifold Preprocessor
This is a preprocessor that is integrated to the Joban Client Mod projects for multi-target builds.

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

To avoid misleading IDE syntax error caused by code from other versions, the code for the main development configuration should be placed in the `#else` block.