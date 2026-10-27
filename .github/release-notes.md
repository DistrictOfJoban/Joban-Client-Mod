JCM Yanyang `2.3.0-beta-2.yanyang.8` — 脚本化 PIDS 渲染修复、开门方向服务端持久记忆、原生桥扩至 8 平台。

**PIDS 文字渲染修复（yanyang.4 回归）**

脚本化 PIDS 预设（内置 `jsblock:pids_1a` 及任何自定义 JS / C++ PIDS 预设）的文字与贴图此前按方块原始尺度渲染——文字约放大 96 倍、锚在方块原点，`.matrices()` 模型姿态丢失。现逐绘制调用捕获变换快照（回收式对象池，稳态零分配保持），缩放与定位恢复正确；JS 装饰物 / 车辆脚本的同源绘制一并修复。JSON 预设 PIDS（`rv_pids` / `lcd_pids` / 投影 PIDS 等）与车辆 LCD 不受影响。新增跨平台回归测试 `scripted_render_semantics` 防止再次回归。

**开门方向：服务端持久记忆 + `getDirectionOfDoorOpening()`**

- 新脚本 API（JS / C++ 一致）：返回 `"not"` / `"left"` / `"right"` / `"all"`——当前站停靠中（与车门开闭阶段无关）或正线运行时预测**下一站**开门侧；左右相对车头、不受倒行影响；纯查询，不改变 MTR 开门行为。
- 服务端安装本模组时，预测由**服务端逐线路持久记忆**作答：首次启动全档扫描（未加载区块按需载入——客户端物理上做不到），站台增删改（含 WorldEdit / Axiom 批量编辑）增量重扫、拆除重建自动收敛，结果持久化在存档内——**跨视距、跨会话生效**。服务端未装时自动回退客户端视距内扫描。
- 原生 C ABI 升至 11：车辆快照新增 `door_direction` 字段（ABI 9/10 旧模块照常工作）；SDK 车辆包装类更名 `mtr::Vehicle`（`mtr::Train` 保留为别名）。

**原生桥扩至 8 平台：windows-arm64**

新增 windows-arm64 原生桥，并修复捆绑桥搜索在 ARM64 Windows 上错误指向 x64 DLL 的问题（x64 DLL 无法在 ARM64 JVM 进程内加载，此前该平台永远加载不到桥）。资源包 C++ 模块需提供 `natives/windows-arm64/` 副本方可在此平台运行。

**全模块执行与性能审计（20+ 项修复）**

JS 捕获 / 回放线程竞争（可致渲染崩溃）、ARM64 结果槽内存序（JVM 崩溃向量）、Forge 端原生车辆视锥剔除从未生效、每部件每车厢全树重建、陈旧桥 15 秒冻结、字形掩码内存无上限、批量实例销毁单 tick 倾泻、门向扫描 O(P²) 等——完整清单见仓库 [CHANGELOG.md](https://github.com/BugQuantumCore/Joban-Client-Mod-Yanyang/blob/main/CHANGELOG.md) 的 yanyang.8 章节。

> Modrinth 用户提示：自 yanyang.4 跳版升级时，yanyang.5–.7 的修复（传输平面自愈、列车 LCD 黑屏、间歇性卡顿）已包含在本版中。

**交付物**：12 个 jar（6 个 MC 版本 × Fabric / Forge，均内置 8 平台原生桥）+ native SDK zip。
