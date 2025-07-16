## JCM (v2.0.0-prerelease.4) for (MTR 4.0.0-prerelease.3) has been released!

**Addition:**
- **Scripting:**
- - Add Script Debug Overlay to display all script instances.
- - - Toggleable in **JCM Settings > Enable debug mode**
- - - Text highlight color represents the execution speed (Blue = Normal, Yellow = Slow, Red = Very Slow!)
- - - - Script authors should generally aim for Blue color
- - Add back `ctx.setDebugInfo()` from NTE
- - Add `ctx.debugModeEnabled()` to determine if JCM debug mode is enabled.

**Changes:**
- **Scripting**
- - Due to popular demand, an option is added in JCM config to disable scripting class restriction entirely, this allows any external java packages to be used just like in NTE. 

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)
