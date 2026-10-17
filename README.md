<p align="center">
  <img src="assets/JCM_logo.png" width="180px" alt="JCM Logo">
</p>

<h1 align="center">
  Joban Client Mod — Yanyang（晏阳）
</h1>

<p align="center">Joban Client Mod 的晏阳技术组维护分支：为 Minecraft Transit Railway 4.0 带来<strong>换乘线路类型 API</strong>与<strong>C++ 原生脚本</strong>。</p>

<a align="center" href="https://weblate.ziyuesinicization.site/engage/joban-client-mod/"><img src="https://weblate.ziyuesinicization.site/widget/joban-client-mod/svg-badge.svg" alt="Translation status"></a>

> [!IMPORTANT]
> 本分支基于 [Joban Client Mod](https://github.com/lf285/JCM-1.20.x-Fabric)（by AmberFrost / lx862 及社区），新增了两大能力并持续修复问题：
>
> - **换乘线路类型 API** —— JS/Java/C++ 三端统一的 `getTransportType()` 与 `getTransportMode()`，可正确识别 [MAGIC MTR Addon](https://github.com/JustN00ne/magic-mtr) 注册的全部 vehicle 类型；
> - **C++ 原生脚本** —— 资源包可声明 `"language": "cpp"` 并附带预编译库，与 JS 脚本共存于同一条 capture-and-replay 渲染管线，除数据获取外全部逻辑在 C++ 侧原生完成。
>
> 详细 SDK 文档见 [`native/README.md`](native/README.md)。

## ✨ Yanyang.11 更新亮点

### 1. 兼容 MAGIC MTR Addon 的 vehicle 类型

两处 `getTransportType()` 此前只能返回 MTR 原版的 4 种基础类型，无法识别 MAGIC MTR Addon 注册的线路类型。现在（ABI 9）：

| 脚本端 | API | 返回值 |
| --- | --- | --- |
| JS / Java | `vehicle.getTransportType()` | 基础类型 `train` / `boat` / `cable_car` / `airplane`，或 MAGIC 类型 `train_metro`、`train_light_rail`、`train_high_speed`、`train_bus`、`train_tram`、`train_sbahn`、`train_normal` |
| JS / Java | `stop.routeInterchanges[i].getTransportType()` | 同上（换乘线路类型） |
| C++ | `train.getTransportType()` / `stop.interchange(i).getTransportType()` | 同上（原生读取，无 Java 回调） |

- 通过**可选反射**集成 MAGIC MTR Addon，**没有硬依赖**——未安装 MAGIC 时一切照旧；
- 线路 `routeId` + `transportType` 随服务端 → 客户端脚本数据集同步，新旧版本服混用也能拿到类型；
- `getTransportMode()` 语义不变，仍返回 MTR 的 `TransportMode`。

### 2. 消除 C++ 脚本每 500ms 的 GC 卡顿

卡顿根因是脚本 tick 循环里每帧制造海量 Java 临时对象（每车每帧数百个 Stop / RouteInterchange / 列表分配 + 一次 256 KB 快照拷贝 + 5 个 arena `ByteBuffer`），持续触发 Minor GC。修复后：

- **StopsData 图跨帧复用**：按「同步数据集身份 + 数据集修订号」缓存，完整数据图不再每帧重建（这是最大的单点垃圾来源）；
- **快照序列化零分配化**：每车复用 Builder——持久字符串池、出口 memo、只增不减的偏移数组；
- **JNI 边界收窄**：移除每车每帧 256 KB 快照 memcpy；5 个 arena `ByteBuffer` 改为每模块缓存的 GlobalRef 视图，每帧仅 rewind；显式快照长度取代切片；
- **数据获取 1s TTL + 路线签名缓存**限制站点换乘映射重建频率；模块列表遍历不再复制；
- **内存占用下降**：每模块像素 arena 由固定 16 MB 改为 1 MB 起步、按需增长。

稳态下 C++ 车辆脚本的每帧分配只剩一次轻量包装构造——**跨语言往返、快照拷贝、站点图重建全部消失**。

### 3. ABI 9 与旧模块兼容

ABI 9 在 `JcmVehicleSnapshot` / `JcmInterchange` 末尾追加了类型字段（布局见 [`native/include/mtr/mtr_native.h`](native/include/mtr/mtr_native.h)）；ABI 6/7/8 模块继续按各自原始布局加载（按 ABI 分组的快照，不影响老资源包）。

## 📦 下载与安装

每个 Release 提供 **12 个 jar**（6 个 MC 版本 × Fabric/Forge 双加载器），均已内置 4 个桌面平台的 JNI bridge，无需手动安装 DLL：

| Minecraft | Fabric | Forge |
| --- | --- | --- |
| 1.20.4 | ✅ | ✅ |
| 1.20.1 | ✅ | ✅ |
| 1.19.4 | ✅ | ✅ |
| 1.19.2 | ✅ | ✅ |
| 1.18.2 | ✅ | ✅ |
| 1.17.1 | ✅ | ✅ |

**前置**：[Minecraft Transit Railway 4.0.5](https://modrinth.com/mod/minecraft-transit-railway) + Fabric API（Fabric 端）。

> 想开发 C++ 脚本？下载 Release 附带的 **`jcm-native-sdk-<版本>.zip`**（含 SDK 头文件、构建脚本、三个示例模块的四个平台预编译库），并阅读 [`native/README.md`](native/README.md)。

## 🔧 C++ 脚本架构与跨平台

脚本核心遵循一条简单规则：**「API 只用于取数据，其余任务原生完成」**——

- 数据获取（列车、站点、换乘、资源、输入）经由 JNI 桥接进入 C++ 快照，每帧一次；
- 渲染、状态机、图形命令记录、纹理像素处理全部留在 C++ 侧，**不产生 Java 对象分配**，因此不参与 JVM GC——这正是 yanyang.11 卡顿修复的架构基础；
- 图形命令记录进 frame arena，由宿主在 Java 端一次性回放。

可移植性：

| 平台 | 支持方式 |
| --- | --- |
| Windows x64 / Linux x64 / macOS x64 / macOS ARM64 | CMake 一键构建，bridge 已随 jar 内置（`assets/jcm/natives/<platform>/`） |
| Android | 脚本核心 + JNI bridge 可用 NDK 编译（JNI 可用） |
| iOS / iPadOS | 脚本核心不依赖 JVM：渲染与逻辑全在 C++，数据获取由宿主注入，可移植至无 JVM 环境 |

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

资源包 `manifest.json` 中声明 `"language": "cpp"` 并放入对应平台的预编译库即可——JS 与 C++ 脚本可在同一资源包中并存。

## 📋 完整更新日志

见 [CHANGELOG.md](CHANGELOG.md)。各版本发布说明见 [Releases](https://github.com/BugQuantumCore/Joban-Client-Mod-Yanyang/releases)。

## 🤝 致谢

- 上游 [Joban Client Mod](https://github.com/lf285/JCM-1.20.x-Fabric) 及其全体贡献者
- [Minecraft Transit Railway](https://github.com/Jonafanho/Minecraft-Transit-Railway)（MTR）
- [MAGIC MTR Addon](https://github.com/JustN00ne/magic-mtr)（JustN00ne）
- 晏阳技术组（Yanyang Technical Group）

## License

This project is licensed under the MIT License.
