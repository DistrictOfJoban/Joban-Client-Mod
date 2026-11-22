JCM Yanyang **2.3.0-1.0.1-hotfix.2**，修复右键装饰物空指针崩溃。

## 修复内容

- 修复右键点击缺少 `model_id` NBT 标签的装饰物方块时客户端崩溃的问题（`NullPointerException`：`NativeScriptManager.isLoaded` → `ConcurrentHashMap.containsKey(null)`）。这类装饰物通常由旧版本 JCM 落地，其方块实体数据中没有模型 ID，脚本入口被解析为 null 后直接传入并发字典查找而崩溃。
- 脚本状态查询 `isLoaded` / `isSkipped` / `getModules` 全部对 null 脚本 ID 安全返回；`blockUse` / `shape` 对无标签装饰物提前返回，行为等同未绑定脚本。Fabric 与 Forge 同修，六个 Minecraft 版本同步发布。

## 升级说明

自 2.3.0-1.0.1 或 hotfix.1 直接覆盖安装即可，无世界数据与配置变更。

## 下载与兼容

提供 **12 个 jar**：Minecraft **1.17.1、1.18.2、1.19.2、1.19.4、1.20.1、1.20.4**，各含 Fabric / Forge 版本；另附 **native SDK zip**。

每个 jar 内置八个平台的原生桥：Windows、Linux、macOS 的 x64 / ARM64，以及 Android ARM64、iOS ARM64。C++ 资源包需提供对应平台的脚本模块。

依赖 **MTR 4.0.5 ≤ 版本 < 4.1**；Fabric 版另需 Fabric API。与原版 JCM 共用 `jsblock`，**只能安装其中一个**。实际画面仍受资源包、光影及运行环境影响。

[开发文档](https://bugquantumcore.github.io/Joban-Client-Mod-Yanyang/) · [完整更新日志](https://github.com/BugQuantumCore/Joban-Client-Mod-Yanyang/blob/main/CHANGELOG.md)
