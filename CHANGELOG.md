## JCM (v2.0.0-beta.9) for (MTR 4.0.0-beta.16) has been released!

**Changes:**
- **Scripting**
- - As it is expected that most if not all scripts needed to be ported, some dummy/compatibility functions has been removed:
- - - `Resources.getNTEProtoVersion()`
- - - `Resources.getNTEVersionInt()`
- - - `Resources.getNTEVersion()`
- - - `Resources.getMTRVersion()`
- - - `Resources.readNbtString()`
- - - - Replacement: `Resources.getAddonVersion("mtr")`
- - To prevent misuse of scripts, script may now only access java class from the following packages:
- - - `com.lx862.jcm.mod.scripting.*`
- - - `org.mtr.*`
- - - `java.awt.*`
- - - `java.lang.*`
- - - `sun.java2d.*`

**Fixes:**
- Compatibility with MTR 4 beta-16
- **JSON PIDS Preset**
- - Fix text potentially overlapping when platform number is hidden
- **Scripted PIDS Preset**
- Fix the last parameter in `create()` function potentially being called with null

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)
