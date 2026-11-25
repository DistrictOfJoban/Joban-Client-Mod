JCM Yanyang **2.3.0-1.0.1-hotfix.3**，一轮全库代码审查带来的稳定性加固：畸形网络包不再断开客户端，脚本引擎错误不再逃逸冷却，多项 GUI 边界崩溃与静默失效修复。

## 修复内容

- **网络容错（版本不匹配防护）**：服务端与客户端版本不一致（升级期间的真实场景）或数据包损坏时，未知的屏蔽门脚本类型（`PSDType`）与问询屏类型（`EnquiryScreenType`）此前抛 `IllegalArgumentException` 直接断开客户端。现在未知脚本类型跳过该条绑定并记录警告（其余绑定正常应用），未知问询屏类型回退到经典样式（CLASSIC）；负的门方向数量同样按畸形包拒绝，不再以裸 `NegativeArraySizeException` 崩出。
- **脚本引擎加固**：循环 `include()`（脚本直接或间接包含自身）此前递归直到 `StackOverflowError`——它是 `Error` 而非 `Exception`，逃逸了全部 `catch (Exception)` 并杀死脚本任务线程。现在 include 递归达 32 层即快速失败并给出明确错误；脚本执行改捕 `Throwable` 且 4 秒失败冷却移入 `finally`，此类错误不再绕过冷却导致每帧重试。
- **GUI 与渲染**：跑马灯（MARQUEE）溢出文本此前静默不绘制（空实现）——现在降级为普通裁剪文本，内容可见；未设置字体的文本条目在缓存比较时不再抛空指针；根级资源路径（如 `mtr:script.js`）的相对路径解析不再空指针崩溃。
- **脚本错误通知**：逐条隔离执行——单条通知抛异常不再阻塞其余通知；玩家在错误排队后离开世界不再崩溃。
- **性能**：客户端没有任何屏蔽门脚本绑定时，玻璃 / 顶板分组扫描完全跳过世界方块遍历。
- **构建可复现性**：钉住全部动态 `+` 依赖版本（Fabric Loom 1.10.5、Mixin 0.7.38、foojay 1.0.0、Manifold 2026.1.16）；Forge 端补齐与 Fabric 一致的完整 Manifold 注解处理器集。六个 Minecraft 版本 × Fabric / Forge 全部以 JDK 25 工具链构建，native 全套件本地 20/20 绿。

## 升级说明

自 2.3.0-1.0.1、hotfix.1 或 hotfix.2 直接覆盖安装即可，无世界数据与配置变更。

## 下载与兼容

提供 **12 个 jar**：Minecraft **1.17.1、1.18.2、1.19.2、1.19.4、1.20.1、1.20.4**，各含 Fabric / Forge 版本；另附 **native SDK zip**。

每个 jar 内置八个平台的原生桥：Windows、Linux、macOS 的 x64 / ARM64，以及 Android ARM64、iOS ARM64。C++ 资源包需提供对应平台的脚本模块。

依赖 **MTR 4.0.5 ≤ 版本 < 4.1**；Fabric 版另需 Fabric API。与原版 JCM 共用 `jsblock`，**只能安装其中一个**。实际画面仍受资源包、光影及运行环境影响。

[开发文档](https://bugquantumcore.github.io/Joban-Client-Mod-Yanyang/) · [完整更新日志](https://github.com/BugQuantumCore/Joban-Client-Mod-Yanyang/blob/main/CHANGELOG.md)
