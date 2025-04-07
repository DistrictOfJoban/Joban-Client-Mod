## JCM (v2.0.0-beta.11) for (MTR 4.0.0-beta.16) has been released!

**Added:**
- **Scripting**
- - Added [Networking](https://www.joban.org/wiki/JCM:Scripting:Documentation:Utilities#Networking) to enable networking functionalities in scripts
- - Added [Files](https://www.joban.org/wiki/JCM:Scripting:Documentation:Utilities#Files) for basic data saving functionalities in scripts
- - Implement `MinecraftClient.worldIsRainingAt` functionality
- - Add `java.util.*` package to whitelist
- - Add `Timing.currentTimeMillis()` and `Timing.nanoTime()`, these are equivalent methods to `java.lang.System.currentTimeMillis()` and `java.lang.System.currentTimeMillis()`. It is advisable to use this over java.lang.System as it may no longer be available soon.

**Fix:**
- Fix circular route not displayed in JCM PIDS
- Fix miscalculation of PIDS arrival time, now they should more accurate.
- Fix Scrollbar in GUI screens shifting away when dragged with mouse

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)
