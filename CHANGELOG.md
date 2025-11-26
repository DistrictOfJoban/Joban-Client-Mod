# JCM v2.1.0-beta.2 for MTR 4.0.2 has been released!

## New (Scripting Implementation)
### Eyecandy Scripting
- Add `EyecandyBlockEntity.redstoneSignal()` to process redstone signal in the block.
- - Note: This only returns 0 and 15 with the current implementation.

## Important Note
### For Minecraft 1.16 players
Minecraft 1.16 has already been released for well over 4 years. While there are many efforts in the codebase to make JCM available for 1.16, it also puts a burden to maintain them.
- Minecraft 1.16 will no longer receive first-class support like other versions.
- Feature parity will no longer be maintained. New script features/capabilities (Especially scripting engine related) may not be available on 1.16.
- Players who are still using Minecraft 1.16 should consider upgrading to any newer version.
- When demands have become low enough, we may consider dropping all 1.16 builds and CI all-together.

### For other players
This is a beta release primarily for script developers to play around and give feedback on.  
Normal players are *strongly* advised to wait until the official release of v2.1.0.

### For Script Developers
Breaking changes may occur for all *new features* introduced during the v2.1.0 beta. JSON fields and scripting APIs may be moved, renamed, redesigned or scrapped at any point during beta releases.

You are more than encouraged to provide feedback for the newly introduced features (Yes, down to the naming!). Otherwise, no new breaking changes will be introduced after the official release, and any imperfections/annoyance that you perceive will stay with you forever :D

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm), [CurseForge](https://curseforge.com/minecraft/mc-mods/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)
