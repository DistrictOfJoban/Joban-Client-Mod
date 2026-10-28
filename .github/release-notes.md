JCM Yanyang `2.3.0-prerelease.2-yanyang.1` — 上游 v2.3.0-prerelease.2 全量同步（61 个提交）；脚本化 PIDS 文字乱飘修复；C++ SDK 与上游 API 面同步。

**脚本化 PIDS 文字乱飘修复（yanyang.8 回归，`jsblock:pids_1a` 报告）**

脚本化 PIDS 的文字与贴图随玩家移动满处乱飘、大小异常。根因：yanyang.8 的变换快照对象池在把 base 变换捕获进回收实例时只复制了 consumer 列表，**丢失了 MTR 4 参构造器承载的世界锚点**——锚点是 flush 时相机偏移补偿的唯一通道，丢失后每次绘制都渲染在渲染实体（相机）位置。修复：`acquire()` 现按上游 `copy()` 的字段语义重新播种锚点，**保持零稳态分配**；同因修复 eyecandy 变换缓存（世界坐标此前被错误塞进不参与补偿的 consumer，几何体被平移渲染实体位置）。车辆 / 电梯 / 原生脚本路径行为不变。新增 S6 / S7 回归测试（锚点存活 + 回收实例逐帧重锚定），六平台 CI 全绿，红绿验证确保旧代码必然触发断言。

**上游同步：2.3.0-beta.3 → v2.3.0-prerelease.2（61 个提交）**

- **脚本执行防护**：新增 `scriptRateLimit` 预设字段与全局速率限制；解析阶段超时不再挂起游戏；无限循环可被检测并中止。
- **脚本诊断**：解析错误以游戏内悬停文本展示；仅运行时错误输出 JS 堆栈；`console` 对象转 JSON 输出。
- **脚本缓存**：同一脚本不再重复编译（重载时重置）；DataReader 运行时缓存按 tick 过期自动重置。
- **脚本核心与 Rhino 解耦**：`ParsedScript` 抽象化 + `ScriptingLanguage` 多语言地基（`scriptLang` 字段）；Rhino 实现移入 `core.rhinojs` 包。
- **脚本 API**：PIDS 绘制类 `Rectangle` 更名 `Rect`（避免与 AWT 同名冲突）；`id()` / `idr()` 长形式标记弃用，新增字符串等价形式与 `JSUtil.parseId`（支持 `"./"` 脚本相对路径）；`Resources.id(id, suffix)`、`Resources.mgr()` 别名、`Resources.ANTE_FLAG`、Java 数组创建等 ANTE 兼容与工具 API；`ConfigResponder`（装饰物自定义配置注册）、`SoundHelper` / `TickableSound` 兼容层。
- **渲染与修补（上游）**：MTR 4 著名的 BVE 电机声音卡死修复（被剔除车辆的声音不再永久滞留）；Iris / OptiFine 阴影 pass 重复执行脚本修复扩展至电梯 / 车辆 / 装饰物 / PIDS 全部脚本渲染面；贴墙方块面朝向检查修复；`GraphicsTexture` 新增 `fullAA()` / `noAA()`；装饰物配置界面重做（布尔开关、`ConfigCheckbox`、ANTE 多配置注册）；运行时阶段读取资源不再抛 `ConcurrentModificationException`。
- **Forge（上游）**：mixin 构建设置修复，refmap 统一为 `jsblock.refmap.json`。
- **翻译（上游）**：新增爱尔兰语（ga_ie）与捷克语（cs_cz）；更新日语、简体中文。
- **晏阳适配**：上游全部改动与原生 C++ 脚本管线（车辆 / PIDS / 装饰物 / 电梯的 JS + C++ 混合条目）、零分配渲染路径、标识符缓存融合；`willInvokeRender`、`takeFrom` 交换、`TransformCapturePool` 等晏阳机制经新抽象层继续生效；yanyang 全部自有特性（共享内存 / FFM 双传输、C++ 原生脚本、服务端门向持久记忆、8 平台原生桥）完整保留。

**C++ SDK：与上游 prerelease.2 JS API 面同步**

- 新增 `mtr::Rect`（上游 JS 类 `Rectangle` → `Rect` 的 C++ 对应物，按 `RectangleWrapper` 语义全量移植）。
- Text / Texture / Rect 构建器新增 `renderType(String)`；记录头 flags 字节承载 `naturalLight()`——结构体尺寸与字段偏移零变化，新旧模块与新旧宿主双向二进制兼容。
- `Gfx2D::full_aa()` / `no_aa()`（一像素覆盖率抗锯齿，默认关闭）、`EyeCandy::facing_angle()`、`mtr::StateTracker<T>`（含上游新语义：`set_state()` 返回是否为进入该状态的首帧）。
- 修复：`Texture::draw` 对未调用 `.texture()` 的构建器不再崩溃整个模块（跳过该记录）；Java 回放此前静默丢弃渲染层字段的问题一并修复（旧记录零值恰好映射包装器默认值，行为不变）。

**交付物**：12 个 jar（6 个 MC 版本 × Fabric / Forge，均内置 8 平台原生桥）+ native SDK zip。mod id 仍为 `jsblock`（与原版 JCM 互斥），依赖 MTR 4.0.5+；fabric 版另需 Fabric API。
