## JCM v2.0.0 for MTR 4.0 has been released!

**Changes:**
- As it is expected that most if not all scripts needs to be ported, some dummy/compatibility functions has been removed:
- - `Resources.getNTEProtoVersion()`
- - `Resources.getNTEVersionInt()`
- - `Resources.getNTEVersion()`
- - `Resources.getMTRVersion()`
- - - Replacement: `getAddonVersion("mtr")`
- - `Resources.readNbtString()`

**Fixes:**
- **JSON PIDS Preset**
- - Fix text potentially overlapping when platform number is hidden

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)
