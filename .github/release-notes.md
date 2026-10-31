JCM Yanyang `2.3.0-prerelease.2-yanyang.1-hotfix.1` — yanyang.1 定向热修：MTR 4 原生路线类别（高铁 / 轻轨）流入换乘类型链，原版高铁、轻轨线路正确从车侧线路图去除；Axiom 区块批量编辑纳入开门方向重扫；iOS 交叉编译腿回归 macos-latest（arm）runner。

**原版高铁 / 轻轨正确按类型隐藏（修复「线路图去不掉原版高铁、轻轨」）**

车侧 LCD 换乘过滤按类型串判定，但 MTR 4 路线编辑器的原生「类别」（普通 / 通勤 / 区域 / 城际 / 高铁 / 轻轨，随 `Route` 与 `VehicleExtraData` 逐车同步）此前从不参与类型解析——解析链只查 MAGIC 边车覆盖，而 MAGIC 下拉框选中高铁 / 轻轨时恰好清除边车、只留原生枚举，这些线路在数据快照里全部退化为基础 `train`，于是按「保留原版默认 Train」规格随普通地铁一起放行。修复（产出串与既有词汇表完全一致，脚本侧零改动）：

- 类型解析新增原生 `RouteType` 入参：MAGIC 边车覆盖仍优先（镜像其 UI 优先级），其次非 NORMAL 原生类别产出 `train_high_speed` / `train_light_rail` 等键（与 MTR 自家 `getRouteTypeKey()` 同格式），NORMAL 仍为基础模式 id——vanilla 默认 Train 行为不变。
- 三条数据链全部类型感知：服务端全量数据预解析（`MTRDatasetHolder.addStation`）、客户端有限数据拼装（`buildLimitedStopsData`，站内换乘两组 + 相连车站换乘）、车辆自身线路（`VehicleExtraData` 逐车同步的类别）；类型串随 JSON 同步，新服务端 + 旧客户端同样正确。
- MAGIC 未安装 / 未覆盖线路、pre-ABI-9 旧数据回退行为不变；`train_high_speed` / `train_light_rail` 本就在脚本侧隐藏集——车侧 LCD 线路图与开门换乘面板无需任何改动即恢复正确过滤。

**开门方向重扫补上 Axiom 区块批量编辑**

高度图追踪现在按改动块收敛 world / chunk / section 三级写入（Axiom set-block-buffer 批量放置的每个方块都会更新高度图），配合区块本地坐标、归属 chunk 路由与服务端线程守卫。手动编辑、WorldEdit、Mishang UC 摆放此前已经由 world-API 钩子覆盖，本次补齐批量写入的最后缺口——开门方向服务端记忆对区块编辑器的批量改动同样即时生效。

**构建与工程**

- iOS 交叉编译腿回归 `macos-latest`（arm）runner：撤除 2026-10-28 / 10-30 两日 arm 供给拥堵期间临时钉上的 `macos-15-intel`（release.yml 与 complete-build.yml 同步撤除）；iPhoneOS SDK 随 arm 宿主 Xcode 同样随附，cross 腿跳过 ctest 不变。
- model-upload 测试台架补 `JSUtil` stub（恢复前置依赖齐备后的全绿）。
- README 补记上游 prerelease.2 基线对照。

**交付物**：12 个 jar（6 个 MC 版本 × Fabric / Forge，均内置 8 平台原生桥）+ native SDK zip。mod id 仍为 `jsblock`（与原版 JCM 互斥），依赖 MTR 4.0.5+；fabric 版另需 Fabric API。
