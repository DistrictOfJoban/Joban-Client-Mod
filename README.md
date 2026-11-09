<p align="center">
  <img src="assets/JCM_logo.png" width="180px" alt="JCM Logo">
</p>

<h1 align="center">
  Joban Client Mod — Yanyang（晏阳）
</h1>

<p align="center">Joban Client Mod 的晏阳技术组维护分支：在原版（[DistrictOfJoban/Joban-Client-Mod](https://github.com/DistrictOfJoban/Joban-Client-Mod)）之上，为 Minecraft Transit Railway 4.0 带来<strong>JS + C++ 混合脚本</strong>、<strong>零 JNI 原生运行时</strong>、<strong>服务端开门方向记忆</strong>、<strong>换乘线路类型 API</strong>与一系列<strong>渲染性能修复</strong>。</p>

<p align="center">
  <a href="https://github.com/BugQuantumCore/Joban-Client-Mod-Yanyang/releases"><img src="https://img.shields.io/github/v/release/BugQuantumCore/Joban-Client-Mod-Yanyang?include_prereleases" alt="Latest release"></a>
  <a href="https://bugquantumcore.github.io/Joban-Client-Mod-Yanyang/"><img src="https://img.shields.io/badge/docs-C%2B%2B_scripting-teal" alt="C++ scripting docs"></a>
  <a href="https://weblate.ziyuesinicization.site/engage/joban-client-mod/"><img src="https://weblate.ziyuesinicization.site/widget/joban-client-mod/svg-badge.svg" alt="Translation status"></a>
  <img src="https://img.shields.io/badge/license-MIT-green" alt="MIT License">
</p>

> [!IMPORTANT]
> - 本分支与原版 Joban Client Mod 使用同一 mod id（`jsblock`），**二者互斥，只能安装其一**。
> - 分支持续跟踪整合原版 JCM 的每个版本（当前基线：[v2.3.0 正式版](https://github.com/DistrictOfJoban/Joban-Client-Mod)），并保持向后兼容：原版 JCM 资源包、`language: "cpp"` 老格式资源包均无需修改即可继续使用。
> - 兼容官方 MTR 与晏阳服务器定制 MTR 构建（MTR-Yanyang）：对 MTR 内部类布局的修补 mixin 均带运行时结构检测（成员名 + 字段描述符双重校验），所用 MTR 构建缺少对应成员或布局分歧时自动跳过该修补并在日志说明（仅失去对应优化，其余功能不受影响），不会在启动时崩溃。MTR-Yanyang 自 2026-11-04 起的新构建已恢复官方 4.0.x `CachedResource` 布局，内存泄漏修补在其实机环境同样生效。
> - C++ 脚本开发文档：**[网页版开发文档](https://bugquantumcore.github.io/Joban-Client-Mod-Yanyang/)**（7 种语言）· [native/README.md](native/README.md)；完整修改历史见 [CHANGELOG.md](CHANGELOG.md)。

## ✨ 核心特性

### JS + C++ 混合脚本

vehicle / PIDS / lift / eyecandy 四类脚本均可用 C++ 编写，并与 JavaScript **在同一条目中自由混用、同时渲染**：

- `scriptLocations` 逐条目自动判定语言，无需声明 `language` 字段；
- 两侧各自捕获绘制指令、各自回放，互不干扰；某一侧缺失或加载失败时仅该侧跳过（latest.log / 调试模式提示），另一侧照常工作；
- C++ 与 JS 共享同一 `create` / `render` / `dispose` 生命周期与逐实例持久状态（`State`），`mtr::Api` 可调用 JavaScript 脚本能访问的全部公开 Java API；C++ SDK 与上游 JS API 面同步维护（`mtr::Rect`、`full_aa()` / `no_aa()` 抗锯齿、`facing_angle()`、`StateTracker`、`renderType(String)` 等）。

### 零 JNI 原生运行时

Java ↔ C++ 之间完全不经 JNI：JDK 16–21 使用共享内存 poller 平面，JDK 22+ 使用 Panama FFM 平面（配置界面可切换）。渲染、状态机、图形指令记录、纹理像素处理全部留在 C++ 侧完成——热路径零 Java 对象分配，不参与 JVM GC。

### 8 平台原生桥，开箱即用

每个 jar 内置八个平台的原生桥（`assets/jcm/natives/`）：**windows-x64 / windows-arm64 / linux-x64 / linux-arm64 / macos-x64 / macos-arm64 / android-arm64 / ios-arm64**。运行时按系统与架构自动匹配（ARM64 Linux 自动回退链；Windows on ARM 使用原生 ARM64 桥——x64 DLL 无法在 ARM64 JVM 进程内加载），无需手动安装任何 DLL / so。资源包模块需为所用平台提供对应目录（如 `natives/windows-arm64/`）。

### 服务端开门方向记忆（`getDirectionOfDoorOpening`）

`vehicle.getDirectionOfDoorOpening()`（JS / C++ 一致，返回 `"not"` / `"left"` / `"right"` / `"all"`）：识别车辆在**当前站**（停靠时，与车门开闭阶段无关）或**下一站**（正线运行时预测）的开门侧。同侧任一车门可开即整侧可开；左右相对车头，不受倒行影响；纯查询，不改变 MTR 实际开门行为。

正线预测的答案由**服务端逐线路持久记忆**提供：服务端装本模组时，首次启动对全档所有有线路经过的站台做一次全量扫描（未加载区块按需载入——客户端物理上做不到的能力），结果按线路持久化在存档内（`routes-door-directions/`，与 MTR 自身分片规则一致），之后站台方块变化（含 WorldEdit / Axiom 批量编辑）按区块合并增量重扫、站台重建自动收敛。因此预测**跨视距、跨会话有效**——列车驶出站台后 LCD 立即能轮播下一站信息，不再需要等站台进入视野。服务端未安装本模组时自动回退为客户端视距内扫描。

### 帧稳定性工程

- 捕获→回放管线**稳态零分配**：变换标记/截断隔离取代逐绘制调用拷贝、绘制调用池化、字符串驻留，从根源消除 GC 周期性停顿；JS 与 C++ 两路捕获/回放均线程安全（执行线程捕获、渲染线程回放共享监视器）；
- **原生车辆视锥剔除（双加载器）**：视锥外列车不再支付快照封送、画布重绘与纹理上传；Forge 端经 `LevelRenderer#cullingFrustum` 直读实现，与 Fabric 端同语义；
- 脏矩形纹理上传 + 逐模块像素 arena（1 MB 起步按需增长、尖峰后衰减收缩）+ 离线程内存回收；
- **实例热保持**：列车离开视锥不再销毁，重新入镜零重建；空闲阀门 `-Djcm.nativeIdleMillis` 与热实例上限 `-Djcm.nativeIdleMax` 约束内存占用；车辆与 PIDS / 装饰 / 电梯实例统一 2/tick 销毁预算，批量退出一次性冲刷。

### 换乘线路类型 API

`getTransportType()`（JS / Java / C++ 三侧一致）：除 MTR 基础类型（`train` / `boat` / `cable_car` / `airplane`）外，可识别 MAGIC MTR Addon 线路类型（`train_metro`、`train_light_rail`、`train_high_speed`、`train_bus`、`train_tram`、`train_sbahn`、`train_normal` 等）——经可选反射集成，无硬依赖。

### 脚本执行防护（上游 v2.3.0-prerelease.2）

预设级 `scriptRateLimit` 速率限制 + 全局执行预算：解析阶段超时不再挂起游戏，无限循环可被检测并中止；同一脚本不再重复编译，DataReader 运行时缓存按 tick 过期自动重置；解析错误以游戏内悬停文本展示。上游 Iris / OptiFine 阴影 pass 重复执行脚本的修复已覆盖电梯 / 车辆 / 装饰物 / PIDS 全部脚本渲染面，BVE 电机声音卡死等 MTR 4 著名缺陷亦随同步修复。

### 相对原版的大量修复

对原版 JCM 与本分支全代码库（Java 渲染管线 + 原生传输 + C++ 桥接边界 + C++ SDK + 服务端门向系统）做过多轮审计修复：F3+T 后 C++ 脚本从不重载、Forge 端车辆脚本只捕获不渲染、脚本化 PIDS 文字随相机漂移（池化快照丢失世界锚点，yanyang.8 回归）、ARM 平台结果槽内存序、JS 捕获/回放线程竞争、每部件每车厢全树重建、渲染线程 use-after-free、模块记录流越界可崩溃 JVM 等**关键修复 + 40 余项渲染正确性 / 性能 / 加固修复**——全部细节见 [CHANGELOG.md](CHANGELOG.md)。

## 📦 下载与安装

| 渠道 | 链接 |
| --- | --- |
| GitHub Releases | https://github.com/BugQuantumCore/Joban-Client-Mod-Yanyang/releases |
| Modrinth | https://modrinth.com/project/kMMJcRqY |
| CurseForge | https://www.curseforge.com/projects/1736483 |

每个 Release 提供 **12 个 jar**（6 个 MC 版本 × Fabric / Forge 双加载器，均已内置 8 平台原生桥）与 **native SDK zip**（`jcm-native-sdk-<版本>.zip`）：

| Minecraft | Fabric | Forge |
| --- | --- | --- |
| 1.20.4 | ✅ | ✅ |
| 1.20.1 | ✅ | ✅ |
| 1.19.4 | ✅ | ✅ |
| 1.19.2 | ✅ | ✅ |
| 1.18.2 | ✅ | ✅ |
| 1.17.1 | ✅ | ✅ |

**环境要求**

| 项目 | 要求 |
| --- | --- |
| Minecraft | 1.17.1 – 1.20.4（Fabric / Forge） |
| 前置模组 | [Minecraft Transit Railway](https://modrinth.com/mod/minecraft-transit-railway) 4.0.5+（必需；官方构建或 MTR-Yanyang 等定制构建均可）；Fabric 端另需 Fabric API |
| Java | 16 及以上（22+ 自动启用全程零 JNI 的 FFM 平面） |
| 与原版 JCM | **互斥**（同一 mod id，二选一，不能共存） |

> [!TIP]
> - 单人游戏 / 局域网联机：本模组即含服务端逻辑，开门方向记忆开箱即用。
> - 专用服务器：**服务端也请安装本模组**，正线开门方向预测才能跨视距生效；客户端单装时自动退化为视距内扫描，其余功能不受影响。
> - Modrinth / CurseForge 项目若仍在平台审核中，请先从 GitHub Releases 下载。

## 🚀 快速上手

### 1. 声明一个混合脚本条目

```json
"vehicleScripts": [
    {
        "id": "my_pack:train_lcd",
        "scriptLocations": [
            "mtr:my_pack/js/train_lcd/main.js",
            "mtr:my_pack/natives/train_num"
        ],
        "input": { "theme": "dark" },
        "dataFetchMode": "ALL"
    }
]
```

第一个路径是 JS 脚本、第二个是 C++ 模块 stem，两个脚本**同时加载、同时渲染**。其余字段（`prependExpressions` / `input` / `dataFetchMode` 等）与原版 2.3 文档一致：[Vehicle](https://jcm.joban.org/v2.3/dev/scripting/type/vehicle/) · [Lift](https://jcm.joban.org/v2.3/dev/scripting/type/lift/) · [Eyecandy](https://jcm.joban.org/v2.3/dev/scripting/type/eyecandy/) · [PIDS](https://jcm.joban.org/v2.3/dev/scripting/type/pids/)。

**逐条目语言判定**：

| `scriptLocations` 条目 | 判定 |
| --- | --- |
| `…/main.js`（大小写不敏感） | JavaScript |
| `mtr:my_pack/natives/my_module`（无扩展名） | C++ 平台无关 stem，各平台自动解析为 `natives/<platform>/lib….so` / `….dll` / `lib….dylib` |
| `…/libfoo.so` / `….dll` / `….dylib` | C++ 完整路径（绑定对应平台，其它平台跳过） |
| `{"windows": …, "linux": …, "macos": …}` | C++ 按平台映射表（支持 `windows-arm64`、`macos-arm64` 等精确键） |

**兼容性**：老 `language: "cpp"` + `nativeLibrary` / `nativeLibraries` 声明继续可用，并可与 `.js` 混用；`"language": "javascript"` 可强制按 JS 解析；MTR 3 老格式 `script_files` 语义不变。

### 2. 第一个 C++ 模块

```cpp
#include <mtr/script.hpp>

struct ArrivalState {};                    // 逐实例持久状态（本例为空）

struct ArrivalDisplay : mtr::PidsScript<ArrivalState> {
    static constexpr auto ID = "example:arrivals";   // 必须与资源包声明一致

    void render(mtr::PidsContext& ctx, ArrivalState&,
                const mtr::Pids& pids) override {
        for (int row = 0; row < pids.rows(); row++) {
            if (pids.is_row_hidden(row)) continue;

            std::string text = pids.get_custom_message(row).str();
            if (text.empty() && row < pids.arrivals().size())
                text = pids.arrivals().at(row).destination().str();

            if (!text.empty())
                mtr::Text::create().text(text.c_str())
                    .pos(2, row * 10.0 + 2)
                    .size(pids.width() - 4, 9)
                    .draw(ctx);
        }
    }
};

MTR_REGISTER_PIDS_SCRIPT(ArrivalDisplay)
```

编译为模块后按平台目录放进资源包（如 `natives/windows-x64/my_arrivals.dll`、`natives/windows-arm64/my_arrivals.dll`、`natives/linux-x64/libmy_arrivals.so` …），再在 `scriptLocations` 中声明 stem 即可。车辆（`mtr::VehicleScript`）/ 电梯 / 装饰脚本同理——完整教程见下方文档。

### 3. 开门方向查询（JS / C++）

```js
// JS（车辆脚本）
const dir = vehicle.getDirectionOfDoorOpening();   // "not" / "left" / "right" / "all"
```

```cpp
// C++（车辆脚本）
const mtr::StrView dir = vehicle.getDirectionOfDoorOpening();
switch (vehicle.doorDirection()) {
    case mtr::door_direction::LEFT:  /* 左侧开门 */ break;
    case mtr::door_direction::RIGHT: /* 右侧开门 */ break;
    case mtr::door_direction::ALL:   /* 双侧开门 */ break;
    default:                         /* 两侧都不开 */ break;
}
```

### 4. 屏蔽门脚本（PSD Scripts，玻璃 / 顶部面板）

在 `mtr_custom_resources.json` 中新增 `psdScripts` 类别——形式同 `vehicleScripts`，新增 `"type"` 设置项（`"glass"` 绑定屏蔽门玻璃 / `"top"` 绑定屏蔽门顶部面板）：

```json
"psdScripts": [
    {
        "id": "psd_glass_pids",
        "type": "glass",
        "scriptLocations": [
            "mtr:my_pack/js/psd_glass_pids/main.js",
            "mtr:my_pack/native/psd_glass_pids"
        ],
        "dataFetchMode": "ALL"
    }
]
```

**游戏内绑定**：使用新物品 **屏蔽门脚本编辑器**（与 MTR 的「刷」在同一创造分类下）对任意屏蔽门玻璃或屏蔽门上方面板右键，在弹出界面中选择一个该类型的 `psdScripts` 条目 **或** 任一脚本化 PIDS 预设（PIDS 脚本原样运行，画布自动适配 PSD 表面）。绑定保存在服务端（`<世界>/mtr/<维度>/psd-scripts/bindings.json`）并同步给同维度的所有玩家；选择「无脚本」即解除绑定。

**渲染效果**：
- **玻璃**（含端部玻璃、半高闸门 APG 玻璃；MTR 与天津地铁等基于 MTR PSD 类的模组均支持）：变为不透明白色，脚本控制的动态贴图叠加在白色之上（仅站台侧显示；背面为纯白色，列车内乘客不会看到镜像贴图）。
- **顶部面板**：不再显示原有的箭头 / 线路图，显示内容完全由脚本控制（78×42 像素画布）。

**脚本 API（JS 的 `psd` 参数 / C++ 的 `mtr::Psd`）**——绘制 API 与 PIDS 完全一致（`Text` / `Texture` / `Rect` + `ctx.draw`），数据 API 覆盖：

| API | 说明 |
| --- | --- |
| `psd.type()` / `width()` / `height()` | 绑定表面（glass/top）与画布尺寸（玻璃 96×96，APG 上半 96×54，面板 78×42 PIDS 像素） |
| `psd.platform()` | 当前站台：编号 / 名称（`number()` / `name()`）、所属车站、停站时间 |
| `psd.stop()` | **当前车站完整信息**：名称、颜色（ARGB / #RRGGBB）、票务分区 zone1–3、经停本站的全部线路、换乘站线路（按站名分组）、物理连通车站、出口与目的地、站台编号列表与数量（C++：`mtr::Psd::stop()` → `mtr::PsdStation`） |
| `psd.routes()` | 经过此站台的**所有线路**：名称 / 编号 / 颜色 / 环线状态 / 交通类型；`dataFetchMode: "ALL"` 时每条线路附带**完整车站列表**（站名、到站显示、自定义到站、**换乘线路**（颜色 + 名称 + 交通类型）、车站出口与出口目的地） |
| `psd.arrivals()` | 即将到达的列车：终点站、所属线路（名称 / 编号 / 颜色）、车厢数、到站倒计时、实时 / 环线 / 终到状态，以及 **`doorNumbers()` —— 该列车将开启的屏蔽门编号**（停站中的列车读实际开门状态，未到站按「列车停于站台中点 + 按车辆资源逐卡累加车长」近似预测） |
| `psd.doors()` / `doorValue()` | 本面屏蔽门墙的门单元列表（1 起编号）与实时开门状态 / 上锁状态、整墙开门量 |

C++ 侧为第五类脚本基类 `mtr::PsdScript<State>`（`MTR_REGISTER_PSD_SCRIPT`），实现方式与车辆 C++ API 相同——每帧一次数据快照（`JcmPsdSnapshot`，ABI 13，含完整车站信息快照），零 JNI、零逐帧分配。见 `native/examples/psd_glass.cpp`。

### 5. 📖 C++ 脚本开发文档（网页版）

**https://bugquantumcore.github.io/Joban-Client-Mod-Yanyang/**

| 页面 | 内容 |
| --- | --- |
| 概览 | 什么是 C++ 原生脚本、架构、何时选 C++ |
| 快速上手 | SDK 获取、模块工程、全平台编译、打包进资源包、游戏内验证 |
| 脚本模型与生命周期 | 四类基类、State 语义、线程模型与崩溃隔离 |
| 数据快照参考 | Vehicle / Stop / Arrival / Pids / Lift / EyeCandy 全访问器 |
| 绘制与画布 | Text / Texture、Matrices、GraphicsTexture、Gfx2D、字体 |
| Java 互操作 | `mtr::Api`、对象与回调、JS↔C++ 对照表 |
| C ABI 与宿主服务 | 导出函数、帧输入输出、绘制记录、ABI 演进 |
| 性能与调试 | 热路径守则、`defer_free`、`latest.log`、FAQ |

站点支持**浅色 / 深色双主题**（跟随系统偏好，手动切换后记住选择）与 **7 种语言**：简体中文（中国大陆）· 繁體中文（香港特別行政區）· 繁體中文（台灣）· 文言（華夏）· English (US) · English (UK) · Français (France)。源文件位于 [`Develop-Documentation-Website` 分支](https://github.com/BugQuantumCore/Joban-Client-Mod-Yanyang/tree/Develop-Documentation-Website)（纯静态、零依赖）。SDK 也可直接从 Release 附件 `jcm-native-sdk-<版本>.zip` 获取（含全部头文件、构建脚本、示例模块与八平台预编译库）。

## 🛠 从源码构建

```sh
# 原生 SDK：构建与测试（需要 JDK 21 与 Python 3）
cmake -S native -B native/build -DCMAKE_BUILD_TYPE=Release
cmake --build native/build --config Release --parallel 4
ctest --test-dir native/build -C Release --output-on-failure
```

- **`build.yml`**：每次 push / PR 在全部 6 个 MC 版本上快速验证编译（快速 CI jar 不含原生桥；游戏可用的完整 jar 请使用 Release 或 `release.yml` 产物）；
- **`release.yml`**：6 平台原生 CTest（x64 / ARM64 Linux、x64 / ARM64 Windows、Intel / Apple macOS）+ 2 项交叉编译（Android NDK arm64-v8a、iOS arm64 Mach-O）→ 12 jar 构建 → `verify_jar.py` + `checkNativeBridges` 资产校验（断言 8 桥齐全）→ 发布 Release（含 native SDK zip）；
- **`complete-build.yml`**：与 release 同构但不打 tag 不发布，随时产出游戏可直接导入的完整测试 jar；
- **`native/bench`**：基于真实 JVM 的集成检查（FFM 平面 70 项 / poller 平面 67 项）+ ABI 布局回归 + API 覆盖率探针。

## 📋 版本沿革

完整更新日志见 [CHANGELOG.md](CHANGELOG.md)，各版本发布说明见 [Releases](https://github.com/BugQuantumCore/Joban-Client-Mod-Yanyang/releases)。

| 版本 | 主题 |
| --- | --- |
| `2.3.0-1.0.1` | **修复「开光影时屏蔽门玻璃显示脚本时不变色」——第七轮（终局）载体修复：底板改骑 drawText 管线**：七轮用户实测排除全部候选后（MTR 队列消费者全线——含 yanyang.13 beacon-beam 与 2.3.0-1.0.0 的 MTR 路线牌同款 EXTERIOR 纪律——在该光影栈下对玻璃后的四边形一律不可见；私有 Immediate + `drawTextureInWorld` 则败于其 NEW_ENTITY 元素序：OVERLAY 值落进 glyph 层 LIGHTMAP 槽（yanyang.12 实为 lightmap 0 渲染）、entity 层扩展元素未填充），底板双绕向（站台黑 / 轨道白，白面先提交）改为在 LIGHT_2 队列回调内新建私有 `immediate`（`GraphicsHolder.drawText` 逐字节同款来源）+ **手写 glyph 顶点序列**（`vertex→color→texture→light→next`，真实 light 值）写上 `RenderLayer.getText(white.png)` + 当场同步 flush——与用户实机每天可见的脚本文字管线除纹理与几何外零差异。新增一次性 INFO 面包屑 `PSD backing glyph carrier active` 供下轮日志确认执行；`jcm$getMatrixStack()` 访问器恢复 |
| `2.3.0-1.0.0` | **主版本升级：同步上游 JCM v2.3.0 正式版**（主版本 2.3.0-prerelease.3 → 2.3.0，子版本 yanyang.13 → 1.0.0）：上游 v2.3.0 全部增量逐文件核对并入（绝大部分经 blob 级比对证实本分支已先行包含，真正缺项全部落地）。**语言文件全量同步**：新增韩语 / 荷兰语 / 荷兰语中古彩蛋语 / 法语四种新语言，日语并入上游更新，共 15 种语言。**修复「开光影时底色不显示（任何光影包均不显示）」**（第六轮尝试，EXTERIOR 队列方案——实机证实仍不可见，为 1.0.1 的根因定位提供排除证据） |
| `2.3.0-prerelease.3-yanyang.12/13` | 画布塌缩修复（pane 几何改为全部平台向四边形的并集，修「脚本仅完整渲染在整块玻璃左上角 1/4」）；底板两轮光影载体尝试（drawText 同款 text 层 → MTR 铁路标志牌 beacon-beam 层）——实机均证实不可见，但为 2.3.0-1.0.0 的 Iris 透明度分相位根因定位提供了排除证据 |
| `2.3.0-prerelease.3-yanyang.11` | 修复 yanyang.10 回归「画布位置完全跑偏、比例完全错误」：水平窗口公式把扫描返回的窗口**右边缘**当成**右侧遮挡宽度**（`[i+left, (i+1)-right]` 应为 `[i+left, i+right]`），任何有真实窗口的列都算出零宽被判全遮挡、画布塌缩成错位窄条；并补上朝向镜像（块空间水平轴与画布帧 +X 在 SOUTH / WEST 墙上方向相反，窗口需映射为 `[i+(1-right), i+(1-left)]`）。修复「光影下黑底 / 白底从未显示过」：底板载体彻底换轨为 MTR `drawText` 同款「私有 Immediate 包共享 Tessellator + 同步 flush」（反汇编证实其为唯一在用户 Iris + Complementary + ImmediatelyFast 环境下从未失效的载体；所有队列载体均被 ImmediatelyFast 推迟到半透明地形后遭深度测试丢弃），两绕向（站台黑 / 轨道白，entityCutout 写深度 + 背面剔除）在队列回调内借用持有者矩阵（新增 `NativeMatrixApplier.jcm$getMatrixStack()` mixin 访问器——yanyang.4-7 私有 Immediate 唯一败因即裸单位矩阵）同步绘制，白面深度同时把画布内容挡在轨道侧之外（修「内容出现在轨道侧」） |
| `2.3.0-prerelease.3-yanyang.10` | 修复「脚本内容越界依然存在」：画布补上**水平**窗口裁剪（垂直窗口的水平孪生——列采样 alpha 遮挡 + 最大连续透明跨度），画布从整段整格宽收缩到透明跨度、锚点平移到跨度起点，`pids.width` 即可见宽度，任何脚本的行布局（含右对齐 ETA / 跑马灯文本框）自动避开门框立柱，首尾字符不再被立柱咬掉；段中竖条断开跨度时取较宽一侧，窗口推导不可用整段回退。修复「开启光影时玻璃依然透明」：站台侧黑面从自建 entity-cutout 消费者（Iris 光影下被丢弃）改为脚本内容同款 **getText 层载体**（以纹理为队列 key 排 LIGHT_2、回调不再另建消费者——JCM PIDS 生态久经验证的光影安全路径），无光影视觉不变；轨道侧白面独立排 EXTERIOR 队列（entity-cutout 写深度）并深 0.0005，靠深度测试剔除黑面轨道侧背面，双面双色依然成立 |
| `2.3.0-prerelease.3-yanyang.9` | 修复 yanyang.8 遗留的「黑色覆盖不完全」：底板此前沿用脚本画布的严格 alpha 窗口（半透明边缘色带被算作门框），玻璃窗口上下边缘留下可透视的窄条——底板是不透明纯色四边形，根本不需要 alpha 裁剪，现改为铺满面板自身的几何范围（+1/64 余量），不透明门框部件在前遮挡余量、半透明色带后方终于是实色（站台侧黑色、轨道侧同步全覆盖）。列车一侧按需求改回**白色**：双绕向双面绘制本就一西一东，站台绕向黑 (PIDS 黑底)、轨道绕向白 (原设计色)，车厢乘客隔窗只看到纯白面板；轨道侧白、站台侧黑互不干扰，仍不透出镜像内容。脚本画布继续用严格窗口（内容不落上色带/门框条带），随底板范围扩大，内容现严格落在黑区之内 |
| `2.3.0-prerelease.3-yanyang.8` | 修复底板从未正确锚定的真正根因——yanyang.4 起的「私有 Immediate 同步 flush」画在裸单位矩阵栈上，相机旋转从未生效（不开光影时错位成悬浮色板随镜头漂移、开 Iris 被光影管线直接丢弃、站台侧不可见——实机三症同源）；yanyang.4 的「延迟 flush 输给半透明玻璃深度」理论被证伪（同一面板后的脚本画布每版可见）。底板改走脚本内容同款 `MainRenderer.scheduleRender` 延迟队列路径（LIGHT_2，帧尾在 MTR 自己的矩阵 holder 上 flush、以 flush 时相机偏移锚定世界坐标）。底板颜色按需求由白改黑：站台侧黑底 + 脚本内容（真实 PIDS 观感），轨道侧纯黑；复用 `jsblock:white` 纹理 + 黑色顶点色调，无新资源 |
| `2.3.0-prerelease.3-yanyang.7` | 修复白底自 yanyang.3 起从未显示——真正根因是深度而非 flush / 纹理：资源包在玻璃面板内自带白色漫射板（位于旧画布位 +0.5/16 与旧底板位 +1/16 之间，画布文字每版可见而底板每版不可见即为其存在实证），三明治整体前移至面板前部（画布 +0.25/16、白底 +0.45/16，均在已验证可见平面前方），白底同步 flush 改用 MTR drawText 同款 Tessellator 缓冲；窗口遮挡改双阈值——严格遍（alpha ≥ 51 即门框，半透明门框条带不再被当作窗口、脚本内容不再画到条带上）+ 宽松遍兜底（染色玻璃包维持旧行为）。附 yanyang.5（jsblock:white + 遮挡 v2）/ yanyang.6（整段绑定修复取消选中 + 上下半合并单画布修复双重渲染）补录 |
| `2.3.0-prerelease.3-yanyang.4` | 修复玻璃白底在 Iris / Sodium / ImmediatelyFast 环境下完全不渲染（共享实体阶段消费者被延迟到半透明地形之后 flush，位于面板内的白底深度测试失败——现复刻 MTR `drawText` 的私有 Immediate + 同步 flush 纪律，任何管线都先于半透明玻璃落地）；新增 `PSDWindowBounds`：脚本画布与白底按玻璃「透明窗口」裁剪，窗口边界从资源包的实际烘焙模型 + 纹理 alpha 运行时逐像素推导（阈值 170，半透明染色窗口与边框皆可区分），任何包的边框厚度 / 窗口形状自动适配，脚本内容不再落到门框条带上；顺带修复 MTR APG 半高闸门上半画布悬浮在面板上方的遗留错位（窗口推导天然以面板实际几何为界）。仅 1.20.x 提供窗口推导（旧版本整面回退） |
| `2.3.0-prerelease.3-yanyang.3` | 玻璃脚本渲染重做为「三明治」：白底 + 脚本画布画进玻璃面板内部，只透过原本透明的窗口可见、玻璃框（边框 / 立柱 / 磨砂带）原样保留并自然裁切画布，轨道侧仍只见白色（旧版白底画在玻璃面前方 0.0005 格，覆盖边框且在 Sodium / Iris 深度精度下易闪烁丢失）；psd_top 画布与原生内容取消范围从整条连续带收缩到绑定方块所在的墙体分段（按下方墙体推导——门段 / 玻璃段，APG 墙上方手动放置的 psd_top 其 SIDE_EXTENDED 全为默认值不可用），其余分段恢复 MTR 原生箭头 / 线路图；PSD 画布 z-order 方向修正（后绘制在上，分层脚本文字不再被背景遮挡） |
| `2.3.0-prerelease.3-yanyang.2` | 修复绑定脚本化 PIDS 预设后 PSD 画布完全空白（合成包装器自定义消息为 null 数组，`getCustomMessage(i) != ""` 对 null 判真、每行都画空文本——现与真实 PIDS 一致返回空串）；玻璃白底与脚本画布横向扩展到整段刷子合并的玻璃（门为段边界，画布宽 = 段宽 × 96px、锚定段左端）；psd_top 脚本画布横贯整段顶板带并整段取消原生箭头 / 线路图（走查规则与 MTR 路线图纹理完全一致，取消范围与画布覆盖逐块对齐） |
| `2.3.0-prerelease.3-yanyang.1` | 同步上游 v2.3.0-prerelease.3；PSD 玻璃背面改纯白；新增 psd.stop() 车站信息 API（JS + C++ ABI 13）；修复 MTR-Yanyang 定制构建启动崩溃（mixin 成员名 + 字段描述符双重结构检测，官方 MTR 不受影响；MTR-Yanyang 仓库侧亦已恢复官方布局，新构建上泄漏修补照常生效）；补齐屏蔽门脚本编辑器物品模型 / 贴图（此前显示为紫黑方块）；修复编辑器选择界面不显示 PIDS 脚本（内置 PIDS 1A 等脚本化预设此前被整体排除在列表外）；修复绑定脚本后 PSD 不渲染脚本内容（回放误读已清空的上下文管理器）、DRL 半高闸门玻璃上半悬空白板、旧世界陈旧 MTR 方块实体在 DRL 玻璃上悬浮线路图、psd_top 画布侧边距错误 |
| `prerelease.2-yanyang.1-hotfix.2` | 原生层两轮人工审查修复首次发布（8 + 3 项）：直接调用模式内存安全（arena 偏移 / 记录长度校验）、每帧 re-attach 泄漏扩容像素 arena、重载代际模块堆内存治理（`mtrCleanup`）、字符串缓存满后插入不可找回（改 16×8 固定分组）、同库多句柄共享锁 + 末句柄清理、空字符串中断 PIDS 帧、阴影阶段重复执行、无原生脚本车辆缓存淘汰、变换池回收滞留 |
| `prerelease.2-yanyang.1-hotfix.1` | 热修：MTR 4 原生路线类别（高铁 / 轻轨）流入换乘类型链，原版高铁、轻轨线路正确从车侧线路图去除；Axiom 区块批量编辑纳入开门方向重扫；iOS 交叉编译腿回归 macos-latest（arm）runner |
| `prerelease.2-yanyang.1` | 上游 v2.3.0-prerelease.2 全量同步（61 提交：脚本限流 / 防死循环、脚本核心解耦、BVE 声音卡死与 Iris / OptiFine 阴影修复等）；脚本化 PIDS 文字乱飘修复（世界锚点）；C++ SDK 与上游 API 面同步（Rect / fullAA / StateTracker 等） |
| `yanyang.11` | PSD 覆盖板：修复水平窗口公式灾难性 bug（边缘 vs 遮挡宽度 + 朝向镜像）修画布跑偏；底板改 drawText 同款同步 flush 载体（光影下首次可望显示），白面深度挡住轨道侧内容泄露 |
| `yanyang.10` | PSD 覆盖板：画布水平窗口裁剪（文字不再被门框立柱咬首尾）；站台黑面改 getText 层（光影安全），轨道白面独立深度层 |
| `yanyang.9` | PSD 覆盖板：底板铺满面板几何范围（半透明边缘色带后方实色，修「黑色覆盖不完全」）；列车侧改回白色（双绕向各侧各色：站台黑 / 轨道白） |
| `yanyang.8` | 脚本化 PIDS 渲染修复（yanyang.4 回归）；开门方向服务端持久记忆与 `getDirectionOfDoorOpening()`（C ABI 11）；原生桥扩至 8 平台（windows-arm64）；全模块执行 / 性能审计 20+ 项修复 |
| `yanyang.7` | 实例热保持：列车离开视锥不再销毁，消除转头时的间歇性卡顿；空闲阀门与销毁预算 |
| `yanyang.6` | 修复列车 LCD 永久黑屏（实例销毁与 Java 侧 id 缓存不同步） |
| `yanyang.5` | 传输平面健壮性审查：poller 线程自愈、消除 JVM 崩溃风险等 9 处修复 |
| `yanyang.4` | 捕获→回放稳态零分配（GC 停顿整治）；`-shmtest5` 起模组全面去 JNI |
| `yanyang.3` | 全代码库审计：7 项关键修复 + 30 余项加固；原生桥扩展至 7 平台 |
| `yanyang.2` | `scriptLocations` 逐条目语言自动判定，同一条目混用 JS 与 C++ |
| `yanyang.1` | 整合原版 `2.3.0-beta.2`（实验性视锥剔除、潜行相机、车门 / 光照 / 相机 API 等） |
| `2.3.0-beta.1-yanyang.1…11` | 混合脚本奠基：原生桥、`getTransportType()`、零分配换乘数据快照、原生视锥剔除 |

## 🤝 致谢

- 原版 [Joban Client Mod](https://github.com/DistrictOfJoban/Joban-Client-Mod)（AmberFrost）及其全体贡献者
- [Minecraft Transit Railway](https://github.com/Jonafanho/Minecraft-Transit-Railway)（jonafanho）
- [MAGIC MTR Addon](https://github.com/JustN00ne/magic-mtr)（JustNoone）
- 晏阳技术组（Yanyang Technology Group）

## License

This project is licensed under the MIT License.
