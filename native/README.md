# MTR Native Scripting (C++)

为 **Minecraft Transit Railway 4.0** 的 `vehicle` / `eye_candy` / `pids`
三类自定义资源提供 **C++ 原生脚本**能力——与 Joban Client Mod v2.3 的
JavaScript (Rhino) 脚本 1:1 语义对齐（同样的 `create/render/dispose`
生命周期、同样的捕获-回放渲染管线、同样的 API 表面），但以原生
共享库执行，消除解释执行与跨语言反射开销。

## 目录结构

```
native/
├── include/mtr/            # 脚本 SDK（header-only）
│   ├── mtr_native.h        #   C ABI：导出、POD 快照、draw call 记录（v7）
│   ├── script.hpp          #   注册宏 + 生命周期适配（ScriptBox）
│   ├── frame.hpp           #   bump-arena 帧录制器（像素 arena 按需增长）
│   ├── vehicle.hpp         #   Train/Car/Stop 包装（对应 VehicleWrapper）
│   ├── pids.hpp            #   Pids/Arrival 包装 + Text/Texture 构建器
│   ├── eyecandy.hpp        #   EyeCandy 包装（对应 EyeCandyScriptContext）
│   ├── gfx.hpp             #   GraphicsTexture（脏矩形纹理）
│   ├── gfx2d.hpp           #   java.awt.Graphics2D 等价软件光栅化器 ★
│   ├── font5x7.hpp         #   5x7 公版点阵字体（ASCII 文本）
│   ├── matrices.hpp        #   矩阵栈（对应 JS Matrices）
│   ├── text.hpp            #   Text/Texture 流式 API（对应 jsblock Text/Texture）
│   └── util.hpp            #   CycleTracker / PidsUtil（对应 pids_util.js）
├── examples/               # 示例脚本（各编译为一个 .so/.dll）
│   ├── vehicle_lcd.cpp     #   车侧 LCD + 车次号（对应 display_helper.js 用法）
│   ├── pids_arrivals.cpp   #   到站信息屏（pids_1a.js 的逐行移植）
│   ├── eyecandy_signal.cpp #   红石信号灯 + 数码时钟
│   ├── jslcd_common.hpp    #   社区 JS LCD 包共享移植层 ★
│   ├── jslcd_vehicle.cpp   #   完整车侧路线图 LCD（13 个 JS 文件的全量移植）★
│   ├── jslcd_train_num.cpp #   车号系统（侧线名解析/侧牌/头尾牌）
│   ├── wr2a03_common.hpp   #   若益宛 WR2-A03 共享层（同一套 JS 源的另一次移植）★
│   ├── wr2a03_lcd.cpp      #   WR2-A03 车侧 LCD（mtrScriptId "wr2a03:lcd"）★
│   └── wr2a03_train_num.cpp#   WR2-A03 侧牌/头尾牌（"wr2a03:train_num"）★
├── java/                   # Java 侧 JNI 桥接参考实现
│   └── com/lx862/jcm/nativeapi/NativeScriptManager.java
├── jni/                    # JNI 桥实现（jcm_native_bridge.{dll,so,dylib}）
│   └── jni_bridge.cpp      #   nOpen/nRender/... + 宿主资源回调（v4/v5）
├── bench/                  # 真实微基准（C++ vs bun-JSC vs Rhino）
│   ├── lcd_bench.cpp       #   原生驱动：mtrCreate/mtrRender x N
│   ├── lcd_bench.js        #   JS 孪生（同算法同字体, bun 运行）
│   ├── lcd_bench_rhino.js  #   Rhino 兼容孪生（同算法, ES5 风格）
│   ├── RhinoBench.java     #   以 JCM 同款引擎/调用模型驱动上者
│   ├── jslcd_smoke.cpp     #   jslcd 端到端冒烟（POSIX）
│   ├── wr2a03_smoke.cpp    #   WR2-A03 端到端冒烟（跨平台，41 项断言）★
│   └── gfx2d_bench.cpp     #   Gfx2D 基元微基准 ★
└── CMakeLists.txt
```

## 快速上手（vehicle 示例）

```cpp
#include <mtr/script.hpp>
#include <mtr/gfx.hpp>

struct LcdState {
    mtr::CycleTracker destination{"TSUEN WAN|KWUN TONG", 120};
};

struct LcdScript : mtr::VehicleScript<LcdState> {
    static constexpr auto ID = "demo:kcx_lcd";

    void create(mtr::VehicleContext& ctx, LcdState& s, const mtr::Train& t) override {
        lcd.create(ctx.input(), 128, 32);        // JS: new GraphicsTexture(128, 32)
    }
    void render(mtr::VehicleContext& ctx, LcdState& s, const mtr::Train& t) override {
        paint(s, t);                              // 纯 C++ 点阵绘制热循环
        lcd.upload(ctx.frame());                  // JS: texture.upload()
        mtr::Matrices m{ctx.frame()};
        ctx.draw_car_model(model, 0, &m);         // JS: ctx.drawCarModel(...)
    }
    mtr::GraphicsTexture lcd{};

    /* ... paint 省略，见 examples/vehicle_lcd.cpp ... */
};

MTR_REGISTER_VEHICLE_SCRIPT(LcdScript)
```

资源包声明（与 JS 脚本共存）：

```json
// mtr_custom_resources.json
{
  "vehicles": [ { "id": "demo:kcx", "scriptId": "demo:kcx_lcd", "hideDisplayParts": true } ],
  "vehicleScripts": [
    { "id": "demo:kcx_lcd", "language": "cpp",

      // 形式 A：平台无关——四平台目录约定自动解析（见下）
      // "nativeLibrary": "mtr:natives/vehicle_lcd"

      // 形式 B：按平台分别指定（Windows/Linux/macOS 各一条，可省略）
      "nativeLibrary": {
        "windows": "mtr:natives/windows-x64/vehicle_lcd.dll",
        "linux":   "mtr:natives/linux-x64/libvehicle_lcd.so",
        "macos":   "mtr:natives/macos-arm64/libvehicle_lcd.dylib"
      }
    }
  ]
}
```

构建：

```bash
# 方式一：CMake（推荐，自动带上 -fvisibility=hidden）
cmake -B build && cmake --build build -j
# 产物: build/vehicle_lcd.so 等三个脚本库 + lcd_bench

# 方式二：直接 g++（注意 -fvisibility=hidden 不可省略——
# 否则多个模块的 inline 静态 recorder 会被动态链接器符号合并）
g++ -O2 -std=c++17 -fPIC -shared -fvisibility=hidden \
    -Iinclude examples/vehicle_lcd.cpp -o libvehicle_lcd.so
```

## 跨平台预编译（GitHub Actions CI）

`ci/native-build.yml`（安装到仓库根 `.github/workflows/native-build.yml`）在
4 个平台上矩阵构建全部 5 个脚本库，Linux 侧同时跑 41 项冒烟断言与
`lcd_bench` 微基准：

| 平台 | runner | 产物 |
| --- | --- | --- |
| linux-x64 | ubuntu-latest | `lib*.so` + 冒烟 + 基准 |
| windows-x64 | windows-latest (MSVC) | `*.dll` |
| macos-x64 | macos-latest（交叉编译 x86_64） | `lib*.dylib` |
| macos-arm64 | macos-latest | `lib*.dylib` |

- 每次 push（涉及 `native/**`）产出 per-platform artifact
  `mtr-native-scripts-<platform>.zip`（内含 `assets/mtr/natives/` 资源包布局），
  以及 `resourcepack` job 组装的 **开箱即用资源包**
  `yanyang-native-lcd-pack.zip`（4 平台 natives + mtr_custom_resources.json
  接线 + pack.mcmeta，放进 `resourcepacks/` 即得两台原生脚本驱动的
  SP1900；详见 `resourcepack/yanyang-jslcd-pack/README.md`）。
- 打 tag 发布正式版：`git tag v3 && git push origin v3` →
  release job 把 4 个平台 zip + 资源包 zip 挂到 GitHub Release，
  即拿到 `.dll`/`.dylib` 的官方下载渠道（无需本地工具链）。
- Windows/macOS 侧无 `dlopen`，CMakeLists 已将 `jslcd_smoke` 守卫为
  `if(UNIX)`——非 POSIX 平台只构建脚本库本体。

### 已修复：macOS dylib 17MB 膨胀

`HostBuffers`（含 16MB `pixel_arena`）曾以函数内 `static` 聚合形式声明。
Apple ld64 会把 comdat 中的零初始化聚合发成**文件后备的 `__DATA`**
（非 zerofill），导致每个 macOS dylib 高达 ~17MB（Linux/gcc 走 `.bss`
不受影响，MSVC 亦然）。修复：`install_frame()` 改为
`static HostBuffers* buffers = new HostBuffers();` —— 三平台均为
demand-zero 页，dylib 回到 ~140KB，语义不变（一次性 attach、模块生命周期
复用）。附带修复 `NativeScriptManager.readResourceBytes`：读二进制
`.so/.dll/.dylib` 不能走文本 `readResource`（UTF-8 往返会损坏字节），
改用 `readAllResources` 的原始流；并新增
`resolveNativeLibraryPath()`：`nativeLibrary` 声明支持平台无关形式
（如 `yanyang:natives/jslcd_vehicle`），按 os.name/os.arch 展开为
`linux-x64/libjslcd_vehicle.so` / `windows-x64/jslcd_vehicle.dll` /
`macos-{x64,arm64}/libjslcd_vehicle.dylib`，带扩展名的完整路径保持兼容。

### 已新增：按平台分别指定 + 未指定平台自动跳过

`language: "cpp"` 的 `nativeLibrary` 现支持**对象形式**，为
Windows / Linux / macOS 分别声明各自的 dll / so / dylib：

```json
"nativeLibrary": {
  "windows":     "demo:natives/windows-x64/kcx_lcd.dll",
  "linux":       "demo:natives/linux-x64/libkcx_lcd.so",
  "macos-arm64": "demo:natives/macos-arm64/libkcx_lcd.dylib"
}
```

- 键：`windows` / `linux` / `macos`，另支持架构级键
  （`windows-x64` / `linux-x64` / `macos-x64` / `macos-arm64`），
  架构级键优先于通用键；`win` / `osx` / `darwin` 为别名。
- 值：带扩展名的完整路径（原样使用），或不带扩展名的平台无关
  茎（仍按目录约定展开）。
- 字符串形式完全向后兼容：平台无关茎四平台全跑；带扩展名的
  字符串自动绑定到对应系统（`.dll`→Windows、`.so`→Linux、
  `.dylib`→macOS）。
- **未声明当前系统时不执行该脚本**：一行提示写入 `latest.log`
  （`[JCM] Native script xxx skipped on <platform>: ...`），
  同时在 **JCM 调试模式**（scriptDebugMode）开启时输出到游戏内
  聊天（黄色 `[C++]` 行）与调试 HUD 的
  「C++ Native Scripts」区块（已加载=蓝色，跳过=红色+原因）。
  vehicle / eye_candy / lift / pids 四类条目均走同一逻辑。


验证（bench/ 下已提供）：

```bash
g++ -O2 -std=c++17 -Wall -Iinclude bench/lcd_bench.cpp \
    examples/vehicle_lcd.cpp -o bench/lcd_bench && ./bench/lcd_bench
g++ -O2 -std=c++17 -fPIC -shared -fvisibility=hidden \
    -Iinclude examples/pids_arrivals.cpp -o bench/libpids_arrivals.so
g++ -O2 -std=c++17 -fPIC -shared -fvisibility=hidden \
    -Iinclude examples/eyecandy_signal.cpp -o bench/libeyecandy_signal.so
g++ -O2 -std=c++17 -Iinclude bench/smoke_test.cpp -ldl \
    -o bench/smoke_test && ./bench/smoke_test   # → PIDS/EYECANDY SMOKE OK
```

## 基准（本仓库 bench/，g++ -O2 vs Rhino 1.7.14）

同一 LCD 渲染算法（128×32 点阵 + 17 条 draw call + 8 车 × 2 侧模型绘制）：

| 引擎 | 每帧 | 相对原生 |
|---|---|---|
| C++ (g++ -O2) | 5.45 µs | 1× |
| bun JavaScriptCore（纯算法对照组） | 4.03 µs | ≈1×（现代 JIT 确实快） |
| Rhino opt9（JCM 最乐观配置） | 230 µs | 42× |
| **Rhino opt0（JCM v2.3 实际默认）** | **271 µs** | **50×** |

注意：三种实现都**未计入** JCM 真实管线中 JS 侧的
NativeJavaObject 反射取值、装箱与每条 draw call 的 Java 对象分配
（原生路径本来就没有这些开销），因此游戏内的差距只会更大。
以 50 辆车同屏、20fps 脚本频率计：Rhino ≈ 13.5 ms/帧 纯脚本开销，
原生 ≈ 0.27 ms/帧。

## ★ 社区 JS LCD 包的完整移植（jslcd_vehicle / jslcd_train_num）

`examples/jslcd_vehicle.cpp` + `examples/jslcd_train_num.cpp` 是一套
社区 JS 资源包（13 个文件、约 3000 行：main.js / config.js / data.js /
circular.js / util.js / mtr_util.js / train_num_util.js / draw_header.js /
draw_common.js / draw_circular.js / draw_linear.js / train_num.js /
draw_num.js）的**全量 C++ 移植**——车侧路线图 LCD + 车号系统：

- **顶部信息栏**：线路色带 + 线路名块（数字号线大号数字版式 / 命名线
  双语居中）+ logo 徽章 + 开往终点站 / 环线方向（内环/外环）+
  下一站/到达 + 车号「液态玻璃」卡片（6 层阴影 + 双层高光 + 描边）；
- **环线**：完整环形线路图（圆角矩形轨道 + 站点圆点 + 自适应字号 +
  换乘徽章 + 进度闪烁叠加 + 弧段箭头）/ 部分图（5 站胶囊，环绕取模）；
- **直线**：完整线路图（灰轨 + 已过/当前段着色 + 奇偶交替站名 +
  字形度量垂直定位）/ 部分图（边界圆 + 已过站置灰）；
- **开门页**：大站名（线路色）+ 终点站提示 + 换乘/Transfer 面板；
- **状态机**：full/partial 10s 轮播、1s 闪烁、门检测阈值、到站去抖、
  行驶方向左右屏（isReversed 翻转）——语义与 main.js 一致；
- **车号系统**：侧线名解析（`"10010/01-02-…"` / `"10010 Tc1 A1-1"`）、
  每车厢双侧牌 + 头尾牌、透明清空（AlphaComposite.CLEAR 等价）、
  车厢数不匹配时的红色双语错误提示。

### 性能策略（比 JS 更快的三层设计）

1. **repaint-on-change**：JS 每帧重绘全部 16 块屏；原生版用内容签名
   （页面模式/闪烁/门状态/站序/线路/侧线名的 FNV 哈希）判定，
   内容不变则 0 光栅化、0 上传——稳态实测 **18.7 µs/帧**；
2. **分帧重绘预算**：内容变化时每帧最多重绘 2 节车厢（脏矩形让
   延迟上传零成本），全列车重绘分摊到 4 帧（每帧 ≈10 ms）而不是
   一帧 40 ms——帧预算友好，JS 则每帧都全量重绘；
3. **Gfx2D 快速路径**：不透明色行走行级 `std::fill`（整屏白底
   186 µs vs 修复前 4213 µs，23×），像素基址缓存消除逐像素引用链。

### 已修复的上游 JS bug（移植时发现）

`computeLcdAspectFromSlot` 对 LCD_POS_L/R 四边形（顶点序 TL→BL→BR→TR）
返回的是**长宽比的倒数**（0.286 而非注释声称的 ≈3.5），会把 SCR_H
吹到 11564px。移植版取长边/短边（3.5），与 config.js 全部布局常量
（TEX 2800×800、LAYOUT_K 1.667、SCR 3304×944）一致。

冒烟验证（`bench/jslcd_smoke.cpp`，41 项断言全过（36 个独立断言 + 循环帧））：合成 8 车 / 10 站 /
换乘/出口/环线快照 → dlopen 真实 ABI 驱动 → 帧记录重建纹理 → PPM 落盘 →
像素级断言（顶栏线路色、红绿站点圆点、玻璃卡字形、环线色环、开门大
站名、车牌墨迹、**出口面板青色字母与黑色目的地 CJK**）+ 性能断言
（稳态 <100µs、重绘预算帧 <20ms）。

## ABI v7：资源包字体

`JcmHostServices` 末尾新增 `measure_font_text` 和 `draw_font_text`。
`Gfx2D` 可指定字体资源 ID，由宿主以 `FontMetrics` 测量、`drawString` 绘制整段 UTF-8
文本，保留字形比例、字宽、基线、下行部、压缩、透明度和裁剪。字模按字体、字号、
缩放和子像素位置缓存，上限 32 MiB；脚本重载时重新读取字体。

WR2-A03 使用资源包自带的思源黑体 Medium、Arial / Arial Bold、Sans Lao、鸿雷行书；
错误提示使用同包已有的思源黑体 Regular。资源缺失时宿主记录一次警告并回退到
内置 Noto。无宿主测试仍可使用 SDK 的点阵回退。

新脚本需搭配 yanyang.6 或更高版本的宿主和四平台桥。ABI 6 的快照与宿主表前缀不变，
新宿主仍接受 ABI 6 模块，并向其传递版本 6 的生命周期输入。

本地可用实际资源字体验证：

```sh
python native/bench/jvm_test.py --build native/build --resource-pack /path/to/WR2-A03-pack
```

验证包含 Java2D 字形、字宽、基线、压缩和裁剪对照，以及通过真实 JNI 生成的
侧面、头尾车号纹理逐像素对照。指定资源包时还会生成三种 LCD 页面预览。
云端未提供 WR2-A03 字体文件时，使用 JCM 自带 Noto 字体验证接口和 JNI 通路。

## ABI v6 变更

新增 `mtrInit(const JcmFrameInput*)`：宿主在分配完 per-instance state 块之后、
**第一次 `mtrCreate` 之前**调用它，模块在块内 placement-new 出自己的 State 对象。

为什么必须这样：旧契约是"宿主给一块 `mtrStateSize()` 大小的**全零**内存"，
但那**不是**一个合法的 C++ 对象。带 `std::string` / `std::vector` 成员的 State
在 libstdc++ / libc++ 上会立刻崩溃（SSO 把缓冲区指针内联在对象里，全零 = 空
指针，第一次 `clear()` / 赋值就写空指针）；MSVC 的布局恰好把全零当成合法空串，
所以这个问题在 Windows 上一直看不出来 —— 于是"只在 Windows 编过"就等于没测。

- 该导出是**可选**的：宿主用 `dlsym`/`GetProcAddress` 拿不到就跳过，ABI ≤5 的旧
  模块照旧工作（它们本来就只在 MSVC 上调过）。
- 模块侧不依赖宿主也正确：`lifecycle()` 在每次进入时通过 `init_state()` 兜底构造，
  因此漏调 `mtrInit` 或首帧乱序都不会读到未构造对象。
- `init_state()` 以**块指针**判断是否需要构造：同一块重复调用是 no-op（否则每帧
  重建会把脚本累积的状态清掉），换了块就重新构造，`mtrDispose` 之后清标志，
  所以"init 构造 / dispose 析构"始终配平。

## ABI v5 变更

`JcmStop` 新增 `route_circular_state`（每个停站所属线路的 CircularState，
0 NONE / 1 CLOCKWISE / 2 ANTICLOCKWISE），等价于 JS 的
`stop.route.getCircularState()`。

LCD 移植的环线判定要遍历整条停站表（circular.js 的方式 (ii)）；只靠
"当前 route 的 circular_state + 停站表里同线路出现两次即绕回"这种形态
判据，会把**任何多站线路**都误判成环线。有了 per-stop 状态就能像
`circular.js` 一样逐步遍历，判定结果与 JS 一致。

## ABI v4 变更

`JcmHostServices` 新增 `acquire_quad_model(vertices_xyz, uv, vertex_count,
render_stage, texture_handle)` —— 宿主构建的贴图四边形，对应 JS 的
`new DisplayHelper(slotCfg)`：

* 资源包里的 LCD/车号四边形原本由 `DisplayHelper` 在脚本里生成网格，
  再把网格交给 `ModelManagerJS` 上传；原生脚本直接把同一组顶点（JS 的
  `pos` 数组）和 texArea 对应的 UV 交给宿主，由宿主拥有网格与纹理生命周期；
* `render_stage` 与 JS 的 slot `"interior"` / `"exterior"` 对应；
* 返回模型句柄（负数 = 宿主无法构建，脚本必须跳过该次绘制）。

ABI 4/5 都改变了结构体尺寸，因此旧宿主会拒绝加载新模块（而不是越界读取）。

## ABI v3 变更

`JcmStop` 新增车站出口字段（JS: `stop.station.getExits()`）：

| 字段 | JS 来源 |
|---|---|
| `exit_count / exit_offset` | `station.getExits().size()` + `JcmExit[]` 池 |
| `JcmExit.name_offset/len` | `exit.getName()`（如 "A"） |
| `JcmExit.destination_count/offset` | `exit.getDestinations()` → `JcmStrRef[]` 数组 |

`examples/jslcd_vehicle.cpp` 的 `draw_exit_info()`（draw_common.js 的
`drawExitInfo` 全量移植）随之激活：「出站口 Exits」面板 = 路线色大字
母（唯一前缀折叠）+ 目的地中/英双行；门开显示当前站、行驶中显示下
一站——与 JS 语义完全一致。宿主无出口数据时写 `0/0`，面板自动跳过
（同 JS `if (!exits.length) return` 守卫）。

## ABI v2 变更

`JcmVehicleSnapshot` 新增（宿主在 marshalling 时写入，脚本端零成本读取）：

| 字段 | JS 来源 |
|---|---|
| `route_name_offset/len` | `thisRouteStops.get(0).route.name` |
| `route_color` | 同上 `.color`（ARGB） |
| `circular_state` | 同上 `.getCircularState()`（0/1/2 = NONE/CLOCKWISE/ANTICLOCKWISE） |
| `siding_name_offset/len` | `vehicle.getSiding().getName()`（车号来源） |

`JcmFrameOutput` 像素 arena 支持**按需增长**（初始 16MB 静态 BSS，
超出时切换到堆缓冲并保留复用）——多车厢 LCD 重绘突发的 50MB 帧不再
受固定容量限制。

## 与 JCM JS API 的映射

| JS (JCM v2.3) | C++ (本 SDK) |
|---|---|
| `function create(ctx, state, wrapper)` | `void create(Context&, State&, const Wrapper&)` |
| `function render(ctx, state, wrapper)` | `void render(Context&, State&, const Wrapper&)` |
| `function dispose(ctx, state, wrapper)` | `void dispose(Context&, State&, const Wrapper&)` |
| `ctx.drawCarModel(model, car, matrices)` | `ctx.draw_car_model(handle, car, &matrices)` |
| `ctx.playAnnSound(sound, vol, pitch)` | `ctx.play_ann_sound(sound, vol, pitch)` |
| `Text.create().text().pos().draw(ctx)` | `mtr::Text::create().text().pos().draw(ctx)` |
| `pids.arrivals().get(i).destination()` | `pids.arrivals().at(i).destination()` |
| `new GraphicsTexture(w, h)` + Java2D | `mtr::GraphicsTexture` + `mtr::Gfx2D`（纯 C++ 光栅化） |
| `new DisplayHelper(slotCfg)` | jslcd 移植：宿主 `acquire_model`（同槽位四边形）+ `GraphicsTexture` |
| `g.fillRoundRect / fillOval / drawLine / GeneralPath` | `Gfx2D::fill_round_rect / fill_oval / draw_line / fill_polygon` |
| `TextUtil.cycleString("A|B")` | `mtr::CycleTracker{"A|B"}` / `pids_util` |
| `be.redstoneLevel()` | `ec.redstone_level()` |
| `ctx.events().onBlockUse(cb)` | `ec.block_use_events()`（快照事件位） |

License: MIT（与 Joban Client Mod 一致）。
