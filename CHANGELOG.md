# JCM v2.1.0 for MTR 4.0.2 has been released!

## New
- Add **Help Line (Tseung Kwan O Line Type)**
- The Script Debug Overlay now allows switching between sources (MTR/JCM) to provide a more clutter-free view.
- - Default keybind is **\[** and **\]**, can be changed in Minecraft keybind settings.
- Add **Eyecandy Scripting**, see [documentations](https://jcm.joban.org/v2.1/dev/scripting/type/eyecandy) for details.

## Changes
- **Scripting Engine (Rhino)**
- - **For Minecraft 1.17+**, the Rhino engine has been updated to **1.8.0** which added more support for modern JS syntax. See [this compatibility table](https://mozilla.github.io/rhino/compat/engines.html) for details.
- **Script Execution:**
- - Script may now execute among 4 different threads.
- - Note: The same type of script entry will always be executed on the same thread
- Slightly refactor the transaction history logic, should hopefully be a *little* more robust.

## Misc
- Full Changelogs for Script Developers:
- - [v2.1.0-beta.1](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases/tag/v2.1.0-beta.1)
- - [v2.1.0-beta.2](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases/tag/v2.1.0-beta.2)

### For Minecraft 1.16 players
Minecraft 1.16 has already been released for well over 4 years. While there are many efforts in the codebase to make JCM available for 1.16, it also puts a burden to maintain them.
- Feature parity for 1.16 will no longer be maintained. New script features/capabilities (Especially scripting engine related) may not be available on 1.16.
- Players who are still playing on 1.16 should consider upgrading to any newer version.
- When demands have become low enough, we may consider dropping 1.16 builds all-together.

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm), [CurseForge](https://curseforge.com/minecraft/mc-mods/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)