# 更新日志 / Changelog

## 未发布

- 新增脚本 API：`vehicle.getDirectionOfDoorOpening()`（JS 与 C++ 均可调用，返回 `"not"` / `"left"` / `"right"` / `"all"`），识别车辆在当前站 / 下一站的开门方向。停靠站台时，无论车门处于未开 / 半开 / 关闭中 / 已关的哪个阶段，返回值都与车门完全打开时一致；正线运行时预测下一站的开门情况（把车辆按停车点摆放后，按 MTR `RenderVehicleHelper#canOpenDoors` 同款方块扫描逐车厢检测）；同侧任一车门可开即整侧记为可开。左右相对车头（bogie-1 端），不受行进方向 / 倒行影响。纯查询语义——不改变 MTR 实际开门行为。
- 原生 C ABI 升至 11：车辆快照原 `_pad0` 字节升格为 `door_direction`（0..3，对应上述四个值），布局与尺寸不变；ABI 9/10 模块照常工作，`mtr::Vehicle` 新增 `doorDirection()`（枚举）与 `getDirectionOfDoorOpening()`（字符串）访问器及 `mtr::door_direction` 常量。
- SDK 术语对齐 MTR 4：车辆快照包装类由 `mtr::Train` 更名为 `mtr::Vehicle`（与 `VehicleWrapper` / `vehicleScripts` 命名一致）；`mtr::Train` 保留为源码兼容别名，老模块无需修改即可继续编译。C ABI 与二进制布局不变。
- CI 修复：Complete Build 的 1.18.2 矩阵项在「Build both loaders with all seven JNI bridges」步骤因 `maven.minecraftforge.net` 下载 `lwjgl-openal-3.2.2-natives-linux.jar` 读取超时而偶发失败（网络瞬时故障，与代码无关）；build / Complete Build / Release 三条流水线的 gradle 步骤现自动重试至多 3 次。
- JNI 术语残留清理（桥自 layout v2 起已完全 JDK-free，本次仅命名与注释）：`native/jni/jni_bridge.cpp` 更名 `native/bridge/bridge.cpp`；workflow 步骤名「seven JNI bridges」→「seven native bridges」、「real JVM JNI integration」→「real JVM integration」；移除从未被 CMake 消费的 `-DJCM_JNI_INCLUDE_DIR` 旗标与配套 jni.h 注释；修正若干过时注释（jvm_test.py 文档串、CMake 与 bench/头文件及 Java 侧若干「the bridge is a JNI library」类表述）；`build.gradle` 旧桥报错不再指向仓库中不存在的 `bridge/README.md` / `repair-api/build-bridges.ps1`。二进制与行为零变化。

## 2.3.0-beta-2.yanyang.7

修复 yanyang.6 之后的间歇性卡顿（日志无报错）。旧清扫把「离开视野」误当成「已销毁」：列车离开相机视锥 5 秒后，原生实例即被整体释放（十余张 GL 纹理、全部已上传模型、脚本画布）；重新入镜的第一帧一次性支付 `mtrCreate` + `createResources` + 整幅画布重绘，转头一次即触发多列车同时重建，形成多毫秒级掉帧。

- 车辆清扫区分「存在」与「可见」：`render()` 对客户端已知的每辆车每帧打 `seen` 标记（剔除与否都会执行），仅当车辆真正离开客户端车辆列表 5 秒后才销毁；镜头后的列车实例保持热态，重新入镜零成本（与 JS 车辆脚本行为一致）。
- 空闲阀门 `-Djcm.nativeIdleMillis`（默认 90000）：仍存在但长期未渲染的实例超出时限后才释放；`0` = 不按时限释放。
- 热实例上限 `-Djcm.nativeIdleMax`（默认 32）：超出后按最久未渲染者优先释放；`0` = 不设上限。
- 每 tick 销毁预算 2 个（旧者先）；8 个以上的批量退出（世界卸载 / 切维度 / 断线）仍一次性冲刷。PIDS / 装饰 / 电梯清扫套用同一阀门，显式移除标志仍立即销毁。
- 清扫工作以每分钟一行的聚合日志输出，销毁流量可直接在 `latest.log` 观测。

## 2.3.0-beta-2.yanyang.6

修复列车 LCD 永久黑屏（数据正常、无报错、不自愈）。列车离开视锥 5 秒后实例被销毁，但 Java 侧实例 id 缓存未同步——重新入镜后每帧携带已被桥遗忘的 id，桥返回 `ERR_INSTANCE(4)`，脚本此后永远返回空帧。

- `disposeInstance` / `disposeVehicleInstance` 在同一临界区删除 id 缓存，下次渲染自动重新注册并按首次渲染重建实例。
- 防御深度：返回 status 4 的渲染在帧内清除自身缓存 id，下一帧即重新注册。
- 「重载全部 MTR 脚本」快捷键此前从不重置 C++ 运行时，现与 F3+T 走同一条卸载 + 重新发现路径。
- 顺带修复：JDK 22+ 的共享内存 FFM/VarHandle 访问器因接口非 public 从未真正安装（一直静默回退 `sun.misc.Unsafe`）；channel-ready 日志改为报告实际安装的访问器；`INSTANCE_IDS` 改为 `ConcurrentHashMap`。
- 回归：`native_jvm_integration` 两平面新增「渲染 → 销毁 → 再渲染」场景（修复前该检查可逐字复现游戏内 status-4 日志）。

## 2.3.0-beta-2.yanyang.5

两个无 JNI 传输平面的健壮性与优化审查（shm 协议 layout v2 不变），修复 9 处真实缺陷：

- poller 线程不再被单次异常永久杀死：服务按请求隔离，异常立即回传错误结果。
- 被放弃区域自愈：`HDR_MODE` 毒化位 + 空闲路径活性复检，卡死的 SVC 往返毫秒级解开，poller 自动脱离死区域并重新扫描握手。
- CBQ 回调环改用按（线程, 槽）保留的缓冲，消除超时后 Cleaner munmap 与 poller 竞争导致整 JVM SIGSEGV 的可能，同时省去每次代理调用两次直接缓冲分配。
- 模块控制的回复长度全路径钳制（CBQ 负长度发布 0；FFM api 回复上限 16 MB）。
- 车辆销毁 / 模块关闭时的传输故障不再逃逸进游戏 tick 路径；`host_invoke_api` 无宿主回退补空指针保护。
- 优化：`jcm_module_render` 实例键改用按线程复用缓冲（不再每帧每模块 malloc）；Reclaimer 合并锁作用域。
- 验证：FFM 平面 70 项 / poller 平面 67 项 JVM 检查全绿；poller 套件新增端到端「放弃 + 自愈」场景；`-Wall -Wextra` 干净。

## 2.3.0-beta-2.yanyang.4-shmtest5

模组全面去 JNI：所有 Java↔C++ 传输在受支持 JVM（16–22+）上无 JNI 运行。

- 新增共享内存 poller 平面（JDK 16..21）：桥仅被 `System.load` 以启动构造器 / poller 线程，经文件系统握手指针文件发现区域；模块生命周期命令、每帧渲染、SVC 宿主服务与 CBQ api 再入全部经共享页面服务，槽位按嵌套深度 / 轮转分配防重入死锁，关闭命令毒化区域头防陈旧映射重连。
- Panama FFM 平面（JDK 22+）行为不变。
- 修复配置界面「Java 与 C++ 通信方式」无法修改：按钮此前未注册为屏幕子控件（列表组件只绘制不派发点击），现可点击、循环并保存；旧 `JNI` 配置值仅为文件兼容保留，重标为「直连调用 / Direct Calls」。
- `jni_bridge.cpp` 彻底 JDK-free（无 `jni.h`）：全部 JNI 导出、`JNI_OnLoad`、宿主机缓存、DirectByteBuffer 包装与 `nativeApiCallback` 移除，两平面共用 `serve_request` 核心，served-seq CAS 保证恰好一次。SDK 模块（ABI 6–10）不受影响。

## 2.3.0-beta-2.yanyang.4

脚本渲染管线（JS + C++）GC 与稳态分配整治，捕获 → 回放稳态分配降至约零：

- 变换隔离零拷贝（`StoredMatrixTransformationsMixin`，双加载器）：mark / append / truncate 取代每绘制调用的 `.copy()`。
- 原生绘制调用（模型 / 文字 / 纹理包装器）经自由列表复用；捕获列表交换引用（`takeFrom`）取代逐帧 `copy()`；空声音帧零分配。
- 矩阵捕获时拷入实例私有可增长存储，取代每记录 `float[count][16]` + lambda + 新变换对象（6 节 10 记录列车 ≈ 460 KB/s 短命数组）；渲染线程 JOML 草稿矩阵复用。
- 原生字符串 / `Identifier` 驻留表、跑马灯字形缓存、`PIDSDrawCall` 持久 consumer。
- 顺带修复：跨车辆 arena 矩阵别名（捕获时拷入实例私有存储）、`RenderEyeCandyMixin` WeakHashMap 自引用泄漏、跑马灯缓存按内容 + 样式键控。

## 2.3.0-beta-2.yanyang.3

全代码库审查（Java + C++ 桥 / SDK），原生桥扩展至 7 平台：

- 新增捆绑桥：`linux-arm64`、`android-arm64`（NDK，16 KB 页对齐）、`ios-arm64`（Mach-O，iOS/iPadOS 共用）。
- 平台键修复：此前所有 Linux/Windows 一律判为 `-x64`，ARM64 Linux 永远解析到错误目录；现按 `os.arch` 推导，ARM 变体间自动回退链。
- 严重修复：F3+T 从不重载 C++ 脚本（`reload()` 死代码 + 渲染线程 use-after-free）；桥对模块提供的 `record_count` / `record_size` 全面边界校验；单条畸形 MTR 4 条目不再中断整包解析；`Text` / `Texture` 默认值统一（未设字段不再全透明 / 零尺寸）；Forge 车辆脚本只捕获不绘制、Forge 轨道剔除条件反置（两处上游同样存在）；畸形记录不再杀死渲染线程（进入 4 秒退避）。
- 性能：每车每帧一棵共享位置树（原每帧 2N+1 棵全树重建）；视锥盒只为带原生脚本的车辆构建；`traceOnce` 零分配；失败加载负缓存；记忆泄漏修剪。
- 加固：`host_acquire/release_model` 判空；SDK 越界索引全部 bounds-check（一处 off-by-one 曾 SIGSEGV 整个 JVM）；GCC/Clang 模块构建启用 `-Wall -Wextra`。

## 2.3.0-beta-2.yanyang.2

脚本条目免声明 `language`：`scriptLocations` 由模组自行分类（`*.js` = JavaScript，其余 = C++ 模块），同一条目可任意混用两种语言，车辆 / 电梯 / 装饰 / PIDS 全部支持；PIDS 预设同键。两侧各自捕获回放互不冲突，失败侧优雅降级；`language: "cpp" / "js"` 保留为强制语义开关，旧键全兼容。

## 2.3.0-beta-2.yanyang.1

上游 `2.3.0-beta.2` 合入：轨道预览缓存、Shift 独占多选、车辆 / 电梯 / 轨道实验性视锥剔除（`enableFrustumCulling*`）、F5 潜行降低相机（`allowCameraLowering`）；脚本 API 新增 `getCameraMode` / `lightAt` / `drawModel` 重载、车门与电梯门可开性查询。Yanyang 侧：`isScriptRendered` / `isScriptControlled` 双字段名兼容（旧资源包不受影响）、电梯门可开性惰性计算、C++ 电梯脚本与 JS 同语义骑乘玩家。

## 2.3.0-beta.1-yanyang.11

- 新增 `getTransportType()`（JS / Java / C++ ABI 9）：返回基础交通类型与 MAGIC MTR Addon 车辆类型（反射可选集成，无硬依赖）。
- 修复 C++ 脚本 GC 卡顿：StopsData 跨帧复用、零分配快照 Builder、去除 JNI 桥的 256 KB 快照拷贝；像素 arena 改为 1 MB 起步按需增长（原每模块固定 16 MB）。
- ABI 6 / 7 / 8 模块与 ABI 9 并存。

## 2.3.0-beta.1-yanyang.10

- 修复游戏内 `NoSuchMethodError`：全部捆绑桥由当前源码重建；新增 `checkNativeBridges` 构建任务，桥与 jar 类声明不符时直接构建失败。
- 原生车辆脚本视锥剔除：视锥外列车不再支付快照封送、画布重绘与纹理上传。
- 渲染线程分配优化（字符串池返回 packed long、 exits 备忘、`IntStream` 改复用缓冲循环）。

## 2.3.0-beta.1-yanyang.9

- 原生纹理像素直接从帧 arena 拷入既有图像存储，去除大临时数组与逐字节颜色转换；保留局部矩形裁剪、ARGB、上传顺序与旧兼容。
- 车辆设置、原生执行、帧回放与 JVM 回收计数每分钟报告一次，用于诊断渲染线程停顿。

## 2.3.0-beta.1-yanyang.8

- 换乘交通方式按 MTR 简化站台数据同规则的路名归一化解析（全量与邻近数据均适用）；缺失或冲突时保持未知。

## 2.3.0-beta.1-yanyang.7

- 新增 `RouteInterchange.getTransportMode()`，类型跨服务端数据与原生快照保持一致。
- C++ PIDS / 电梯 / 装饰脚本接入真实渲染入口；C++ 可直连共享 Java 脚本 API（资源、模型、图形、声音、Minecraft、文件、网络、回调）。
- ABI 8 引入并保留 ABI 6 / 7 兼容；按实例在移除 / 重载时释放资源，缓存 JNI 方法 / 类绑定。
