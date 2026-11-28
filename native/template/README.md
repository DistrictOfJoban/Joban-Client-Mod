# JCM native script scaffold

One-command project skeleton for JCM (Joban Client Mod) C++ scripts,
built against the `jcm-native-sdk` zip that ships with every release.

## Use it

```bash
cp -r <sdk>/template my_script
cd my_script
cmake -B build -DJCM_SDK=<path-to-extracted-sdk> -DCMAKE_BUILD_TYPE=Release
cmake --build build
```

Copy the built module into your resource pack using the loader's
platform convention:

```
mypack/
  assets/mypack/natives/linux-x64/libscaffold_pids.so
  assets/mypack/natives/windows-x64/scaffold_pids.dll
  assets/mypack/natives/macos-arm64/libscaffold_pids.dylib
  mtr_custom_resources.json
```

and declare the script ids in `mtr:mtr_custom_resources.json`:

```json
{
  "pidsScripts": [{
    "id": "scaffold:pids",
    "scriptLocations": ["scaffold:natives/pids"]
  }],
  "levelCrossingScripts": [{
    "id": "scaffold:crossing",
    "scriptLocations": ["scaffold:natives/crossing"]
  }]
}
```

## What's inside

- `scaffold_pids.cpp` — a PIDS script (text draws, arrival reads).
- `scaffold_vehicle.cpp` — a vehicle script with the ABI 16 styled
  model draw (tint + emissive) and the signal-state extension.
- `scaffold_crossing.cpp` — an ABI 16 level crossing script: barrier
  animation from the host's target, warning lamp, approach bell.

Keep the one-module-per-script rule: every `MTR_REGISTER_*_SCRIPT`
expands the module exports, so one script id = one source file = one
built module.

For the full API surface see the SDK's `include/mtr/` headers and the
`README.md` at the SDK root.
