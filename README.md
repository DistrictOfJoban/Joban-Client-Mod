<p align="center">
  <img src="assets/JCM_logo.png" width="180" alt="JCM Logo">
</p>

<h1 align="center">Joban Client Mod — Yanyang（晏阳）</h1>

<p align="center">Joban Client Mod 的晏阳技术组维护分支，为 Minecraft Transit Railway 4.0 提供 JS / C++ 混合脚本、屏蔽门信息显示与运行时改进。</p>

<p align="center">
  <a href="https://github.com/BugQuantumCore/Joban-Client-Mod-Yanyang/releases"><img src="https://img.shields.io/github/v/release/BugQuantumCore/Joban-Client-Mod-Yanyang" alt="Latest release"></a>
  <a href="https://bugquantumcore.github.io/Joban-Client-Mod-Yanyang/"><img src="https://img.shields.io/badge/docs-scripting-teal" alt="Scripting docs"></a>
  <a href="https://weblate.ziyuesinicization.site/engage/joban-client-mod/"><img src="https://weblate.ziyuesinicization.site/widget/joban-client-mod/svg-badge.svg" alt="Translation status"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-green" alt="MIT License"></a>
</p>

## 主要功能

- **五类混合脚本**：车辆、PIDS、电梯、装饰物、屏蔽门均支持 JavaScript 与 C++，可在同一条目中混用。
- **屏蔽门脚本**：通过游戏内编辑器为玻璃和顶部面板绑定专用脚本或脚本化 PIDS 预设，支持连续面板画布、服务端保存与同步，以及车站、线路、到站和门状态数据。
- **原生运行时**：提供共享内存与 FFM 传输方式，正式发行包内置八个平台的原生桥；C++ 资源包仍需提供对应平台的脚本模块。
- **线路数据扩展**：服务端开门方向记忆、换乘线路类型 API，以及脚本生命周期、渲染和缓存管理改进。

## 下载

[GitHub Releases](https://github.com/BugQuantumCore/Joban-Client-Mod-Yanyang/releases) · [Modrinth](https://modrinth.com/project/kMMJcRqY) · [CurseForge](https://www.curseforge.com/projects/1736483)

正式版提供 **12 个 jar**（六个 Minecraft 版本 × Fabric / Forge）及 `jcm-native-sdk-<版本>.zip`。请选择与游戏版本和加载器一致的 jar。

## 兼容环境

- **Minecraft**：1.17.1、1.18.2、1.19.2、1.19.4、1.20.1、1.20.4。
- **依赖**：MTR **4.0.5 ≤ 版本 < 4.1**；Fabric 版另需 Fabric API。Java 版本应满足对应 Minecraft 与加载器的要求；FFM 方式需要 JDK 22 或更新版本。
- **原生桥**：Windows / Linux / macOS 的 x64 与 ARM64，以及 Android ARM64、iOS ARM64。
- **安装互斥**：本分支与原版 JCM 使用同一 mod id（`jsblock`），**只能安装其中一个**。

## 文档

- [晏阳脚本开发文档](https://bugquantumcore.github.io/Joban-Client-Mod-Yanyang/)（七种语言）与 [C++ SDK](native/README.md)
- [原版 JCM 文档](https://jcm.joban.org/v2.3/) · [更新日志](CHANGELOG.md) · [参与翻译](https://weblate.ziyuesinicization.site/engage/joban-client-mod/)
- 网站源文件位于 [Develop-Documentation-Website 分支](https://github.com/BugQuantumCore/Joban-Client-Mod-Yanyang/tree/Develop-Documentation-Website)。

## 上游与许可

基于 [Joban Client Mod v2.3.0](https://github.com/DistrictOfJoban/Joban-Client-Mod)，感谢 AmberFrost 及全体上游贡献者、[Minecraft Transit Railway](https://github.com/Jonafanho/Minecraft-Transit-Railway)（jonafanho）与 [MAGIC MTR Addon](https://github.com/JustN00ne/magic-mtr)（JustNoone）。本分支由晏阳技术组维护，使用 [MIT License](LICENSE)。
