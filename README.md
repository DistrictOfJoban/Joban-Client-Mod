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
> - 分支持续跟踪整合原版 JCM 的每个版本（当前基线：[2.3.0-beta.2](https://github.com/DistrictOfJoban/Joban-Client-Mod)），并保持向后兼容：原版 JCM 资源包、`language: "cpp"` 老格式资源包均无需修改即可继续使用。
> - C++ 脚本开发文档：**[网页版开发文档](https://bugquantumcore.github.io/Joban-Client-Mod-Yanyang/)**（7 种语言）· [native/README.md](native/README.md)；完整修改历史见 [CHANGELOG.md](CHANGELOG.md)。

## ✨ 核心特性

### JS + C++ 混合脚本

vehicle / PIDS / lift / eyecandy 四类脚本均可用 C++ 编写，并与 JavaScript **在同一条目中自由混用、同时渲染**：

- `scriptLocations` 逐条目自动判定语言，无需声明 `language` 字段；
- 两侧各自捕获绘制指令、各自回放，互不干扰；某一侧缺失或加载失败时仅该侧跳过（latest.log / 调试模式提示），另一侧照常工作；
- C++ 与 JS 共享同一 `create` / `render` / `dispose` 生命周期与逐实例持久状态（`State`），`mtr::Api` 可调用 JavaScript 脚本能访问的全部公开 Java API。

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

### 相对原版的大量修复

对原版 JCM 与本分支全代码库（Java 渲染管线 + 原生传输 + C++ 桥接边界 + C++ SDK + 服务端门向系统）做过多轮审计修复：F3+T 后 C++ 脚本从不重载、Forge 端车辆脚本只捕获不渲染、ARM 平台结果槽内存序、JS 捕获/回放线程竞争、每部件每车厢全树重建、渲染线程 use-after-free、模块记录流越界可崩溃 JVM 等**关键修复 + 40 余项渲染正确性 / 性能 / 加固修复**——全部细节见 [CHANGELOG.md](CHANGELOG.md)。

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
| 前置模组 | [Minecraft Transit Railway](https://modrinth.com/mod/minecraft-transit-railway) 4.0.5+（必需）；Fabric 端另需 Fabric API |
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

### 4. 📖 C++ 脚本开发文档（网页版）

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
| 未发布 | 原生桥扩至 8 平台（windows-arm64）；开门方向迁移服务端并持久化（正线跨视距预测）；全模块执行与性能审计（JS 捕获/回放竞争、ARM 结果槽内存序、Forge 视锥剔除缺失、门向 O(P²) 等 20+ 项修复） |
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
