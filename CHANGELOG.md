## JCM v2.0.0 for MTR 4.0 has been released!

**Changes:**
- **Scripting**
- - As it is expected that most if not all scripts needed to be ported, some dummy/compatibility functions has been removed:
- - - `Resources.getNTEProtoVersion()`
- - - `Resources.getNTEVersionInt()`
- - - `Resources.getNTEVersion()`
- - - `Resources.getMTRVersion()`
- - - `Resources.readNbtString()`
- - - - Replacement: `Resources.getAddonVersion("mtr")`
- - To prevent misuse of scripts, script may now only run java method from the following packages:
- - - `com.lx862.jcm.mod.scripting.*`
- - - `org.mtr.*`
- - - `java.awt.*`
- - - `java.lang.*`
- - - `sun.java2d.*`

**Fixes:**
- **JSON PIDS Preset**
- - Fix text potentially overlapping when platform number is hidden

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)
