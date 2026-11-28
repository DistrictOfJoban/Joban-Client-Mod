# JavaScript → C++ script migration guide

The native (C++) pipeline shares the wrapper semantics of the
JavaScript pipeline almost one-to-one; a port is mostly renaming and
re-shaping. This table lists the common API pairs, followed by the
workflow and the semi-automatic converter (`native/tools/js2cpp.py`).

## Lifecycle

| JavaScript | C++ |
|---|---|
| `function create(ctx, state, pids)` | `void create(PidsContext& ctx, State& state, const Pids& pids) override` |
| `function render(ctx, state, pids)` | `void render(PidsContext& ctx, State& state, const Pids& pids) override` |
| `function dispose(ctx, state, pids)` | `void dispose(...) override` |
| closure variable surviving frames | member of the `State` struct (host-allocated, placement-new'd) |
| `SCRIPT_INPUT` JSON | `api.input()` (`mtr::Api`, ABI 8+) |

Register with `MTR_REGISTER_PIDS_SCRIPT(MyScript)` (or
`_VEHICLE_ / _LIFT_ / _EYECANDY_ / _PSD_ / _LEVEL_CROSSING_ /
_FARE_MACHINE_ / _ANNOUNCEMENT_`) — one script id per module, declared
in `mtr_custom_resources.json` exactly like the JS entries.

## PIDS drawing

| JavaScript (TextWrapper / TextureWrapper) | C++ (`mtr::Text` / `mtr::Texture` / `mtr::Rect`) |
|---|---|
| `new Text("X").pos(0,0).size(w,h).centerAlign().stretchXY().draw()` | `mtr::Text::create().text("X").pos(0,0).size(w,h).center_align().stretch_xy().draw(ctx)` |
| `.font("demo:fonts/x.ttf")` | `.font("demo:fonts/x.ttf")` |
| `.color(0xFF00FF00)` | `.color(0xFF00FF00)` |
| `.naturalLight()` | `.natural_light()` |
| `.renderType("LIGHT_2")` | `.render_type("LIGHT_2")` |
| `new Texture("ns:path.png").uv(...)` | `mtr::Texture::create()...` (same fields) |
| `new Rectangle().color(...)` | `mtr::Rect()...` |

## PIDS data

| JavaScript | C++ |
|---|---|
| `pids.width` / `pids.height` | `pids.width()` / `pids.height()` |
| `pids.arrivals().forEach(a => ...)` | `for (int i = 0; i < pids.arrivals().size(); i++) { auto& a = pids.arrivals().at(i); ... }` |
| `arrival.arrivalTime()` | `arrival.arrival_time()` |
| `arrival.destination` | `arrival.destination()` (a `StrView`) |
| `arrival.routeColor()` | `arrival.route_color()` |
| `arrival.realtime()` | `arrival.realtime()` |
| `arrival.isExpress()` (FangSu, ABI 16) | `arrival.isExpress()` — same |
| `pids.getCustomMessage(i)` | `pids.get_custom_message(i)` |
| `pids.getWeather()` | `pids.getWeather()` — same |
| `Timing.now()` | the snapshot's `game_time_millis()` |

## Vehicle

| JavaScript | C++ |
|---|---|
| `vehicle.speed` (blocks/tick) | `vehicle.speed_ms() / 20` |
| `vehicle.speedKmh()` | `vehicle.speed_kmh()` |
| `vehicle.doorValue` | `vehicle.door_value()` |
| `vehicle.getDirectionOfDoorOpening()` | `vehicle.getDirectionOfDoorOpening()` — same |
| `vehicle.stops` | `vehicle.stops()` (a `StopList`) |
| `ctx.drawCarModel(m, car, matrices)` | `ctx.draw_car_model(m, car, matrices)` |
| `ctx.getCarBogieRenderManager(...).drawModel(...)` | `ctx.draw_bogie_model(m, car, bogie, matrices)` |
| model tint/emissive (ABI 16) | `ctx.draw_car_model(m, car, mats, mtr::VehicleContext::ModelStyle().tint(0x80FFFFFF).emissive().translucent())` |
| signal state (ABI 16) | `vehicle.signalState()` / `vehicle.getSignalState()` |
| `ctx.playCarSound(...)` | `ctx.play_car_sound(...)` |
| `ctx.playAnnSound(...)` | `ctx.play_ann_sound(...)` |

## GraphicsTexture / canvas

| JavaScript | C++ |
|---|---|
| `new GraphicsTexture(w, h)` | `mtr::LcdCanvas` (gfx.hpp) — paints BGRA8 pixels |
| `g.setColor(...)` / `g.fillRect(...)` | `gfx2d.hpp` rasterizer (`fill_rect`, `draw_line`, ...) |
| `GraphicsTexture.writePixel` | `canvas.pixel(x, y, argb)` |
| `texture.upload()` | the `JCM_DRAW_TEXTURE_UPLOAD` record (automatic per frame) |

## Sound

| JavaScript | C++ |
|---|---|
| `ctx.playSound("minecraft:...", x, y, z, vol, pitch)` | `ctx.play_sound("minecraft:...", x, y, z, vol, pitch)` |
| riding-only announcements | `JCM_DRAW_LOCAL_SOUND` records (`ctx.play_ann_sound` / `ctx.play_local_sound`) |

## Things with no twin (port by hand)

- `MinecraftClient.*` (player state, screen queries) — use the
  `mtr::Api` JSON bridge (`api.hpp`) into the Java wrappers.
- `JSON.parse` / dynamic objects — parse with the bundled
  `mtr::Json` (nlohmann) instead.
- `setTimeout` / `setInterval` style scheduling — implement with
  `game_time_millis()` deltas in the State struct.
- `require`-style imports — static linking; the SDK headers are all
  available.

## Semi-automatic conversion

```
python3 native/tools/js2cpp.py input.js -o output.cpp --id mypack:pids --kind pids
```

The tool performs the table's renames, converts `forEach` loops and
Text chains, moves obvious module-scope constants into TODO-commented
State fields, and leaves the genuinely dynamic parts as review markers.
Expect a compileable skeleton plus a handful of TODOs.

## Why migrate?

- One marshal per frame instead of hundreds of reflective getter calls
  (the whole snapshot is a flat POD the script reads as memory).
- No GC pressure from wrapper objects; draw records are bump-allocated.
- Crash guard: a faulting module disables itself, never the game.
- ABI 16 extras arrive native-first (styled model draws, the signal
  extension, the block-script kinds).
