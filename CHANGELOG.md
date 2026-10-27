# 更新日志 / Changelog

## 2.3.0-beta-2.yanyang.8

- 修复（yanyang.4 引入的回归，实机报告 `jsblock:pids_1a`）：**脚本化 PIDS 预设的文字与贴图按方块原始尺度渲染**——文字约放大 96 倍、锚在方块原点完全偏离，`.matrices()` 模型姿态全部丢失。受影响面：内置 `pids_1a` 预设（JS）与任何自定义脚本化 PIDS 预设（JS 或 C++，含原生 `JCM_DRAW_TEXT` / `JCM_DRAW_TEXTURE` / `JCM_DRAW_MODEL` 记录）的 Text / Texture / 带姿态 Model 绘制；JS 装饰物（eyecandy）与 JS 车辆脚本中依赖 `.matrices()` 合并的绘制同样受累。基于 JSON 预设的 PIDS 方块（`rv_pids` / `rv_pids_sil_1` / `rv_pids_sil_2` / `lcd_pids` / `pids_projector` 默认预设）与「绝对坐标四边形 + 像素上传」型原生脚本（如 wr2a03 车辆 LCD）不受影响——这也是车辆显示一直正常、唯有脚本化 PIDS 暴露的原因。
  - 根因：`ScriptRenderManager.invoke`（yanyang.4 零分配改造）把每个绘制调用对共享 `StoredMatrixTransformations` 追加的变换消费者（PIDS 的 `BASE_SCALE`+坐标消费者、Z 序偏移、`.matrices()` 旋转三明治）在 `run()` 返回的瞬间就地截断——但绘制的真正执行是 `MainRenderer.scheduleRender` 排入的**延迟回调**，它在帧末队列冲刷时才读取消费者列表（已反编译 MTR 4.0.5 字节码确认：`transform()` 只 push 不 pop、消费列表在冲刷时现场遍历）。冲刷时列表只剩基础变换：位置与缩放从未生效。上游逐调用 `copy()` 语义正确但每帧每调用两份分配；yanyang.4 的 mark/truncate 优化正是丢掉这一点。
  - 修复：新增 `TransformCapturePool`——每个绘制调用回放时从池中取一个回收的变换快照（清空 + 以基础列表重新播种），延迟回调捕获各自的快照，冲刷时所见即所得；共享基础实例从此只读不写（eyecandy 变换缓存因此也更安全）。池在 `MainRenderer.render` 尾部（mtrpatch mixin TAIL 注入，早退路径不回收——彼时回调仍未冲刷）整体回收，稳态零分配保持，免费表上限 1024 防病态帧钉死内存。
  - 回归测试：新增 ctest `scripted_render_semantics`（纯 JVM，无需桥 / MC 依赖，四平台全跑）——以反编译核实的 MTR 4.0.5 语义桩驱动真实 `ScriptRenderManager` + `TransformCapturePool` 的完整「捕获→回放→冲刷」生命周期，断言每个调用自己的消费者存活到冲刷、调用间相互隔离、共享基础不被改动、池稳态不增长、重入绘制不在本趟执行；并内嵌 yanyang.4..8 的 mark/truncate 回放逐字语义，断言其**确实丢失**变换（若某天该判别失效，测试会以专门错误信息报警而非静默通过）。修复前对生产代码实测红（`BASE_SCALE consumer must survive until flush`），修复后全绿。
- 修复（第二轮测试版实机：自愈与日志已就位，但正线预测仍失效、全档扫描仍无输出、无 `scan-complete-v2` 标记）：站台↔站台轨道的关联判定用错了 MTR 的两套同名字。`Rail.getHexId()` 是**两端点坐标**拼接的十六进制串（`TwoPositionsBase` 分支），而 `Platform.getHexId()` 是**站台数字 id** 的 16 位十六进制（`NameColorDataBase` 分支）——两个毫无关系的 final 方法恰好同名。首轮实现把站台轨道按坐标串建索引、再用站台的 id 串去查，任何存档上都**一个也匹配不上**：全档扫描清单恒为空、扫描静默不启动、不写标记，懒扫描同样落空，客户端只能退回视距内自扫描（与实测现象逐字吻合：站台进入视野后才开始轮播）。修复与加固：
  - 关联判定改用 MTR core 自己的站台轨道解析（`SavedRailBase#isInvalidSavedRail` 同款）：以站台的公开端点访问器（`getRandomPosition()` / `getOtherPosition()`）取两端点，经 `Data.tryGet(simulator.positionsToRail, p1, p2)` 查轨道并校验 `isPlatform()`——与核心判定「站台是否有效」走同一条路，任何 MC 版本语义一致；不再涉及任何十六进制串。
  - 全档扫描完成后置位内存中的 `scanned` 标记（此前漏置导致扫描成功后仍每 tick 重新武装全档扫描，循环重扫、反复重写全部线路文件）。
  - 「扫描清单为空」从无声改为一次性 WARN（附站台总数与有线路站台数，可区分「存档确无站台」与「关联又失败」），且**不写完成标记**——环境修好后下次启动自动重扫，空结果永远不会被冻结成真相。
  - 复现与回归：以 MTR 4.0.5 真实 core 源码在沙箱内重建可运行核心（fastutil/msgpack/gson 由 Maven Central 引入），新建仿真台 snapsim 逐字移植扫描关联逻辑——修复前 6/19 断言失败（含「坐标串≠id 串」的根因断言），修复后 23/23 全绿：新造数据、真 MessagePack 存读往返、端点逆序、200 站台混合朝向规模测试（196 个有线路站台全部命中）与按线路懒扫描全覆盖。
- 修复（服务端自学习整体静默失效 → 正线仍无法预测下一站开门方向）：第一轮测试版在实机日志中整个服务端管线零输出——启动钩子依赖「MTR core 在 server-started 事件时已就绪」且所有早退路径不产生任何日志，一旦次序不符即整场无声失效。本轮修复：
  - 启动自愈：server-started 时 MTR core 未就绪不再静默放弃，改由世界 tick 懒初始化兜底——任何事件次序/环境差异最多损失几个 tick，并留有明确日志；每个维度上线时记录「标记是否存在 + 已载入文件数」，无法匹配模拟器的维度记一次性警告（含两侧维度字符串）。
  - 首扫完整性：首次全档扫描的站台清单改取自服务器启动至少 5 秒后的快照，确保不会把「线路↔站台尚未装配完」的瞬时状态冻结成永久标记；全档扫描的启动与完成各有一条 INFO，完成行包含站台总数与 left-only / right-only / both / neither 分布及路线文件数——「是否完整扫过全档」「结果是否合理」从此一行日志可判。
  - 扫描标记升级为 `scan-complete-v2`：早期测试版可能已写入的 v1 标记（当时使用过时的方块判定，数据可能为全 false）被刻意忽略，升级后每个存档都会自动获得一次全新的完整扫描。
  - 方块判定改为仅认站台方块（游戏内验证的列车行为：屏蔽门存在与否不影响列车开关门，开关门仅由站台方块决定；且无列车在场时屏蔽门必然处于锁闭态，「已解锁屏蔽门」分支在扫描场景下只可能造成漏判，不可能带来修正）——移除 `DoorScan` 中 PSD/APG 解锁分支；增量更新的方块变化监听仍同时跟踪站台方块与 PSD/APG 门（PSD 门可能占位站台边缘或伴随站台改动，宁可多触发一次有界重扫）。
  - 客户端请求尚无数据的线路时，服务端记录一次 INFO（该线路已排入懒扫描）。
- 重构（自学习迁移到服务端）：下一站开门方向记忆从客户端 `config/jsblock/door-directions.json` 迁至服务端存档，正线预测不再依赖本客户端的到访历史。根因与背景：预测扫描发生在下一站站台的世界方块位置，而 Minecraft 客户端只持有已加载区块的方块——下一站在视距之外时 `getBlockState` 一律返回空气，扫描必然落空；客户端侧记忆只能覆盖「本客户端见过的站台」。现由服务端在存档的 MTR 数据根（`./world/mtr/<维度>/`）下维护每条线路的开门方向：
  - 存储：`routes-door-directions/<2位十六进制分片>/<线路hex id>.json`（与 MTR 自身 `routes/` 分片规则一致，每条线路一个新文件，按站台记录规范化后的左右可开侧）；`scan-complete-v1` 标记文件防止以后启动重复全档扫描。**不在 `routes/` 内部建文件的原因**：MTR 的 `FileLoader` 会把 `routes/` 内的每个文件当作 MessagePack 线路数据解析——解析失败（`MessageTypeException`）当场删除；解析「成功」的文件（哪怕解析出来是无效线路）也会进入其哈希簿记，下次自动存档（30 秒）即被清除——任何外来文件都会在半分钟内自我销毁，故镜像其目录结构存放于同级文件夹。
  - 首次扫描：服务器第一次以本版本 JCM 启动时，对**全服所有有线路经过的站台**（且仅有这些，无线路经过的站台轨道不扫描）逐条站台轨道采样扫描周围方块（是否加载均扫，未加载区块按需载入读取——这正是客户端物理上做不到的能力）；扫描沿每 tick 预算推进（每 tick 至多 2 个站台、4ms 时间上限），绝不停顿单帧。方向以站台轨道固有方向规范化存储，与列车是否倒行无关；每条线路的每个站台记为 `not` / `left` / `right` / `all`（以列车车头为准，经客户端既有的 `reversed == reversePositions` 换算后即得本车左右）。
  - 增量更新：服务器正常运行时，对站台方块（含 PSD/APG 门）被放置 / 拆除 / 替换的变化，经 `World#setBlockState` mixin 捕获后按区块→站台两级合并去重，每 tick 至多重扫 4 个站台——WorldEdit / Axiom 的批量放置 / 拆除 / 替换无论多大，都会收敛为少量重扫而非卡顿；离变更很远的站台永不被触碰。新增站台 / 线路由「按请求懒扫描」补齐（客户端请求无数据的线路时服务端自动排入扫描队列）。
  - 客户端：`getDirectionOfDoorOpening()` 预测时优先请求并使用服务端存储（整条线路一次请求、客户端缓存、含 TTL 刷新与限流退避），服务端未答（未装本版本 / 扫描进行中）时回退到既有的视距内自扫描。站台停靠时的实时扫描（真值）仍然优先。
  - 存档迁移性：删除 `scan-complete-v1` 标记即可强制下次启动重扫全档（如恢复世界备份后）；`mtr restoreWorld` 整体替换 mtr 目录时门向数据随线路数据一起复位，标记随之一并消失，行为自洽。
  - 客户端侧旧文件 `config/jsblock/door-directions.json` 不再读取 / 写入（可手动删除）。
- 新增脚本 API：`vehicle.getDirectionOfDoorOpening()`（JS 与 C++ 均可调用，返回 `"not"` / `"left"` / `"right"` / `"all"`），识别车辆在当前站 / 下一站的开门方向。停靠站台时，无论车门处于未开 / 半开 / 关闭中 / 已关的哪个阶段，返回值都与车门完全打开时一致；正线运行时预测下一站的开门情况（把车辆按停车点摆放后，按 MTR `RenderVehicleHelper#canOpenDoors` 同款方块扫描逐车厢检测，视距外由上文服务端存储作答）；同侧任一车门可开即整侧记为可开。左右相对车头（bogie-1 端），不受行进方向 / 倒行影响。纯查询语义——不改变 MTR 实际开门行为。
- 原生 C ABI 升至 11：车辆快照原 `_pad0` 字节升格为 `door_direction`（0..3，对应上述四个值），布局与尺寸不变；ABI 9/10 模块照常工作，`mtr::Vehicle` 新增 `doorDirection()`（枚举）与 `getDirectionOfDoorOpening()`（字符串）访问器及 `mtr::door_direction` 常量。
- SDK 术语对齐 MTR 4：车辆快照包装类由 `mtr::Train` 更名为 `mtr::Vehicle`（与 `VehicleWrapper` / `vehicleScripts` 命名一致）；`mtr::Train` 保留为源码兼容别名，老模块无需修改即可继续编译。C ABI 与二进制布局不变。
- CI 修复：Complete Build 的 1.18.2 矩阵项在「Build both loaders with all seven JNI bridges」步骤因 `maven.minecraftforge.net` 下载 `lwjgl-openal-3.2.2-natives-linux.jar` 读取超时而偶发失败（网络瞬时故障，与代码无关）；build / Complete Build / Release 三条流水线的 gradle 步骤现自动重试至多 3 次。
- JNI 术语残留清理（桥自 layout v2 起已完全 JDK-free，本次仅命名与注释）：`native/jni/jni_bridge.cpp` 更名 `native/bridge/bridge.cpp`；workflow 步骤名「seven JNI bridges」→「seven native bridges」、「real JVM JNI integration」→「real JVM integration」；移除从未被 CMake 消费的 `-DJCM_JNI_INCLUDE_DIR` 旗标与配套 jni.h 注释；修正若干过时注释（jvm_test.py 文档串、CMake 与 bench/头文件及 Java 侧若干「the bridge is a JNI library」类表述）；`build.gradle` 旧桥报错不再指向仓库中不存在的 `bridge/README.md` / `repair-api/build-bridges.ps1`。二进制与行为零变化。

- 原生桥扩展至 8 平台：新增 **windows-arm64**（`windows-11-arm` 原生 runner 构建，含全套 JVM 集成 ctest）。修复捆绑桥搜索列表在 ARM64 Windows 上仍指向 `windows-x64` DLL 的问题——x64 DLL 无法在 ARM64 JVM 进程内加载（无进程内仿真），此前 Windows on ARM 用户永远加载不到捆绑桥。资源包模块需提供 `natives/windows-arm64/` 副本方可在此平台运行。
- 全模块执行与性能审计修复（Java 渲染管线 / 原生传输 / C++ 桥与 SDK / 服务端门向系统，共 4 路并行审计）：
  - **C++ 桥（ARM 内存序）**：poller 平面的结果槽 seq 回显此前是无栅栏普通写入——其余所有跨线程发布点均已用释放栅栏（`wr64`），唯独此处在 ARM64（linux / android / ios / macos / windows）上允许负载先于 seq 可见，Java 端可能消费上一帧的结果（含已被后台回收器释放的 arena 地址，即 JVM SIGSEGV 向量）。现 seq 回显统一经 `wr64` 发布。
  - **C++ 桥（结果槽别名）**：渲染发布的 `RES_EXTRA`（偏移 128）与 FLOATS arena 描述符的 capacity 字（112+16）重叠，每次渲染把浮点容量清零——今天被 pre-ABI-9 回退掩盖，任何未来按 capacity 的复用都会踩中。现渲染发布先写 status/count，再写 arena 三元组，seq 回显最后（带栅栏）；命令路径的 `RES_EXTRA` 语义不变。
  - **C++ 桥（FFM 平面异常兜底）**：poller 平面每请求都有 try/catch（yanyang.5），但 `jcm_module_*` / `jcm_shm_kick` 直调导出没有——桥自身 `bad_alloc`（路径、键、实例表、最大 16 MB 的 api 回复）或手写非 SDK 模块抛出的异常会穿越 extern "C" 下调用帧直入 JVM，`std::terminate` 杀死整个进程。现全部导出逐一带兜底（错误码返回 + 一行 stderr），`jcm_shm_kick` 在 seq 已认领时发布错误结果防调用方干等。
  - **C++ SDK**：`Text` 不设 `.text()` 直接 `draw()` 由 `strlen(nullptr)` 崩掉整个模块，现按空串处理；`Arrivals::next_for_platform` 负游标先钳到 0（与同族访问器的边界检查对齐）。
  - **JS 捕获 / 回放竞争（yanyang.4 `takeFrom` 回归）**：JS 脚本在 4 条后台执行线程上运行，捕获回调在执行线程把已捕获绘制列表原地换入（`takeFrom`），渲染线程可能正在同列表上回放——换表可能落在 size 读取与 `get(i)` 之间（`IndexOutOfBoundsException` 直透 MTR 渲染），或回放已释放回池的绘制调用。四类 JS 脚本（vehicle / PIDS / lift / eyecandy）全部受影响；`takeFrom` 的「仅渲染线程」注释与事实不符。现回放（`invoke`）与捕获（`takeFrom` / `reset`，声音侧含 `addCallsFrom`）共享同一管理器监视器——两侧都只持微秒级。
  - **每部件每车厢全树重建**：`ModelPropertiesPartMixin` 在每个模型部件渲染前调用 `getVehicleCarsAndPositions()`（整列车位置树重建）——P 部件 × N 车厢 = 每帧 P×N 棵，而全帧本有一次足够；且 MTR 在存储模型渲染路径传 `null` vehicle 时直接 NPE。现仅 DISPLAY 部件查询、车辆 id 从不可变车厢表 O(1) 读取、判空。
  - **shouldInvalidate O(V) 流扫描**：每个 JS 车辆实例每帧一次 `vehicles.stream().noneMatch(...)`（外加每 tick 清扫），几十列车即每秒数十万次流操作。现由 `RenderVehiclesMixin` 每帧重建一次存活车辆 id 集，逐实例查询降为 O(1)。
  - **包装器白建**：JS 车辆包装器（位置树 + 逐车厢门方块扫描）此前在脚本任务仍在途的每一帧都完整构建再丢弃；慢脚本运行期间即每帧一次。现按「本帧是否会真正提交脚本」判定，不在提交帧不建包装器。
  - **Forge 端原生车辆视锥剔除从未生效**：`FabricVehicleCulling` 只在 Fabric 入口注册，Forge 上 `frustum()` 恒 null、剔除永远放行——每辆脚本列车全价支付快照封送 + 画布重绘 + 纹理上传。Forge 1.17.1 无携带视锥的渲染事件（`RenderLevelStageEvent` 1.18 起才有且各版本阶段集不同），故 Forge 侧改走**直读**路径：新增 `LevelRendererCullingAccessor`（`cullingFrustum` 字段，官方映射 6 版本逐一核验）+ 入口安装的存活供应者（帧中读取恒为当前帧；每帧最多重包一次 MTR 持有器）。任何异常即回退「不剔除」。
  - **控制槽双锁**：渲染发布持 `NativeChannel.class`、命令发布持 `ShmPoller.COMMAND_LOCK`——单控制槽两个互斥锁，仅靠「全部发布者都在渲染线程」的未成文不变量才安全；第二发布线程可在命令取号与发布之间插入，撕裂请求槽。现命令发布与渲染同锁（`reset`/`ensureShm` 本就持该锁，天然重入），`COMMAND_LOCK` 继续守会话关联与 CBQ 环。
  - **reset 顺序**：先 close 区域再 detach，期间 CBQ 重入线程可摸到已 unmap 的映射。现先清会话引用再 unmap。
  - **陈旧桥 15 秒冻结**：pre-poller 时代的旧桥二进制（`System.load` 正常、无 poller 线程）每次 F3+T / 进世界都让主线程干等 15 秒再报一串误导性的「模块打不开」；`reset()` 又清掉 `shmBroken`，冻结每次重载重付。现就绪预算降为 8 秒（工作桥 25 毫秒内即挂载），失败后会话级粘滞跳过并给出明确的「桥二进制早于 poller 传输」提示与处置指引。
  - **字符串驻留表哈希混入 arena 偏移**：同一字符串因跑马灯长度变化而偏移挪动后算作新条目，表被撑爆整体清空再全量重解码。现哈希仅按（长度、首字节），等值判定本就按内容。
  - **字形掩码无字节上限**：`rasterizeText` 的掩码以条目数为唯一上限，`maxWidth` 由模块控制——单条可达 maxWidth²×4 字节。现边长钳到 2048、总量 32 MB 超限整体回收（与姊妹字体渲染器的 LRU 同策）。
  - **内容实例清扫无预算**：PIDS / 装饰 / 电梯实例批量空闲过期在单 tick 内全部销毁（一次销毁≈十几张 GL 纹理 + 已上传模型 + 整幅画布）——yanyang.7 只给车辆侧加了 2/tick 预算，此处是同类卡顿。现与车辆同款：常态 2/tick（最旧先销毁），≥8 的批量退出（世界卸载 / 切维度 / 断线）一次性冲刷。
  - **门向系统 O(P²) 首扫**：全档扫描进行期间 `canFullScan` 恒真，tick 循环每 ~2 tick 重新快照全部平台构建工作清单，只为被 `fullScanActive` 守卫丢弃——服务端线程全程 O(P²) 分配churn。空扫描维度（每个存档的下界 / 末地）更以同一节奏无限重试。现运行中不再重复请求；空扫描的自动重试搭 600 tick 索引刷新的车（自愈保留，churn 从每 2 tick 降到每 30 秒）。
  - **门向系统重建站台不收敛**：拆除重建的站台（新站台 id、同线路）此前永远进不了答案——懒扫描只对「无数据的线路」排队，而站台自身的放置变化落在索引覆盖之前被丢弃。现每次索引刷新先按存活索引修剪失据条目（删掉的站台 / 失去站台轨 / 离开线路），再把「线路有数据但缺此站台」的站台排入扫描；短暂失去轨道的站台会在后续刷新中自动回补。逐 tick 预算改用单调钟（`System.nanoTime`，`currentTimeMillis` 在 Windows 集成服务器上仅 ~15.6 ms 粒度且可被 NTP 拨动）；无模拟器维度进入负缓存（此前每 tick 重新分配包装器再丢弃，仅在 Main 实例更换时失效）；`setBlockState` mixin 改挂四参重载（2/3 参委托其内、`ServerWorld` 不覆写——所有世界级写入恰经过一次，直接 4 参调用者不再漏报，双加载器同改）；门向查询加 2 秒每线路节流；退出时写线程 10 秒未冲刷给出明确 WARN（此前静默放弃）。
  - 顺带修复：调试叠加层在异常 message 为 null（裸 NPE）时自身崩溃进入 HUD 渲染循环。
- 开发文档同步：网页版（Develop-Documentation-Website 分支，7 语言）与 native/README.md 同步 8 平台清单与 windows-arm64 打包目录；`getDirectionOfDoorOpening` 条目补服务端持久记忆优先、客户端视距扫描兜底的语义（此前仅描述客户端扫描）。

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
