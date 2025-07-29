## JCM (v2.0.0-prerelease.4) for (MTR 4.0.0-prerelease.3) has been released!

**Additions:**
- **Scripting:**
- - Add Script Debug Overlay to display all script instances.
- - - Toggleable in **JCM Settings > Enable debug mode**
- - - Text highlight color represents the execution speed (Blue = Normal, Yellow = Slow, Red = Very Slow!)
- - - - Script authors should generally aim for Blue color
- - Add back `ctx.setDebugInfo()` from NTE
- - Add `ctx.debugModeEnabled()` to determine if JCM debug mode is enabled.

**Changes:**
- **Operator Button** will no longer accept driver keys that are expired. (Creative keys are always accepted)
- **Scripting**
- - Due to popular demand, an option is added in JCM config to disable scripting class restriction entirely, this allows any external java packages to be used just like in NTE.

**Fixes:**
- Fix horizontal blocks (Double ceiling/Trespass Sign) potentially breaking when stacked with WorldEdit
- **Scripting**
- - Texts with marquee overflow now no longer disappear for a prolonged duration of time before resetting.

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)
