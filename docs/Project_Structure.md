# Project Structure
Joban Client Mod development happens on the Fabric modloader, where the main source code and debugging happens. Whereas Forge/other modloaders are only for compiling.
As such almost all the assets and codes are located in the `fabric` folder.

The main mod's code are located in `fabric/src/main/java/com/lx862/jcm/mod`, these files will be compiled by all modloaders. (If you have experience with Architectury, this is technically the common module)
The assets are located in `fabric/src/main/resources/assets/jsblock`, these files are also used by both fabric and forge.
Anything other than that is mod-loader specific.

Because the main mod's code are located in the fabric module, you need to run `forge:setupFiles` in gradle to copy the source code from Fabric to Forge. This is done automatically when using GitHub action to compile.