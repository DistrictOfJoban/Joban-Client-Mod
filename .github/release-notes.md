JCM Yanyang `2.3.0-beta-2.yanyang.3` — 全代码库 bug 与性能审计（yanyang.3）：7 项关键修复 + 30 余项正确性/性能/加固修复。本 Release 同时包含 yanyang.1（原版 2.3.0-beta.2 整合）与 yanyang.2（`scriptLocations` 自动语言判定与 JS + C++ 混合条目）。

**关键修复 / Critical fixes (yanyang.3)**

- F3+T 资源重载从未真正重载 C++ 脚本：`NativeScriptManager.reload()` 是死代码——更新后的 `.so`/`.dll` 被静默忽略，且重载后 `NativeHost` 继续引用已被 `ModelManagerJS.reset()` 释放的模型（渲染线程 use-after-free）。现在重载会先释放全部模块再重新解析。
- 模组记录流越界可崩溃整个 JVM：桥接层 `patch_frame_handles` 无边界校验地遍历模块提供的 `record_count`/`record_size`（在模块崩溃保护之外执行 memmove）。现已逐步校验。
- 单个损坏的 MTR 4 脚本条目会中止整个资源包的解析（vehicle/eyecandy/lift 三类全部受牵连）；`NativeScriptManager.load()` 遇到非法库标识符（大写路径等）直接逃逸。现逐条目隔离并跳过。
- C++ `Text`/`Texture` 未设置的字段被清零覆盖：未设 `color` 完全透明、未设 `scale` 尺寸为零、未设 `uv` 退化、未设 `align` 居中——官方示例 `pids_arrivals.cpp` 即受影响。默认值现由单一来源提供。
- Forge 端原生车辆脚本只捕获不渲染（`VehicleResourceMixin` 从未接入原生回放）；Forge 端轨道裁剪逻辑反转（限内轨道永远渲染、从不遮挡剔除——原版上游同样存在）。
- 阴影 pass（Iris/OptiFine）下车辆/电梯脚本每帧被驱动两次；骑乘中被视锥裁剪的电梯会冻结玩家；`mtrDispose` 永远拿到实例首帧的快照。
- C++ SDK 访问器（`Stop::interchange/exit`、`Exit::destination`、`StopList::at`、`Train::car`、`Train::rail_progress`）全部补齐越界检查——脚本差一即越界读取快照 direct-ByteBuffer 并 SIGSEGV 整个 JVM。

**性能 / Performance (yanyang.3)**：每车每帧一棵共享车厢位置树（原先每车 2N+1 棵，12 节列车 ≈ 每秒 1500 次全树重建）、仅对持有原生脚本的车辆计算视锥盒、零分配一次性追踪、`rasterizeText` 直读 `int[]`、gfx2d 复用草稿缓冲、泄漏修剪（`SCRIPT_IDS`/`TRACED`/`CAR_POSITIONS`）、失败加载负缓存、稳定临时文件名、`clearDeadInstance` 去快照化；font5x7 补齐小写 a-z（原先小写渲染为空白）。

---

**yanyang.2 — `minecraft_custom_resources.json` 脚本条目不再需要 `language` 字段：`scriptLocations` 逐条目自动判定语言，同一条目可混用任意数量的 JavaScript 与 C++ 脚本。

**新版声明格式 / New declaration format**

```json
"vehicleScripts": [
    {
        "id": "cst_sp1900_lcd",
        "prependExpressions": ["print('Hello world')"],
        "scriptLocations": ["mtr:my_pack/js/sp1900_lcd/main.js", "mtr:my_pack/natives/sp1900_train_num"],
        "input": { "yearEra": 2004 },
        "dataFetchMode": "ALL"
    }
]
```

- `*.js`（大小写不敏感）→ JavaScript；无扩展名 → C++ 平台无关 stem（`natives/{platform}/lib*.so|*.dll|lib*.dylib` 自动解析）；`.so/.dll/.dylib` 完整路径与按平台映射表亦可用。
- **vehicle / lift / eyecandy / pids 四类脚本全部支持混合**：JS 部分连接成单一 ParsedScript，C++ 模块全部挂到同一脚本 id；两侧各自捕获、各自回放，互不冲突；某一侧缺失或失败只跳过该侧（附提示），另一侧照常渲染。
- PIDS 预设（`joban_custom_resources.json`）新增同样的 `scriptLocations` 键，与老的 `scriptFiles` 并存。
- 老格式完全兼容：`"language": "cpp"` + `nativeLibrary`/`nativeLibraries` 仍是纯 C++ 条目（也可与 `.js` 混用）；`"language": "javascript"` 强制全 JS；MTR 3 `script_files` 语义不变。
- 混合电梯条目下内置渲染只抑制一次、乘坐移动每帧只应用一次；eyecandy 方块使用事件同时送达 JS 与 C++。

**En (summary)**: script entries no longer declare `language` — each `scriptLocations` entry is auto-classified (`*.js` = JavaScript, anything else = a C++ module) and any number of both may be mixed in one entry. Vehicles, lifts, eyecandies and PIDS all support it; each side captures into its own replay registry (no draw-call conflicts) and a failed part degrades gracefully. Legacy `language: "cpp"` / `nativeLibrary` declarations keep working and may be combined with `.js` entries. PIDS presets gain the same `scriptLocations` key.

**翻译对齐原版 / Translations aligned with upstream**

- 语言文件与原版 JCM（同样由 BugQuantumCore 维护翻译）逐键对齐、原版优先；本版采纳原版 `main` 的爱尔兰语（ga_ie）占位与捷克语新键。

**保留的全部晏阳能力 / All Yanyang features retained**

- `getTransportType()` 返回 MAGIC MTR Addon 车辆类型 + 4 种基础类型（JS / Java / C++，ABI 9；可选反射集成，无硬依赖）。
- 原生脚本零 GC 卡顿：StopsData 跨帧复用、零分配快照 Builder、JNI 边界收窄（无 256 KB 拷贝）、1 MB 按需像素 arena。
- 原生车辆视锥剔除（`VehicleCulling`）与原版 beta.2 的 MTR 层剔除互补；ABI 6/7/8 模块与 ABI 9 并存。
- 12 jar（6 MC 版本 × Fabric/Forge，各 ~24.6 MB，内置 4 平台 bridge）+ native SDK zip，随本 Release 附带。
