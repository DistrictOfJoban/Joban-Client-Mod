# PSD 屏蔽门底色（黑/白背板）在 Iris 光影下不渲染 —— 完整复盘与接手文档

> **文档目的**：本 bug 已经历十一轮修复尝试（yanyang.4–13 + 2.3.0-1.0.0/1.0.1/1.0.2 + 第九/十/十一轮）。
> 本文档提炼全部历史记录、反汇编实证、已证伪与未测试的方案矩阵，以及后续诊断流程——
> 供任何接手者（人类工程师或 Claude 等 AI 助手）直接续作，不必重新踩坑。

---

## 1. 问题定义

**现象**：PSD（站台屏蔽门）玻璃上绑定的脚本显示（`PSDScriptRenderer`）中，**底色背板**（站台侧黑色 + 轨道侧白色）在 Iris 光影下不渲染——透过玻璃看到的是场景而非黑底；**无光影时一切正常**。脚本的**文字内容**在光影下始终可见（这是唯一从未失效的东西）。

**用户环境（多次实测日志一致）**：
- Minecraft 1.20.1（fabric）
- **Iris 1.7.6** + **ComplementaryUnbound r5.5.1**（光影包）
- **ImmediatelyFast 1.5.5**（曾短暂卸载复测，底色仍不显示——已排除为唯一根因）
- Sodium 0.5.x + Sodium Extra、Continuity、Indium 等
- **YYMTR-fab 4.0.5+1.20.1**（用户的自定义 MTR 构建，`2026.11.04.3084`）
- MTR Scripting via JCM（本模组的脚本运行时）

**代码位置**：`fabric/src/main/java/com/lx862/jcm/mod/scripting/psd/PSDScriptRenderer.java`
（双加载器共享源码 + Manifold 预处理器 `#if LOADER == "fabric"` / `#if MC_VERSION >= "11903"`；`forge/src` 是构建期从 `fabric/src` 复制的镜像，**真源码只有 fabric/src**）。

**几何设计（三明治）**：画布与底板都嵌在玻璃块内部、玻璃面（平台侧）之后——
底板在 `paneAlong + 0.45/16`、脚本画布在 `paneAlong + 0.25/16`（`paneAlong` 为运行时从烘焙模型推导的玻璃面深度，vanilla 为 4/16）。玻璃的半透明贴图叠在最前，只在贴图的透明窗口处透出底色与内容。

---

## 2. 唯一在用户光影栈下被实证可见的路径（截至第十一轮）

**脚本文本**（自 yanyang.1 起从未失效）与 **MTR 原版 PIDS 文字**（`RenderPIDS`）共用同一条管线：

```
脚本: TextWrapper.drawTransformed
  → MainRenderer.scheduleRender(QueuedRenderLayer.TEXT, callback)   // 无 key（""）
  → 回调内: graphicsHolder.drawText(text, x, y, color, shadow, light)
      ↓ （GraphicsHolder.drawText 反汇编，MTR 4.0.5 真实字节码）
  → immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer())  // 每次调用新建的私有 immediate
  → TextRenderer.draw(text, x, y, color, shadow, matrix, immediate,
      TextLayerType.field_33993 /* = NORMAL */, 0 /* 无背景色 */, light)
  → immediate.draw()                                                 // 回调内当场同步 flush
```

**MTR 原版 PIDS（`RenderPIDS`）只画文字**（`graphicsHolder.drawText`，同一 TEXT 队列）——这就是它们在用户光影下可见的原因。

---

## 3. 本轮（第十一轮）反汇编三大实证

对 Mojang 官方 1.20.1 client.jar（HTTP range 拉取 `RenderLayer`/`RenderPhase`/`TextRenderer`/`GlyphRenderer`/`BufferBuilder`）与 MTR 4.0.5 jar 的 `GraphicsHolder`（32 个方法全量反汇编）得到：

### 实证 ①：`drawText` 用的是 **NORMAL** 层，不是 SEE_THROUGH

`TextRenderer$TextLayerType`（`class_327$class_6415`）枚举声明序（`<clinit>` 反汇编）：
`a = field_33993 = NORMAL`，`b = field_33994 = SEE_THROUGH`，`c = field_33995 = POLYGON_OFFSET`。

`GraphicsHolder.drawText`（真实 4.0.5 字节码，offset 64）：
```
getstatic class_327$class_6415.field_33993   // ← NORMAL！
invokevirtual class_327.method_30882         // TextRenderer.draw(..., layerType, backgroundColor=0, light)
```

**结论**：可见文字骑 `RenderLayer.getText(atlasPage)`（程序 `rendertype_text`）。
第九/十轮的通道 T 用了 `SEE_THROUGH`（`getTextSeeThrough`，程序 `rendertype_translucent_text` + `text_background_see_through`）——**另一个着色器程序对**，在用户光影栈下不渲染。这就是通道 T 隐形的直接解释（此前第九轮 CHANGELOG 记载的「getText 带深度测试」推断是**错的**，见实证②）。

### 实证 ②：两个 text 层都不做深度测试、都不写深度

`RenderLayer` 的 text 层构造（`fkf` 方法 x = getText、y = getTextSeeThrough）反汇编，两者 phase 链完全一致：
- `fke.ay` = `DepthTest("always", 519)`（519 = GL_ALWAYS）——**无深度测试**
- `fke.aD` = `WriteMaskState(color=true, depth=false)`——**只写颜色**
- `fke.h` = `translucent_transparency` → Iris 的 **GENERAL_TRANSPARENT** 批次（`endTranslucents` 后 flush，**在半透明玻璃面板之后**——这正是文字能穿过面板可见的原因之一）
- `fke.as` = `lightmap(true)`；无 Cull phase（剔除态继承自外部——绕向必须用被实证可见的那个，见实证③）

### 实证 ③：可见字形的顶点序 = 左上 → 右上 → 右下 → 左下

`GlyphRenderer.render`（`err.a(Lerr$a;Lorg/joml/Matrix4f;Lein;I)V`）反汇编：
`Rectangle(minX,maxX,minY,maxY,z,r,g,b,a)`，四顶点依次 `(minX,minY) → (maxX,minY) → (maxX,maxY) → (minX,maxY)`，每顶点写 `vertex(Matrix4f,x,y,z) → color(IIII) → texture(FF) → light(I) → next()`（**四元素序列，无 overlay 无 normal**）。

text 层顶点格式 = `POSITION_COLOR_TEXTURE_LIGHT`——所以 **`GraphicsHolder.drawTextureInWorld` 不能用于此层**：它的写入序列是 `vertex→color→texture→OVERLAY→light→normal`，OVERLAY 值（0）会落进 LIGHTMAP 槽、normal 越过 stride（yanyang.11/12 之败因，第九轮 1.0.1 提交已定位但该版从未发布）。

### 附带实证（MTR 4.0.5 队列机制，`MainRenderer.java`）

- `MainRenderer` 是 `EntityRenderer<EntityRendering>` 的子类——全部队列在**实体阶段**排空。
- 排空顺序按 `QueuedRenderLayer` **ordinal**：`LIGHT(0) → LIGHT_TRANSLUCENT → LIGHT_2 → INTERIOR → INTERIOR_TRANSLUCENT → EXTERIOR(5?) → … → TEXT(8, 最后)`；每 key 先 `createVertexConsumer(layer)` 预热（TEXT 无预热层）。
- `MoreRenderLayers`：`getLight(tex,false)=getBeaconBeam(tex,false)`、`getLight2(tex)=**getText(tex)**`、`getExterior(tex)=getEntityCutout(tex)`。
- `GraphicsHolder.createVertexConsumer(layer)`：`holder.vertexConsumer = holder.vertexConsumerProvider.getBuffer(layer.data)`；`drawTextureInWorld` 写入 `holder.vertexConsumer`（即队列持有者的实体 immediate——**不是** drawText 的私有 immediate）。
- MTR 车辆（`RenderVehicles`）通过 keyed `EXTERIOR` 队列 + `drawTextureInWorld` 绘制，在用户光影下**可见**——EXTERIOR/entity-cutout 队列管线本身没坏；**坏的是「被半透明玻璃面板遮住的深度测试四边形」在这套光影栈上的呈现**（车辆在开阔空间，前面没有玻璃面板）。

---

## 4. 十一轮尝试矩阵（时间序）

| 轮次 | 版本 | 载体（层 / 队列 / 写入器 / immediate） | 光影下结果 |
| --- | --- | --- | --- |
| y4–y7 | yanyang.4–7 | 私有 Immediate + **裸矩阵**（未借用持有者矩阵） | 不可见（矩阵是 identity，画在世界原点附近——已定位） |
| y8–y10 | yanyang.8–10 | MTR 队列回调内**每帧新建**消费者（entityCutout 等） | 不可见 |
| y11/y12 | yanyang.11/12 | 私有 Immediate + `drawTextureInWorld` + entityCutout/getText + 同步 flush | 不可见（**写入器错配**：overlay 落进 LIGHTMAP 槽；当时未定位，误判为层不可见） |
| y13 | yanyang.13 | LIGHT 队列 beacon-beam 层 + `drawTextureInWorld` | 不可见（格式错配 + GENERAL_TRANSPARENT/深度疑云） |
| 第六轮 | 2.3.0-1.0.0 | keyed EXTERIOR（RenderRouteBase 路线牌纪律）+ `drawTextureInWorld` | 不可见（build 失败一度未实测；后续实测证伪） |
| 第七轮 | 2.3.0-1.0.1 | **私有 Immediate + 手写 glyph 顶点序 + `RenderLayer.getText(white.png)` + 同步 flush**，排 LIGHT_2 队列回调 | **从未发布**（两处编译错误，CI 失败；修复提交 900be67/7f07a43 后被第八轮整体替换——**该组合从未实机测试**，且其绕向抄了 entity-cutout 谱系（反序），见实证③） |
| 第八轮 | 2.3.0-1.0.2 | 载波 A：keyed LIGHT_2（预热 `getText` 消费者）+ `drawTextureInWorld`（写入器错配！）；载波 B：keyed EXTERIOR + `drawTextureInWorld` | 载波 A/B 面包屑均出现、完整执行、不可见（build 因 macOS runner 故障一度取消后成功） |
| 第九轮 | build.8x | **通道 T**：`TextRenderer.draw(W行, SEE_THROUGH, backgroundColor=黑)` + 私有 immediate + 同步 flush + 相机侧门控 | 不可见（**层选错**——SEE_THROUGH 程序对，见实证①） |
| 第十轮 | build.91 | 通道 T + `ShaderDetector` 光影门控 + 开销削减 | 不可见（同上；日志证实 `channel T active` 出现、无异常） |
| **第十一轮** | **本版** | **通道 N：`RenderLayer.getText(jsblock:white.png)`（NORMAL 层）+ 手写 glyph 顶点序（字形绕向）+ 私有 immediate + 同步 flush + 相机侧门控（站台黑/轨道白各一四边形）+ 光影门控 + 窗口裁剪** | **待实测** |

**矩阵透视**：「NORMAL 层 + 正确写入器 + 私有 immediate」这一格从未被实机测过——第七轮造出了它但从未发布，之后所有实测轮要么写入器错（drawTextureInWorld）、要么层错（SEE_THROUGH）。第十一轮正是补测这一格，且每个成分都有独立的实证支撑（层=可见文字的层；写入器=可见字形的序列；immediate=drawText 的字节码纪律；绕向=可见字形的绕向）。

---

## 5. 第十一轮实现要点（`drawBackingText` + `glyphQuad`）

1. **调度**：`MainRenderer.scheduleRender(QueuedRenderLayer.TEXT, callback)`——与脚本文本同队列、同 `""` key、**先于** `replayScript` 提交（同 key 列表 FIFO ⇒ 底板先 flush、文字后画在其上）。
2. **相机侧门控**：`along = dot(相机 - 段锚点, facing) ≥ 0.25` → 轨道侧。站台侧画黑四边形（`flip=true`，字形绕向 左上→右上→右下→左下）；轨道侧画白四边形（反序绕向，`z=+0.0005` 更深——`facing` 指向轨道侧）。**每视图恰一个四边形**，不依赖剔除/深度状态。
3. **层**：`immediate.getBuffer(RenderLayer.getText(new Identifier("jsblock","textures/block/white.png")))`（forge：`RenderType.text(ResourceLocation)`）。层实例由原版 `Util.memoize` 按纹理 ID 缓存。纹理 = 本模组自带**不透明** 16×16 纯白（`mtr:white` 在用户 YYMTR 4.0.5 里是透明 Utility 精灵，yanyang.5 已证不可用）。
4. **写入器**：`glyphQuad` 逐顶点 `vertex(Matrix4f,x,y,z) → color(r,g,b,a) → texture(u,v) → light(MAX_RENDER_LIGHT) → next()`——**严禁** `drawTextureInWorld`（overlay 陷阱）。
5. **immediate 纪律**：`VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer())` + 回调内 `immediate.draw()` 同步 flush——`GraphicsHolder.drawText` 的字节码复刻。矩阵取自回调持有者（`((NativeMatrixApplier)(Object) holder).jcm$getMatrixStack()`）。
6. **几何**：`[segLeft, segRight] × [frameTop-windowTop, frameTop-windowBottom]`（派生透明窗口 = 脚本画布同一矩形）。该层在玻璃面板**之后** flush（GENERAL_TRANSPARENT），会画在面板像素之上——超出窗口即涂门框，所以必须裁到窗口。
7. **门控**：`shaderPack = !shadowPass && ShaderDetector.isShaderPackInUse()`——无光影完全不画（载体 B 独撑既有正确观感，且无深度矩形会盖住脚本的图片/形状——LIGHT 队列 ordinal 0 先 flush、TEXT ordinal 8 最后）；OptiFine 暂不启用通道。
8. **性能**：每段每帧 1 个四边形（2 三角形）、零字符串工作、层缓存、唯一分配 = MTR 每次 drawText 本就构造的 Immediate 小包装。
9. **面包屑**：`[JCM] PSD backing channel N active (text-layer quad, shader pack in use)`（无光影不出现属预期）；异常面包屑 `[JCM] PSD backing channel N failed once: <异常>` 保留。
10. **载体 B 原样保留**：keyed EXTERIOR + `drawTextureInWorld` 双绕向全范围双色——无光影观感的唯一承载者，也是其他光影包 OPAQUE 批次可用时的备胎。

---

## 6. 本版验证流程（给用户 / 测试者）

1. 下载 Complete Build 产物（版本号 `2.3.0-1.0.0-build.<N>`，构建自本提交）。
2. **无光影**：观感应与 build.91 完全一致（载体 B 独撑；`channel N active` 面包屑**不应**出现）。
3. **开 Iris + ComplementaryUnbound**：
   - 站台侧：透过玻璃窗口应见**黑色底板**，脚本文字叠在其上；
   - 轨道侧（乘车/进隧道回头看）：透过窗口应见**白色底板**（不再透出镜像内容或场景）；
   - `latest.log` 应出现 `[JCM] PSD backing channel N active (text-layer quad, shader pack in use)`；
   - 无任何 `channel N failed once` 异常行。
4. 性能：应无可感知帧率变化（1 四边形/段/帧）。
5. 含图片/形状的脚本：与 build.91 行为一致（本通道不触碰其路径）。

---

## 7. 若本版仍不可见——后续候选与诊断

按优先级排序（每条都附判据，来自既有实证）：

### 7.1 先查日志分流

- **无 `channel N active` 且无 `failed once`**：回调没跑——绑定/驱动层问题（`PSDClientBindings`、`MainRendererPSDMixin`），或光影门控误判（`ShaderDetector` 对该 Iris 版本失效）。
- **有 `failed once: <异常>`**：按栈修（最可能是某 MC 版本的 VertexConsumer 方法名差异——CI 全版本编译会先拦住）。
- **`channel N active` 出现且无异常、仍不可见** → 进入 7.2。

### 7.2 剩余变量：纹理 / 程序组合

通道 N 与可见文字的唯一剩余差异是 **Sampler0 绑的是我们的白纹理而非字体图集页**。个别光影包的 `rendertype_text` 补丁可能假定纹理是字体图集（尺寸/alpha 语义）。候选：

- **A（首选）**：用**字体图集自身的纯白字形区**做 UV——把四边形 UV 映射到某个实心字符（如 U+2588 全块，或实测字体图集中的不透明像素区）的图集矩形上，纹理 key 换成该图集页（如何拿到图集 Identifier：`GlyphRenderer` 持有；或退化用 `RenderLayer.getText(new Identifier("minecraft","textures/font/ascii.png"))` 之类固定页 + 精心挑 UV）。零新纹理、程序/纹理组合与可见文字完全一致。
- **B**：自定义 `RenderLayer.MultiPhase` 完整克隆 getText 的 phase 链（同 program/transparency/depth/writemask/lightmap/texture），另加显式 `Cull` phase。用于隔离「继承剔除态」变量（如果怀疑绕向）。
- **C**：`getTextSeeThrough` 的四边形版（非 TextRenderer.draw 版）——nametag 语义；与 NORMAL 版做 A/B。
- **D**：退路——动态纹理（`GraphicsTexture`，MTR 路线牌同款）注册纯白纹理，排除资源包纹理加载差异。

### 7.3 更深的结构性方向（若 7.2 全败）

- 检查 Iris 的 `MixinCompositeRenderType` / `FullyBufferedMultiBufferSource` 对**非实体渲染路径**提交的 GENERAL_TRANSPARENT 内容的处理：文本可见说明 text 层过了；但如果我们的 flush 时机（实体阶段内、私有 immediate 同步 flush）与 drawText 有任何微妙差异……可以用 F3 日志或 Iris 的调试视图（`iris.debug`）确认批次归属。
- 在**无 ImmediatelyFast** 环境 + Iris 下单独复测（历史上做过一次，但当时通道本身有其他缺陷——矩阵已变，值得重测一次完整矩阵）。
- 用其他光影包（BSL、Photon 等）+ Iris 复测：定位是 Complementary 特有还是 Iris 通用。

### 7.4 已证伪 / 已排除，不要再试

- MTR 队列预建消费者 + `drawTextureInWorld` 的任何组合（EXTERIOR/LIGHT_2/LIGHT/beacon-beam）——多轮实证不可见（写入器与 beacon-beam/getText 格式错配是已知缺陷，且 entity-cutout 路线在玻璃后被该光影栈吃掉）。
- SEE_THROUGH 层上的 `TextRenderer.draw` 背景色矩形（第九/十轮通道 T）——程序对不渲染。
- `drawTextureInWorld` 写 text 层（overlay 落 LIGHTMAP 槽）。
- `mtr:white` 作为底板纹理（用户 YYMTR 内为透明精灵）。
- ImmediatelyFast 是唯一根因的假设（卸载后仍不显示）。
- 「getText 带深度测试所以不可见」的假设（实证②证伪：两个 text 层都是 GL_ALWAYS、只写颜色）。

---

## 8. 构建 / 部署备忘（本仓库工具链）

- **版本**：`gradle.properties` 的 `mod_version=2.3.0-1.0.0`；Complete Build 产物版本号 = `<基础版本>-build.<github.run_number>`（例：`2.3.0-1.0.0-build.91`）。Release Build 全平台全量发布（勿动）；Complete Build 仅 3 平台原生桥（Win x64 / Linux x64 / Android ARM64，按用户 2026-11-10 要求精简）。
- **提交**：本机 git push 网络不通——用 Git Data API 三步提交（参见 `scripts/push_round10.py` 的模式：GET 当前 HEAD → POST blob/tree/commit → PATCH ref）。token 在 `/home/z/.gh_token_chmod_tmp`。
- **触发构建**：workflow_dispatch `Complete Build`，入参只传 `{"mod-version": "2.3.0-1.0.0"}`（**不要**传 retention-days 等额外字段，会 422）。
- **监视**：`/home/z/my-project/scripts/watch_build_run.py <runId>`；构建产物在 run artifacts。
- **反汇编工具链**（本轮自建，/tmp 清理后需重建）：Python class 解析器 `/home/z/psd11/classdump.py`（修复了 Code 属性头 u4 多读的 bug）；原版类按 HTTP range 从 piston-data 拉（`/home/z/psd11/vanilla_fetch.py`）；MTR 4.0.5 jar 同法从 Modrinth CDN 拉（`/home/z/psd11/mr_fetch.py`）；MTR 源码直接从 GitHub `Minecraft-Transit-Railway/Minecraft-Transit-Railway` master 取（`fabric/src/main/java/org/mtr/mod/render/*.java`）；Yarn 映射 `maven.fabricmc.net` 的 tiny.gz。
- **关键 Yarn/intermediary 对照**（1.20.1）：`RenderLayer=fkf(class_1921)`、`RenderPhase=fke(class_4668)`、`TextRenderer=eov(class_327)`、`TextLayerType: field_33993=NORMAL, field_33994=SEE_THROUGH, field_33995=POLYGON_OFFSET`、`GlyphRenderer=err(class_382)`、`VertexConsumer=ein(class_4588)`、`BufferBuilder=eie(class_287)`、`RenderLayer.getText=m(fkf).r(method_23028)(Identifier)acq(class_2960)`、`getBeaconBeam=e(method_23592)`、`getEntityCutout=c(method_23576)`；phase：`fke.h=TRANSLUCENT`、`fke.c=NO_TRANSPARENCY`、`fke.ay=DepthTest("always",519)`、`fke.aD=WriteMask(true,false)`、`fke.as=Lightmap(true)`。

---

*文档生成于第十一轮修复（2026-11-10）。维护者：BugQuantumCore / Z.ai 会话记录。*
