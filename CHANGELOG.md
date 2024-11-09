## (JCM v2.0.0 beta.5) for (MTR 4.0.0-beta.12) has been released!

**New:**
- **Added Scripted PIDS Preset**
- - Create custom PIDS with JavaScript!
- - Experimental, feedback needed!
- - Documentation and Example Pack available at https://www.joban.org/wiki/JCM:Scripted_PIDS_Preset
- JSON PIDS Preset
- - You can now set `textOverflowMode` as a property (string), possible value are `STRETCH`, `SCALE`
- - PIDS Variable are reintroduced, just like in JCM v1.
- - - `{worldPlayer}` are not implemented, and will return `?` for now.
- PIDS Preset (JSON/Scripted)
- - You can now set `name` as a property (string) to show a user-friendly name shown in the UI
- - You can now set `blacklist` as a property (String JSON Array) to prevent your preset from being used in certain PIDS:
- - - Possible values for inclusion are `rv_pids`, `rv_pids_sil_1`, `rv_pids_sil_2`, `lcd_pids`, `pids_1a`, `pids_projector`

**Changes**
- The PIDS 1A block now also supports PIDS Preset
- - A new PIDS Preset "PIDS 1A" is introduced to replicate the PIDS 1A look.

**Fixes:**
- Fix LRT Inter-Car barrier breaking when flooded with water
- Fix West Rail Line Double Ceiling Block not being able to be placed

**Download:**  
You can download this release on [Modrinth](https://modrinth.com/mod/jcm) or [GitHub](https://github.com/DistrictOfJoban/Joban-Client-Mod/releases)
