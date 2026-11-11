# PSD 屏蔽门底色（黑/白背板）在 Iris 光影下不渲染 —— 完整复盘与接手文档

> **文档目的**：本 bug 已经历十三轮修复尝试（yanyang.4–13 + 2.3.0-1.0.0/1.0.1/1.0.2 + 第九至十二轮代码轮 + 第十三轮纯取证轮）。
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

### 2.1 可见文字在 IF + Iris 下的完整管线（第十三轮源码级取证，2026-11-11）

对 Iris 1.20.1 分支源码 + ImmediatelyFast v1.5.5 源码的完整取证，把上面这条可见路径逐环节展开为：

```
MTR drawText 的私有 immediate 构造
  → ImmediatelyFast `MixinVertexConsumerProvider` @Overwrite `Immediate.immediate(Map, BufferBuilder)`
  → 实际得到 IF 的 BatchableBufferSource（fallback=构造参数，仅作占位）
  → TextRenderer.draw → GlyphRenderer.render → source.getBuffer(RenderLayer.getText(字体图集))
  → BatchableBufferSource.getOrCreateBufferBuilder：text 层 areVerticesNotShared=true
     → 不走 fallback/预注册 map，而是 BufferBuilderPool.get() 池化新 buffer → begin(QUADS, P_C_T_L)
  → Iris `MixinBufferBuilder.iris$extendFormat`：POSITION_COLOR_TEX_LIGHTMAP(24B) → GLYPH(52B)
     （endVertex HEAD 注入回填 Normal/iris_Entity/mc_midTexCoord/at_tangent；
       QUADS 满 4 顶点触发 fillExtendedData → NormalHelper.computeFaceNormal 从顶点位置算面法线，非零）
  → immediate.draw()（MTR drawText 回调内当场同步 flush）
  → BatchableBufferSource.draw(layer)（isRenderingLevel=true → 跳过 renderWithExtendedVertexFormat 切换）
  → vanilla RenderLayer.MultiPhase.draw → startDrawing → ShaderPhase
     → RenderSystem.setShader(GameRenderer::getRendertypeTextShader)  ← ★求值时机就在此刻★
  → Iris `MixinGameRenderer.iris$overrideTextShader`（HEAD, cancellable）：
       ShadowRenderer.ACTIVE? → SHADOW_TEXT（阴影 pass）
       HandRenderer.isActive()? → HAND_TEXT
       isBlockEntities()? → TEXT_BE（BlockTrans）
       shouldOverrideShaders()（= isRenderingWorld && isMainBound，实体阶段 true）
         → ShaderKey.TEXT —— ExtendedShader，光影包程序 = ProgramId.EntitiesTrans
  → Complementary 无 entities_translucent override → fallback 到 gbuffers_water 程序
  → BufferRenderer.drawWithGlobalProgram(builtBuffer)：GLYPH 格式绑属性 → 绘制 ✓ 可见
```

**关键结论**：ShaderKey.TEXT 的三个定义（Iris `ShaderKey.java`）：
- `ProgramId.EntitiesTrans`（**不是 gbuffers_textured**——分析 Complementary `gbuffers_textured.glsl` 对 text 问题是错方向）
- `AlphaTests.ONE_TENTH_ALPHA`（alpha < 0.1 丢弃——纯白不透明 alpha=1.0 安全）
- `IrisVertexFormats.GLYPH`（52B 扩展格式，vsh 读 gl_Normal/gl_Color/gl_MultiTexCoord0，回填机制自洽）

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
| 第十一轮 | build.92 | 通道 N：`RenderLayer.getText(jsblock:white.png)`（NORMAL 层）+ 手写 glyph 顶点序 + 私有 immediate + 同步 flush + 相机侧门控（站台黑/轨道白各一四边形）+ 光影门控 + 窗口裁剪 | 不可见（**任何光影包**；日志证实 `channel N active` 出现、无异常——手写 quad 路径完整执行却不渲染） |
| 第十二轮 | build.93/94 | **通道 N2**：`TextRenderer.draw(单字符W, NORMAL, backgroundColor=同色)` 字形管线本体（同色字形 quad + 背景 quad 双程序冗余）+ 非均匀矩阵拉伸铺满窗口 + 双侧分层调度（站台黑→LIGHT 队列最先 / 轨道白→TEXT 队列最后）+ 专用 BufferBuilder + 光影门控 | **不可见**（build.94 实测 2026-11-11：三条面包屑齐现、无异常；含两欢开关光影对照） |
| **第十四轮** | **本版** | **通道 P：`holder.drawText(U+2588 全块单字符, …)` —— TextWrapper.drawTransformed 语句级逐句复刻（同队列/同 "" key/持有者本体 scale+drawText，零私有构造、零原生类）+ 双侧（站台黑先提交/轨道白后提交）+ 橙/白 6px 诊断条纹（一次实测三分：路径/色彩/已修复）** | **待实测** |

**矩阵透视**（第十四轮修订）：截至第十三轮的排除法已将变量收敛到一个从未被测试过的组合：**「由 TextWrapper 的语句序列在队列持有者本体上提交的 drawText」**——即字面意义上的可见路径本身。历轮失败载体全部在这件事上偏离：深度测试队列消费者（缓冲 OPAQUE 段——车辆证明其在开阔空间可见、玻璃后不可见）、SEE_THROUGH 程序对、手写 quad（哪怕在 NORMAL 层上）、甚至真 TextRenderer.draw 但经我们的私有 immediate / 借用矩阵 / LIGHT 队列提交。第十四轮不再模仿，直接复用：与可见文字共享调度、持有者、scale 调用序列与 drawText 方法本身，仅字符内容（单个 U+2588 全块）、颜色（黑/白）与拉伸系数不同；橙/白诊断条纹用可见文字的原色在同一路径上把「路径失败 / 色彩失败 / 成功」三个结局在一次实测中分开。

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

## 5.5 第十二轮实现要点（`drawBackingGlyph2`，通道 N2）

1. **一次调用两个程序**：`TextRenderer.draw("W", 0, 0, color, false, model, immediate, TextLayerType.NORMAL /*field_33993*/, color /*backgroundColor=同色*/, MAX_RENDER_LIGHT)`——字形 quad 走 `rendertype_text`、Drawer 背景矩形走 `rendertype_text_background`（nametag 程序），同色冗余承载。1.19.3 以下层参数退化为布尔 `false`（= NORMAL）。
2. **单字符铺满窗口**：字符画在窗口左上，非均匀缩放 `sx = wPx/(advance+1)`、`sy = hPx/10`（Drawer 背景固定 `(startX−1 … startX+advance) × (y−1 … y+9)` 像素，advance 经 `getWidth` 运行时实测），预平移 `(1,1)` 抵消 −1px 边距使背景与窗口逐像素对齐。96px/block 画布（`PIDSPresetBase.BASE_SCALE`）。
3. **双侧分层调度**：站台半（黑）→ `scheduleRender(QueuedRenderLayer.LIGHT, …)` 于 `driveScript` 之前（ordinal 0 最先 flush、同队列 FIFO 先于脚本图片 → 图片/文字画于其上，**永不遮挡图片**——修复第十一轮 TEXT 层四边形会盖住图片/形状的隐患）；轨道半（白）→ `scheduleRender(QueuedRenderLayer.TEXT, …)` 于 `replayScript` 之后（TEXT 共享 `""` 列表 FIFO 最后 flush → 纯白面板盖住轨道侧镜像内容）。两半回调内相机侧门控（`along ≥ 0.25` 判轨道侧），每视图恰一半生效。
4. **专用 BufferBuilder**（`N2_BUFFER`，1024 字节静态）：**不可**用 Tessellator 单例——LIGHT 队列 keyed 预热在队列持有者 immediate 上以同一单例开 batch，私有 immediate 共用会在 `begin()` 冲突（第十一轮通道 N 用 Tessellator 单例安全只因 TEXT 队列无预热）。
5. **`backing` transform 提升到 `!shadowPass` 块外**：轨道半的调度点在 `replayScript` 之后（块外），与站台半共享同一 `backing` 平面变换。
6. **MTR 队列结构**（MainRenderer 源码实证）：`Object2ObjectArrayMap`（fastutil，**插入序**）→ 同 layer 内 key 按首次 schedule 顺序、每 key 列表 FIFO；无 key 版本实际以 `new Identifier("")` 入队——「同 key FIFO」是所有顺序保证的基础。

---

## 5.6 第十三轮取证要点（纯取证轮，零代码变更，2026-11-11）

本日对 Iris 1.20.1 分支与 ImmediatelyFast v1.5.5 的源码取证结论（源码文件已拉至 `/home/z/psd12/`）：

1. **通道 N2 与可见文字的路径已逐环节对齐**（见 §2.1）——私有 immediate、TextRenderer.draw、字形机器、字体图集 UV、同色背景、当场 flush、IF 池化 buffer、实体阶段求值的同一 ExtendedShader(TEXT)。理论上 build.93（第十二轮产物）与可见文字**零差异**。
2. **「世界主 immediate 直写 + endBatch flush」方案已否决**（本日一度实施后回滚）：IF `BatchableBufferSource.getLayerOrder()` 把不透明非 text 层排 order 0、text 层（minecraft 图集）order 2、translucent 层排 100_000_000+——世界主源 endBatch 时**脚本图片（LIGHT 层）会先画、黑底（text 层）后画 → 黑底盖图**，round 11 隐患复活；且该 flush 路径在用户栈上无可见先例（MTR 文字从不进世界主源）。
3. **IF 的 text 层永远写池化 buffer**（`areVerticesNotShared=true` → `BufferBuilderPool.get()`）——私有 immediate 的 fallback 构造参数（N2_BUFFER 或 Tessellator 单例）在 IF 下**无实质作用**；纯 Iris 下各自独立 fallback buffer 互不冲突。N2_BUFFER 的存在是纯 Iris 下的防冲突正确选择（LIGHT 队列预热不碰它）。
4. **Iris MixinBufferBuilder 的 normal 回填**在 QUADS 模式满 4 顶点时自动触发，从顶点位置计算面法线（非零、非 NaN）——「normal 为零导致 NaN 剔除」的假说被否定（机制自洽，UV0@16 与 PolygonView 偏移对齐验证无误）。
5. **Complementary 取证**（r5.5.1，`/home/z/psd12/comp/`）：无 `rendertype_text` / `entities_translucent` override（后者 fallback gbuffers_water）；`gbuffers_textured.glsl`（粒子程序）的粒子检测逻辑不会丢弃纯白不透明像素（雪分支要求 `a<0.765`、水粒子要求蓝色占优、篝火烟雾要求 `a<0.99`）。

---

## 5.7 第十四轮实现要点（`drawBackingHolderText`，通道 P）

1. **逐句复刻，零自造环节**：回调内仅做相机侧门控（`along ≥ 0.25` 判轨道侧）后，依次执行 `transform.transform(graphicsHolderNew, offset)`（窗口左上角已烘焙进 SMT）→ `holder.scale(BASE_SCALE, BASE_SCALE, BASE_SCALE)` → `holder.scale(sx, sy, 1)` → `holder.drawText(BACKING_GLYPH_CHAR, 0, 0, color, false, MAX_RENDER_LIGHT)` → `pop`——与 `TextWrapper.drawTransformed`（可见文字的提交代码）语句一一对应；私有 immediate / 矩阵借用 / `createInstanceSafe` 全部不再出现，drawText 内部的一切（私有 immediate 构造、NORMAL 层、字体图集、同步 flush）由 MTR 自己的代码完成。
2. **U+2588 全块单字符**：字形本身就是实心矩形；advance 与 9px 行高运行时实测（`GraphicsHolder.getTextWidth`），非均匀拉伸铺满派生透明窗口。视觉为纯色块，无文字图案（用户约束为视觉约束）。源码中以 `"\u2588"` 转义书写，编码免疫。
3. **双侧调度**：站台半（黑）调度于 `replayScript` 之前（TEXT 队列 "" 列表 FIFO，黑底先落地、脚本文本后画其上）；轨道半（白）调度于 `replayScript` 之后（列表末尾最后 flush，白板盖住镜像内容）。
4. **诊断条纹**（仅站台半，本版临时）：顶边 6px 橙（`0xFC9700`，可见到站文字原色）+ 底边 6px 白，同一语句序列、同一拉伸机制——一次实测三分定位（见 §6）。
5. **异常安全**：drawText 链以 push/pop 深度计数器 + finally 兜底（泄漏的姿态会污染同帧后续所有绘制）；几何退化（advance=0 / 窗口空）显式 WARN 而非假面包屑（第十二轮的教训：面包屑在 if 块外，draw 被跳过也会记 active）。
6. **通道 N / N2 整体移除**（实机证伪）；载体 B 原样保留。新通道零原生 Minecraft 类引用（全部走 MTR 映射层 API），六个 MC 版本 × 双加载器无 `#if` 直编。

---

## 6. 本版（第十四轮）验证流程（给用户 / 测试者）

1. 下载 Complete Build 产物（版本号 `2.3.0-1.0.0-build.95`，构建自本提交）。
2. **无光影**：观感不变（载体 B 独撑；`channel P` 面包屑**不应**出现）。
3. **开 Iris + ComplementaryUnbound r5.5.1**（用户主环境），站台侧看绑定脚本的屏蔽门玻璃，回报三选一：
   - ① **黑底、顶部橙条、底部白条全部出现** → 已修复（条纹为诊断件，下版移除）；`latest.log` 应有 `channel P active (holder drawText, platform half)` 与 metrics 行。
   - ② **只有橙条与白条、中间黑底仍缺** → 路径已通、**纯黑被丢弃**（色彩问题实锤；下轮按 §7.1-P 修色彩）；请附 `latest.log`。
   - ③ **三者全不可见、只有橙色到站文字** → 路径仍有未知差异（按 §7.0-14 续诊）；请附光影包名 + `latest.log`。
4. **轨道侧**（乘车回头看）：应见纯白面板（`channel P active (… track half)`）。
5. 性能：应无可感知帧率变化（每段每帧每侧至多 3 次 drawText）。

---

## 7. 若本版仍不可见——后续候选与诊断

### 7.0-14 第十四轮之后的形势判断（2026-11-11）

先明确两个本轮新确立的现场事实（改写了问题模型）：

- **事实 A：用户的 PSD 脚本是纯文字脚本**（内置 PIDS 1A，`pids_1a.js` 只提交 `TextWrapper` 文字）——所以十三轮以来「透过窗口可见」的从来只有 drawText 输出；脚本图片（LIGHT 队列 beacon-beam、缓冲 OPAQUE 段）在用户光影下**从未被测试过**。
- **事实 B：MTR 车辆与载体 B 走同一条缓冲 OPAQUE 段管线**（Iris 1.7.6 `iris-batched-entity-rendering`：`MixinRenderBuffers` 把世界实体源换成 `FullyBufferedMultiBufferSource`，`MixinLevelRenderer` 在 translucent 相位点按 `TransparencyType` 分批 flush）——车辆在开阔空间可见、载体 B 在玻璃后不可见 ⇒ **丢弃发生在「半透明玻璃面板之后的实体阶段内容」这一维度上**，而非某条具体管线。

按 §6 的三选一回报续诊：

- **结局②（条纹可见、黑底缺失）——色彩问题实锤**：同路径同语句，唯一差异是 `0xFF000000` vs `0xFC9700/0xFFFFFFFF`。候选修法（按序）：P-1 直接改用非纯黑（`0xFF0A0A0A` 起步试探，或深灰）——若有效则是光影包对纯黑像素的着色器侧处理；P-2 用 RenderDoc/截帧定位 gbuffers_entities 对纯黑的实际输出；P-3 换光影包对照（BSL/Photon）确认是否 Complementary 特有。
- **结局③（三者全不可见）——路径仍有未知差异**：通道 P 与可见文字的剩余差异只有字符内容、拉伸系数与提交者（我们的回调 vs replayScript 的回调，同队列同 key）。候选（按序）：P-4 用**完全相同参数**在两个回调里各画一条对照条纹（同字符、同色、同尺度——隔离「提交者」变量，需临时改代码）；P-5 Iris `iris.debug` / RenderDoc 截帧确认字形是否进入 draw call；P-6 修改 mtrscripting 的 TextWrapper 传 `backgroundColor`（把脚本文本本身带背景色——终极候选，观感与文字绑定需用户同意）；P-7 无 IF + 不同光影包矩阵（IF 非前置依赖，但其在场时接管 immediate 工厂，无 IF 复测可洗清最后一层变量）。
- **结局①（全部可见）**：移除诊断条纹，发布。

### 7.1 先查日志分流（本版）

- **无 `channel P active` 且无 `failed once`/`degenerate`**：回调没跑——绑定/驱动层问题（`PSDClientBindings`、`MainRendererPSDMixin`），或光影门控误判（`ShaderDetector`）。
- **有 `failed once: <异常>`**：按栈修。
- **有 `degenerate: window …px, U+2588 advance N — nothing drawn`**：字体包未提供全块字形或窗口推导退化——把 advance 值与窗口尺寸附在报告里（advance=0 → 换字符；窗口 0×N → 窗口推导问题）。
- **`channel P active` 两半都出现且无异常、仍不可见** → 进入 7.0-14。

### 7.2 剩余变量：纹理 / 程序组合

（第十三轮取证后注：通道 N2 已用字体图集 + 字形 UV + TextRenderer.draw，本节 A 候选已被 N2 覆盖；保留供通道 N 或其他变体参考。）通道 N 与可见文字的唯一剩余差异是 **Sampler0 绑的是我们的白纹理而非字体图集页**。个别光影包的 `rendertype_text` 补丁可能假定纹理是字体图集（尺寸/alpha 语义）。候选：

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
- 「getText 带深度测试所以不可见」的假设（实证②证伪：两个 text 层都是 GL_ALWAYS、只写颜色）。
- **手写 quad 于 NORMAL text 层 + glyph 顶点序 + 私有 immediate 同步 flush**（第十一轮通道 N，build.92）——日志证实完整执行、任何光影均不可见。
- **真 `TextRenderer.draw` 字形机器 + 同色背景，经我们的私有 immediate + 借用矩阵提交于 LIGHT 队列**（第十二轮通道 N2，build.93/94）——build.94 三面包屑齐现、无异常、两欢开关光影对照后仍不可见。
- **世界主 immediate 直写 + endBatch flush**（第十三轮方案，实施后回滚）——IF `getLayerOrder` 令 endBatch 时不透明 LIGHT 层先画、text 层后画 → 黑底盖图（三明治坍塌）；且无可见先例。**不要再走此方向**。
- **ImmediatelyFast 是唯一根因的假设**（旧通道矩阵下卸载复测仍不显示）——IF 也非本模组及其上游的前置依赖，修复不得依赖其存在或缺失；其接管 immediate 工厂的效应已被第十三轮取证纳入模型（对 drawText 与我们的构造无差别）。

---

## 8. 构建 / 部署备忘（本仓库工具链）

- **版本**：`gradle.properties` 的 `mod_version=2.3.0-1.0.0`；Complete Build 产物版本号 = `<基础版本>-build.<github.run_number>`（例：`2.3.0-1.0.0-build.91`）。Release Build 全平台全量发布（勿动）；Complete Build 仅 3 平台原生桥（Win x64 / Linux x64 / Android ARM64，按用户 2026-11-10 要求精简）。
- **提交**：本机 git push 网络不通——用 Git Data API 三步提交（参见 `/home/z/my-project/push_round12.py`：GET 当前 HEAD → POST blob/tree/commit → PATCH ref）。token 在 `/home/z/.gh/token`（2026-11-11 更新）。
- **触发构建**：workflow_dispatch `Complete Build`，入参只传 `{"mod-version": "2.3.0-1.0.0"}`（**不要**传 retention-days 等额外字段，会 422）。
- **监视**：`/home/z/my-project/scripts/watch_build_run.py <runId>`；构建产物在 run artifacts。
- **反汇编工具链**（本轮自建，/tmp 清理后需重建）：Python class 解析器 `/home/z/psd11/classdump.py`（修复了 Code 属性头 u4 多读的 bug）；原版类按 HTTP range 从 piston-data 拉（`/home/z/psd11/vanilla_fetch.py`）；MTR 4.0.5 jar 同法从 Modrinth CDN 拉（`/home/z/psd11/mr_fetch.py`）；MTR 源码直接从 GitHub `Minecraft-Transit-Railway/Minecraft-Transit-Railway` master 取（`fabric/src/main/java/org/mtr/mod/render/*.java`）；Yarn 映射 `maven.fabricmc.net` 的 tiny.gz。
- **第十三轮新增源码库存**（`/home/z/psd12/`）：Iris 1.20.1 分支关键源码（`irismixin_MixinGameRenderer.java`、`iris_IrisRenderingPipeline.java`、`ShaderKey/ShaderMap/ShaderAccess`、`MixinBufferBuilder/IrisVertexFormats/BufferBuilderPolygonView/NormalHelper` 等）；ImmediatelyFast v1.5.5 关键源码（`if_MixinVertexConsumerProvider.java`、`if_BatchableBufferSource.java`、`if_IrisCompat.java`）；ComplementaryUnbound r5.5.1 解包（`comp/shaders/`）；Iris 1.7.6 jar（`iris.jar`，取自 Modrinth CDN，大文件需 curl 分段拉取）。
- **关键 Yarn/intermediary 对照**（1.20.1）：`RenderLayer=fkf(class_1921)`、`RenderPhase=fke(class_4668)`、`TextRenderer=eov(class_327)`、`TextLayerType: field_33993=NORMAL, field_33994=SEE_THROUGH, field_33995=POLYGON_OFFSET`、`GlyphRenderer=err(class_382)`、`VertexConsumer=ein(class_4588)`、`BufferBuilder=eie(class_287)`、`RenderLayer.getText=m(fkf).r(method_23028)(Identifier)acq(class_2960)`、`getBeaconBeam=e(method_23592)`、`getEntityCutout=c(method_23576)`；phase：`fke.h=TRANSLUCENT`、`fke.c=NO_TRANSPARENCY`、`fke.ay=DepthTest("always",519)`、`fke.aD=WriteMask(true,false)`、`fke.as=Lightmap(true)`。

---

*文档生成于第十一轮修复（2026-11-10）；第十二轮（2026-11-11）、第十三轮取证（2026-11-11）与第十四轮（2026-11-11，通道 P + 诊断条纹）更新。维护者：BugQuantumCore / Z.ai 会话记录。*
