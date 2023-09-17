# Joban Client Mod - Rewrite
This is an experimental rewrite of the Joban Client Mod from scratch.  
This is not production ready yet and contains many changes (Including block ids) from Joban Client Mod 1.x.x series which may not be migrated properly to v2.  
Use with caution!

## Roadmap
### Implemented
- Creative Inventory Tabs Registration
- Block and Item Registration
- Integrate '''Minecraft Mapping''' project to provide cross version and cross-platform compatibility
- Blocks:
  - Buffered Stop (East Rail Line)
  - Departure Pole
  - Slanted Ceiling
  - Exit Sign (Odd)
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
- Finish all uncompleted blocks
- Block Entity Rendering
- GUI Screen
- Networking (Packets)
- Migration path for JCM Legacy 1.x.x
  - Roadblock:  
  No official path to migrate blockstates and id, will have to find our own, potentially dangerous path... or have to leave all the player unsatisfied

## Contributing
This project is still in an early stage, feel free to PR stuff mentioned in the **To be done** category.  
Note that I also have my own scheduling and thoughts on handling the new rewrite, as such I may modify your PR (Usually commiting on it or afterwards) before merging.

## Setup

For setup instructions please see the [fabric wiki page](https://fabricmc.net/wiki/tutorial:setup) that relates to the IDE that you are using.

## License

This project is licensed under the MIT License.
