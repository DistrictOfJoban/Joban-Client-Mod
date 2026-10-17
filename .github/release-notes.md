JCM Yanyang `2.3.0-beta-2.yanyang.2` — `minecraft_custom_resources.json` 脚本条目不再需要 `language` 字段：`scriptLocations` 逐条目自动判定语言，同一条目可混用任意数量的 JavaScript 与 C++ 脚本。

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
