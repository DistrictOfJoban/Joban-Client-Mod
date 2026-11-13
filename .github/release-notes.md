JCM Yanyang **2.3.0-1.0.0 正式版**，基于上游 JCM v2.3.0。

## 主要更新

- 新增屏蔽门 JS / C++ 脚本：玻璃与顶部面板可绑定专用脚本或脚本化 PIDS 预设，支持连续面板画布、服务端保存与同步，以及车站、线路、到站和门状态数据。
- 修复屏蔽门朝向与上下半块分组、长段画布、门状态匹配、绑定同步和切换世界后的缓存问题；改进背板边界、渲染异常恢复与资源重载清理。
- 加固脚本资源生命周期、文件访问与原生桥边界校验，修复重复站台、终点信息及模型向量顶点坐标，并改善文档站点的导航、复制与移动端交互。

## 下载与兼容

提供 **12 个 jar**：Minecraft **1.17.1、1.18.2、1.19.2、1.19.4、1.20.1、1.20.4**，各含 Fabric / Forge 版本；另附 **native SDK zip**。

每个 jar 内置八个平台的原生桥：Windows、Linux、macOS 的 x64 / ARM64，以及 Android ARM64、iOS ARM64。C++ 资源包需提供对应平台的脚本模块。

依赖 **MTR 4.0.5 ≤ 版本 < 4.1**；Fabric 版另需 Fabric API。与原版 JCM 共用 `jsblock`，**只能安装其中一个**。实际画面仍受资源包、光影及运行环境影响。

[开发文档](https://bugquantumcore.github.io/Joban-Client-Mod-Yanyang/) · [完整更新日志](https://github.com/BugQuantumCore/Joban-Client-Mod-Yanyang/blob/main/CHANGELOG.md)
