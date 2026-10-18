<p align="center">
  <img src="assets/JCM_logo.png" width="180px" alt="JCM Logo">
</p>

<h1 align="center">
  Joban Client Mod — Yanyang（晏阳）
</h1>

<p align="center">Joban Client Mod 的晏阳技术组维护分支：在原版（[DistrictOfJoban/Joban-Client-Mod](https://github.com/DistrictOfJoban/Joban-Client-Mod)）之上为 Minecraft Transit Railway 4.0 带来<strong>自动识别语言的 JS + C++ 混合脚本</strong>、<strong>换乘线路类型 API</strong>与一系列<strong>渲染性能修复</strong>。</p>

<a align="center" href="https://weblate.ziyuesinicization.site/engage/joban-client-mod/"><img src="https://weblate.ziyuesinicization.site/widget/joban-client-mod/svg-badge.svg" alt="Translation status"></a>

> [!IMPORTANT]
> 本分支跟踪整合原版 JCM 的每个版本（当前基线：[2.3.0-beta.2](https://github.com/DistrictOfJoban/Joban-Client-Mod)），并保持向后兼容：原版资源包、`language: "cpp"` 老格式资源包均无需修改即可继续使用。
>
> 详细 C++ SDK 文档见 [`native/README.md`](native/README.md)，完整修改历史见 [CHANGELOG.md](CHANGELOG.md)。

## ✨ 当前版本：2.3.0-beta-2.yanyang.4

**GC 与稳态分配优化**（yanyang.4，针对周期性瞬时卡顿）：脚本渲染管线（JS + C++）的「捕获→回放」路径不再每帧重建全部 wrapper / 列表 / lambda / 矩阵容器——变换隔离改为标记/截断（`StoredMatrixTransformationsMixin`）取代逐绘制调用 `copy()`；原生绘制调用池化复用；`takeFrom` 零拷贝捕获取代逐车逐脚本整表复制；原生字符串与 `Identifier` 驻留；跑马灯字形缓存；每 pose 一次的 JOML 矩阵分配改为线程复用。多车地图上年轻代分配速率从「每秒数百 KB～MB 级」降到接近零，从根源上消除 GC 周期性停顿导致的掉帧。同时修复了两个过程中发现的正确性隐患：**跨车辆矩阵 arena 别名**（多车共享脚本模块时的回放串车）与 **WeakHashMap 自引用泄漏**（眼糖变换缓存的 lambda 捕获了键对象）——详见 [CHANGELOG](CHANGELOG.md)。

（yanyang.3）**全代码库 bug 与性能审计**（Java + JNI 边界 + C++ 桥接/SDK）：修复 F3+T 后 C++ 脚本从不重载且渲染线程 use-after-free、模块记录流越界可崩溃 JVM、单个坏脚本条目中断整包解析、C++ `Text`/`Texture` 默认值被清零导致绘制不可见、Forge 端原生车辆脚本只捕获不渲染、Forge 端轨道裁剪逻辑反转（原版上游同样存在）、阴影 pass 脚本双重驱动、骑乘时裁剪冻结玩家、`mtrDispose` 拿到首帧快照等 **7 项关键修复 + 30 余项渲染正确性、性能与加固修复**——详见 [CHANGELOG](CHANGELOG.md)。

**原生脚注扩展至 7 个平台**（yanyang.3）：jar 内置的 C++ 桥新增 **Linux ARM64**（树莓派/Asahi 等 ARM 桌面 + Pojav 类 Android 启动器）、**Android**（arm64-v8a，bionic）与 **iOS / iPadOS**（共用 arm64 Mach-O 镜像）三个目录，与原有 windows-x64 / linux-x64 / macos-x64 / macos-arm64 并列。运行时按 `os.name` + `os.arch` 匹配，Linux+aarch64 环境会依次尝试 linux-arm64 → ios-arm64 → android-arm64 镜像（iOS 沙箱内 JVM 会报 `os.name=Linux`），并修复了平台键把 Linux/Windows ARM64 一律归为 `-x64` 的旧 bug。

（yanyang.2）`minecraft_custom_resources.json` 的脚本条目**不再需要 `language` 字段**：`scriptLocations` 中的每个路径由模组自动判断语言，且**同一条目可同时混用任意数量的 JavaScript 与 C++ 脚本**——vehicle / lift / eyecandy / pids 四类脚本全部支持。

```json
"vehicleScripts": [
    {
        "id": "cst_sp1900_lcd",
        "prependExpressions": ["print('Hello world')"],
        "scriptLocations": ["mtr:my_pack/js/sp1900_lcd/main.js", "mtr:my_pack/natives/sp1900_train_num"],
        "input": {
            "yearEra": 2004
        },
        "dataFetchMode": "ALL"
    }
]
```

示例中 `mtr:my_pack/js/sp1900_lcd/main.js` 是 JS 脚本，`mtr:my_pack/natives/sp1900_train_num` 是 C++ 模块，两个脚本**同时加载、同时渲染**。其余字段（`prependExpressions` / `input` / `dataFetchMode` 等）与原版 2.3 文档一致：[Vehicle](https://jcm.joban.org/v2.3/dev/scripting/type/vehicle/) · [Lift](https://jcm.joban.org/v2.3/dev/scripting/type/lift/) · [Eyecandy](https://jcm.joban.org/v2.3/dev/scripting/type/eyecandy/) · [PIDS](https://jcm.joban.org/v2.3/dev/scripting/type/pids/)。

### 逐条目语言判定规则

| `scriptLocations` 条目 | 判定结果 |
| --- | --- |
| `…​/main.js`（大小写不敏感） | JavaScript |
| `mtr:my_pack/natives/sp1900_train_num`（无扩展名） | **C++ 平台无关 stem** → 各平台自动解析为 `natives/linux-x64/lib…​.so`、`natives/linux-arm64/lib…​.so`、`natives/windows-x64/….dll`、`natives/macos-arm64/lib….dylib`、`natives/ios-arm64/lib….dylib` 等（ARM64 Linux 环境自动追加 ios-arm64 / android-arm64 候选） |
| `…​/libfoo.so` / `…​.dll` / `…​.dylib` | C++ 完整路径（绑定对应平台，其它平台跳过） |
| `{"windows": …, "linux": …, "macos": …}` | C++ 按平台映射表（支持 `macos-arm64` 等精确键） |

- **混合运行不冲突**：JS 部分照常连接成单一 `ParsedScript`，C++ 部分全部挂到同一脚本 id；两侧各自捕获绘制指令、各自回放，互不干扰。某一侧缺失或加载失败时仅该侧跳过（附 latest.log / 调试模式提示），另一侧照常工作。
- **老格式完全兼容**：`"language": "cpp"` + `nativeLibrary` / `nativeLibraries` 仍是纯 C++ 条目；这些字段也可与 `.js` 的 `scriptLocations` 混用；`"language": "javascript"` 可强制全部按 JS 解析（无扩展名 JS 路径的逃生口）；MTR 3 老格式的 `script_files` 语义不变（无扩展名仍按 JS，`.so/.dll/.dylib` 与平台映射表按 C++）。
- **PIDS 预设**（`joban_custom_resources.json` 的 `pids_images`）同样支持 `scriptLocations` 自动判定，与老的 `scriptFiles` 并存。
- **电梯语义**：混合条目下内置渲染只被抑制一次、乘坐玩家移动每帧只应用一次（不会叠加）；eyecandy 的方块使用事件同时送达 JS 与 C++ 两侧。

## 🛠 晏阳版对原版的全部修改

以下为本分支相对原版 JCM 的**完整修改清单**（按层次划分；详细技术细节见各版本 [CHANGELOG](CHANGELOG.md) 条目）。

### 1. 资源包声明格式（`minecraft_custom_resources.json` / `joban_custom_resources.json`）

- `scriptLocations` **逐条目语言自动判定**，无 `language` 字段，同一条目可混用任意数量的 JS 与 C++ 脚本（本版本新增）；
- 混合条目优雅降级：C++ 库缺失 / 平台未声明 / JS 解析失败都只影响该侧，另一侧照常渲染；
- 老 `language: "cpp"` + `nativeLibrary`（单库）/ `nativeLibraries`（多库）声明继续可用，并可与 `.js` 混用；
- `lifts` / `objects` 的 `isScriptControlled`（原版 beta.2 改名）与 `isScriptRendered`（beta.1）**两个名字都接受**；
- 原生脚本的跨平台声明三种形式（stem / 完整路径 / 按平台映射表），运行平台未声明时跳过并输出 latest.log + 调试模式提示（游戏内黄字/调试 HUD），而不是报错或静默不渲染。

### 2. C++ 原生脚本运行时（原版完全没有的能力）

- **JNI 桥 + 原生宿主**：预编译 C++ 模块（`.so` / `.dll` / `.dylib`）从资源包解压加载，与 JS 脚本共用同一条 capture-and-replay 渲染管线；渲染、状态机、图形指令记录、纹理像素处理全部在 C++ 侧完成，不产生 Java 对象分配、不参与 JVM GC；
- **ABI 版本协商**：宿主 ABI 9；ABI 6/7/8 模块按各自原始布局继续加载（按 ABI 分组快照），老资源包无需重编译；
- **`getTransportType()` / `getTransportMode()`**（C++：`mtr::Train`、`mtr::Stop::Interchange`）：原生读取（无 Java 回调），可识别 MAGIC MTR Addon 线路类型；
- 每模块 1 MB 起步、按需增长的像素 arena；崩溃防护（`crash_guard`）与渲染错误 4 秒熔断；
- [native SDK](native/README.md)：头文件 + 构建脚本 + 三个示例模块（车辆/电梯/PIDS/eyecandy）× 7 平台预编译库，随 Release 以 zip 附带。

### 3. 脚本 API（JS / Java 侧）

| API | 说明 |
| --- | --- |
| `vehicle.getTransportType()` / `stop.routeInterchanges[i].getTransportType()` | 返回基础类型（`train` / `boat` / `cable_car` / `airplane`）或 MAGIC 类型（`train_metro`、`train_light_rail`、`train_high_speed`、`train_bus`、`train_tram`、`train_sbahn`、`train_normal`）；MAGIC 通过**可选反射**集成，无硬依赖 |
| `getTransportMode()`（不变） | 仍返回 MTR 的 `TransportMode` |
| 服务端 → 客户端脚本数据集 | 携带 `routeId` + `transportType`，新旧版本服混用也能拿到类型 |

### 4. 渲染性能修复（原版的已知问题）

- **消除 C++ 脚本每 500ms GC 卡顿**（yanyang.11）：StopsData 完整数据图按「数据集身份 + 修订号」跨帧复用（原版每帧每车重建数百个 Stop / RouteInterchange 对象）；快照序列化零分配（复用 Builder、持久字符串池、出口 memo）；移除每车每帧 256 KB JNI 快照拷贝；arena `ByteBuffer` 改为每模块缓存 GlobalRef 视图、每帧仅 rewind；有限站点数据 1s TTL + 路线签名缓存；
- **原生车辆视锥剔除**（yanyang.10，`VehicleCulling`）：整车包络在视锥外的列车不再支付快照序列化 + 整画布重绘 + 纹理上传；与原版 beta.2 的 MTR 层剔除互补，两层脚本都照常执行；
- 渲染线程零垃圾化：模块列表遍历不再复制、script-id 逐车 memo、字符串池返回打包 `long`、`IntStream` 换成复用缓冲的普通循环、纹理像素直接从 frame arena 拷入既有图像存储；
- 修复陈旧内置 bridge 导致的 `NoSuchMethodError`（yanyang.10）：JNI 查找失败立即清理并报告，新增 `checkNativeBridges` 构建校验；
- **全库热路径审计**（yanyang.3）：每车每帧一棵共享车厢位置树（原先 2N+1 棵，N 节列车 ≈ 每秒 1500 次全树重建）、仅对持有原生脚本的车辆计算视锥盒、零分配一次性追踪日志、`rasterizeText` 直读 `int[]` 光栅、gfx2d 复用草稿缓冲、`SCRIPT_IDS`/`TRACED`/`CAR_POSITIONS` 失效修剪、失败加载负缓存、稳定临时文件名（不再每次重载新增 `deleteOnExit`）、`clearDeadInstance` 不再每秒 40 次全表快照。

### 5. 原版版本整合策略

- 每个原版版本（如 2.3.0-beta.2 的轨道预览保留、Shift 独占选择、实验性 frustum culling、潜行降低视角、车门/光照/相机 API、cs_cz/de_de 等）逐文件三路合并，**保留晏阳全部自有能力**；
- 整合时附带晏阳侧优化：如 beta.2 的 `LiftWrapper` 车门可开启性懒惰计算（原版在每帧每电梯的构造函数里急切做世界查询）、C++ 电梯脚本与 JS 一致的 `VehicleRidingMovement` 乘坐语义。

### 6. 构建与发布体系

- 单一源码构建 **12 个 jar**（6 个 MC 版本 1.17.1 / 1.18.2 / 1.19.2 / 1.19.4 / 1.20.1 / 1.20.4 × Fabric / Forge），每个 jar 内置 7 个平台的 JNI bridge（`assets/jcm/natives/<platform>/`：windows-x64、linux-x64、linux-arm64、macos-x64、macos-arm64、android-arm64、ios-arm64），用户无需手动安装 DLL；
- CI（`build.yml`）：每次 push 在 6 个 MC 版本上验证编译；`release.yml`：5 平台原生 CTest（x64/ARM64 Linux、Windows、Intel/Apple macOS）+ 2 个交叉编译（Android NDK arm64-v8a、iOS arm64 Mach-O）→ 12 jar 构建 → `verify_jar.py` + `checkNativeBridges` 资产校验 → 发布 Release（含 native SDK zip）；
- 原生测试：`native/bench` 下 45 项 JVM 集成检查（真实 OpenJDK 21 JNI bridge）、ABI 布局回归、API 覆盖率探针。

## 📦 下载与安装

每个 Release 提供 **12 个 jar**（6 个 MC 版本 × Fabric/Forge 双加载器），均已内置 4 个桌面平台的 JNI bridge：

| Minecraft | Fabric | Forge |
| --- | --- | --- |
| 1.20.4 | ✅ | ✅ |
| 1.20.1 | ✅ | ✅ |
| 1.19.4 | ✅ | ✅ |
| 1.19.2 | ✅ | ✅ |
| 1.18.2 | ✅ | ✅ |
| 1.17.1 | ✅ | ✅ |

**前置**：[Minecraft Transit Railway 4.0.5](https://modrinth.com/mod/minecraft-transit-railway) + Fabric API（Fabric 端）。

> 想开发 C++ 脚本？下载 Release 附带的 **`jcm-native-sdk-<版本>.zip`**（含 SDK 头文件、构建脚本、示例模块的七平台预编译库），并阅读 [`native/README.md`](native/README.md)。

## 🔧 C++ 脚本架构与跨平台

脚本核心遵循一条简单规则：**「API 只用于取数据，其余任务原生完成」**——

- 数据获取（列车、站点、换乘、资源、输入）经由 JNI 桥接进入 C++ 快照，每帧一次；
- 渲染、状态机、图形命令记录、纹理像素处理全部留在 C++ 侧，**不产生 Java 对象分配**，因此不参与 JVM GC；
- 图形命令记录进 frame arena，由宿主在 Java 端一次性回放——与 JS 脚本的回放路径逐字节一致，这正是 JS 与 C++ 能在同一列车、同一脚本条目里共存的基础。

可移植性：

| 平台 | 支持方式 |
| --- | --- |
| Windows x64 / Linux x64 / macOS x64 / macOS ARM64 | CMake 一键构建，bridge 已随 jar 内置 |
| Android | 脚本核心 + JNI bridge 可用 NDK 编译 |
| iOS / iPadOS | 脚本核心不依赖 JVM：渲染与逻辑全在 C++，数据获取由宿主注入 |

```sh
# 构建与测试 native SDK（需要 JDK 21 与 Python 3）
cmake -S native -B native/build -DCMAKE_BUILD_TYPE=Release
cmake --build native/build --config Release --parallel 4
ctest --test-dir native/build -C Release --output-on-failure
```

## 🚀 快速上手：第一个 C++ 车辆脚本

```cpp
#include <mtr/script.hpp>

struct State { mtr::Mat4 base; };

class MyTrain final : public mtr::VehicleScript<State> {
    void create(mtr::Train& train, State& s) override { s.base = train.baseMatrix(); }
    void render(mtr::Train& train, State& s, mtr::Frame& f) override {
        // MAGIC MTR 类型原生可读：
        //   train.getTransportType()  -> "train_metro" / "train_high_speed" / ...
        //   train.getTransportMode()  -> TransportMode::TRAIN
        f.pushMatrix(s.base);
        f.drawTexture("jsblock:textures/generic.png", 0.0F, 0.0F, 1.0F, 1.0F);
        f.popMatrix();
    }
};

MTR_REGISTER_VEHICLE_SCRIPT(MyTrain, "example", "my_train")
```

资源包 `minecraft_custom_resources.json` 的 `scriptLocations` 里直接写上编译出的模块路径（或老格式 `"language": "cpp"` + `nativeLibrary`）即可——JS 与 C++ 脚本可在同一条目、同一资源包中并存。

## 📋 完整更新日志

见 [CHANGELOG.md](CHANGELOG.md)。各版本发布说明见 [Releases](https://github.com/BugQuantumCore/Joban-Client-Mod-Yanyang/releases)。

## 🤝 致谢

- 原版 [Joban Client Mod](https://github.com/DistrictOfJoban/Joban-Client-Mod)（AmberFrost）及其全体贡献者
- [Minecraft Transit Railway](https://github.com/Jonafanho/Minecraft-Transit-Railway)（jonafanho）
- [MAGIC MTR Addon](https://github.com/JustN00ne/magic-mtr)（JustNoone）
- 晏阳技术组（Yanyang Technology Group）

## License

This project is licensed under the MIT License.
