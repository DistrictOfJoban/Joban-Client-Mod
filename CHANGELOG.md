# JCM v2.1.0-beta.2 for MTR 4.0.2 has been released!

**Note: Normal players are *strongly* advised to wait until the official release of v2.1.0.**

## New (Scripting Implementation)
### Scripting Engine
- Update to **Rhino 1.8.1** (Minecraft 1.17+) & **Rhino 1.7.15.1** (Minecraft 1.16.5)
- - Fix a problem with formatting of floating-point numbers to strings that may result in very bad performance in some cases. (Rhino Changelog)

### Execution
- Scripts may now execute across 4 different threads (Instead of only 1 background thread), which should improve the amount of scripts that can be executed simultaneously. (zbx1425 for the idea)

### Networking
- **Breaking change:** A failed HTTP request via the `Networking` class no longer throws an exception.
- - Use `NetworkResponse.success()` to check whether the request succeeded
- - Use `NetworkResponse.exception()` to get the raw java exception.
- Add `NetworkResponse.ok()` to check whether the response code returned by the server is successful. (HTTP Request Code >= 200 & <= 299)

### Eyecandy Scripting
- Add `EyecandyBlockEntity.redstoneSignal()` to process redstone signal in the block.
- - Note: This only returns 0 and 15 (lit & unlit) with the current implementation. Scripts should however account for future changes, where the true signal level is returned.
- Experimental: Add `EyecandyBlockEntity.setCollisionShape()` to set the client-side collision shape of the block.
- - At most 1x1x1 (16x16x16 block unit), anything higher may cause the collision shape to not work as intended.

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm), [CurseForge](https://curseforge.com/minecraft/mc-mods/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)