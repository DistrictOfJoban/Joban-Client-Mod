# Joban Client Mod - Rewrite (v2)
This is an experimental rewrite of the Joban Client Mod from scratch.  
This is currently not production ready yet and contains many changes (Including block ids) from Joban Client Mod 1.x.x series which may not be migrated properly to v2.  

Prebuilt jar of the mod can be found in the [action tab](https://github.com/DistrictOfJoban/JCM-Rewrite/actions).  
Use with caution!

## Roadmap
### Implemented
- Block and Item Registration
- Block Entity Rendering
- GUI Screen
- Integrate '''Minecraft Mapping''' project to provide ~~cross version~~ and ~~cross-platform~~ compatibility
- Blocks:
  - Buffered Stop (East Rail Line)
  - Circle Walls
  - Departure Pole
  - Departure Timer
  - Slanted Ceiling
  - Exit Sign (Odd)
  - Exit Sign (Even)
  - E44 Train Model
  - MTR Trespass Signage
  - KCR Trespass Signage
  - LRT Trespass Signage
  - MTR Enquiry Machine
  - MTR Wall Mounted Enquiry Machine
  - KCR Wall Mounted Enquiry Machine
  - MTR Railway Vision Enquiry Machine
  - MTR Station Ceiling (2009)
  - MTR Station Ceiling Pole (2009)
  - Light Lantern
  - MTR Stairs
  - Wall attached Helpline (Sticker & No Sticker)
  - Standing Helpline (Normal & EAL)
  - LRT Inter Car Barriers
  - Operator Button
  - Emergency Button (TCL)
  - Emergency Button (TML)
  - Emergency Button (SIL)
  - KCR Emergency Stop Sign
  - Station Ceiling
  - Spot Lamp
  - Water Machine

## To be done
- Add MTR Mod as dependencies (Need a way to depend on 4.0?)
- Finish all incompleted blocks and functionalities
- GUI Config Screen
- Client-Side Config (Disable custom fonts, debug mode, etc).
- Build for multiple minecraft version
- Start building for different modloaders
- Migration path for JCM Legacy 1.x.x
  - Roadblock:  
  No official path to migrate blockstates and id, will have to find our own, potentially dangerous path... or have to leave all the player unsatisfied

## Contributing
This project is still in an early stage, feel free to PR stuff mentioned in the **To be done** textCategory.  
Note that I also have my own scheduling and thoughts on handling the new rewrite, as such I may modify your PR (Usually commiting on it or afterwards) before merging.

## License
This project is licensed under the MIT License.
