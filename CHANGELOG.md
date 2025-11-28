# JCM v2.1.0-beta.2 for MTR 4.0.2 has been released!

## New (Scripting Implementation)
### Execution
Scripts may now execute across 4 different threads (Instead of only 1 background thread), which should improve the amount of scripts that can be executed simultaneously. (Thanks zbx1425 for the idea)

### Eyecandy Scripting
- Add `EyecandyBlockEntity.redstoneSignal()` to process redstone signal in the block.
- - Note: This only returns 0 and 15 (lit & unlit) with the current implementation. Scripts should however account for future changes, where the true signal level is returned.
- Experimental: Add `EyecandyBlockEntity.setCollisionShape()` to set the client-side collision shape of the block.

## Important Note
### For players
This is a beta release primarily for script developers to play around and give feedback on.  
Normal players are *strongly* advised to wait until the official release of v2.1.0.

### For Script Developers
Breaking changes may occur for all *new features* introduced during the v2.1.0 beta. JSON fields and scripting APIs may be moved, renamed, redesigned or scrapped at any point during beta releases.

You are more than encouraged to provide feedback for the newly introduced features (Yes, down to the naming!). Otherwise, no new breaking changes will be introduced after the official release, and any imperfections/annoyance that you perceive will stay with you forever :D

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm), [CurseForge](https://curseforge.com/minecraft/mc-mods/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)
