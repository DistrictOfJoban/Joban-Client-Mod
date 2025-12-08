# JCM v2.1.0-beta.3 for MTR 4.0.2 has been released!

**Note: Normal players are *strongly* advised to wait until the official release of v2.1.0.**

## Change (Scripting Implementation)

### Execution
- Scripts are now multithreaded by default for scripts registered in MTR 4 & JCM PIDS.
- - MTR 3 Eyecandy Entries now retains the old behaviour of executing in a single thread.
- - Can be manually controlled with the following boolean field in the registration entry:
  - MTR 4: `scripting.multithreaded`
  - JCM PIDS: `scriptMultithreaded`

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm), [CurseForge](https://curseforge.com/minecraft/mc-mods/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)