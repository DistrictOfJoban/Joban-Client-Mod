# PSD 屏蔽门底色（黑/白背板）在 Iris 光影下不渲染 —— 完整复盘与接手文档

> **文档目的**：本 bug 已经历十七轮修复尝试（yanyang.4–13 + 2.3.0-1.0.0/1.0.1/1.0.2 + 第九至十四轮代码轮 + 第十三轮纯取证轮 + 第十五轮 + 第十六轮（第二轮取证 + 通道 R）+ 第十七轮（通道 S：回放注入））。
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

### 2.2 第二轮取证（2026-11-12，原版 client.jar 字节码，工具链 /home/z/psd16）——通道 Q 证伪后的三项决定性结论

对第十五轮 build.96 实测结果（文字可见、U+2588 行含同色橙条纹全部不可见、面包屑+metrics 齐现）做变量消除法收束后，对 Mojang 官方 1.20.1 client.jar 重建反汇编工具链（Python class 解析器 `classdump.py` + ProGuard client.txt 混淆名对照；前身 /home/z/psd11 已随沙盒重置丢失），三项结论改写了问题模型：

1. **字形内容是第十五轮唯一幸存变量 ⇒ 用户的字体包（Source Han Sans Font Pack 1.20）提供的 U+2588 栅格化为全透明**。（**第十七轮修订：已被第十六轮实测证伪**——build.97 的字形探针行（「底色R16测试」，与可见到站文字同字形族、同色、同 wrapper 机器）同样不可见，而脚本自己的 CJK 行始终可见 ⇒ 字形内容不是变量；第十五/六轮的行死于它们与脚本行的另外两项差异：**+0.45/16 底板平面**与**从 renderAll 直接 `run()` 的提交方式**。以下推断链保留作历史记录：）推导链：① vanilla 对缺失码点渲染 `SpecialGlyphs.MISSING` —— 逐像素反汇编证实是 **5×8 空心边框（边框像素 0xFFFFFFFF 不透明、内部全透明）**，即「可见的豆腐框」而非空白，且 advance = 6；② 通道 Q 的 metrics 实测 advance = 9（字形**存在**、advance 正常）；③ CJK 文字（同字体栈）可见；④ ⇒ U+2588 字形「存在但画不出」——位图 chars 网格行错位或 TTF 空轮廓，字体包侧的栅格化损坏。这同时解释第十四（holder.drawText(U+2588)）与第十五（wrapper(U+2588×29)）两代载体：**只要内容是 U+2588，任何管线复刻都救不了**。（**证伪记录**：若此理论成立，第十六轮的真实字形探针行应当可见——实测仍不可见。）
2. **背景矩形（backgroundColor）机制 = 字形免疫的实心四边形**（`Font$StringRenderOutput.finish` 反汇编）：每行文字的背景是 `BakedGlyph$Effect` 四边形，使用 `FontSet.whiteGlyph()` —— **`SpecialGlyphs.WHITE`（5×8 每像素 0xFFFFFFFF）由 vanilla 自己合成并烘焙进字体图集**（任何字体包不参与其栅格化）——的 UV 区域，经 `whiteGlyph.renderType(NORMAL)` 与字形**完全相同的 RenderLayer / 图集纹理 / 程序 / 绘制调用**画出；矩形尺寸只由字符串 advance 决定（Drawer 按字符位置连续绘制、**空格同样覆盖**——玩家名牌背景即其每日证明）。矩形 y 范围 (y−1 … y+10)（11px、比行高大 2px——行距 9px 时天然重叠成无缝色场）。
3. **第十一轮「实证②」部分证伪**：`RenderType.text`（lambda `C`，字符串名 "text"）**没有 DepthTest 相位、没有 WriteMask 相位**（深度测试/写掩码继承外部状态——实体阶段即 LEQUAL + 写深度）；带 `DepthTest "always"(fke.ay)` + `COLOR_WRITE(fke.aD)` 的是 **SEE_THROUGH 变体**（lambda `x` "text_intensity_see_through" / `y` "text_see_through"）——第十一轮把两个 SEE_THROUGH lambda 的相位链误读成了 getText/getTextSeeThrough。无光影下文字在玻璃后可见的真正机制是**绘制顺序**：实体阶段（MainRenderer 是实体渲染器）文字先画、半透明地形（玻璃）后画并叠加混合——文字被玻璃「罩染」而非穿透深度。该顺序对 TEXT 队列里先于脚本回放提交的一切内容同样成立。

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
| **第十四轮** | **build.95** | **通道 P：`holder.drawText(U+2588 全块单字符, …)` —— TextWrapper.drawTransformed 语句级逐句复刻（同队列/同 "" key/持有者本体 scale+drawText，零私有构造、零原生类）+ 双侧（站台黑先提交/轨道白后提交）+ 橙/白 6px 诊断条纹（一次实测三分：路径/色彩/已修复）** | **不可见**（实机报告 2026-11-12，附两份 latest.log——**未装 / 已装 ImmediatelyFast 各一，均不显示**；日志证实 `channel P active (holder drawText, platform half)` + metrics 行出现、无 `failed once`、无 `degenerate`——回调完整执行；用户未到轨道侧，track half 面包屑未出现属预期） |
| **第十五轮** | **build.96** | **通道 Q：底板就是脚本文本——真实 `TextWrapper` 行（`TextWrapper.create().text(U+2588×N).pos(0,y).size(w,h).stretchXY().color(色).run(world, SMT.copy(), facing, MAX_RENDER_LIGHT)`）经 `PIDSDrawCall.run → drawTransformed → scheduleRender(TEXT)` 本体机具，从 renderAll 头部提交（与脚本 replay 同一快照）** + 四变量一次性全消（String 重载/无样式字体 → MutableText+`mtr:mtr` 样式；28×16 拉伸单字形 → 未拉伸文本行；纯黑 0xFF000000 → 近黑 0xFF0A0A0A；手写回调 → wrapper 本体）+ 调度时玩家侧门控（避开 Iris 阴影 pass 队列快照陷阱）+ 首末行橙/白诊断条纹（第十三轮三分法延续） | **不可见**（实机报告 2026-11-12：文字可见、底色与橙白条纹均不可见；日志证实面包屑 + metrics（window 255x150px, advance 9, mtr font OFF, rows 17 x cols 29）齐现、无异常——当时的推断是字形内容成为唯一幸存差异（U+2588 栅格化全透明，见 §2.2）；**第十六轮探针行实测后此推断被证伪**，真凶是 +0.45/16 平面或直接提交本身） |
| **第十六轮** | **build.97** | **通道 R：底板是脚本文本自己的背景矩形——真实 `TextWrapper` 行 `text(空格×N).fontMC().color(色).jcm$backgroundColor(色)`，`TextWrapper.drawText` 检测到背景色后直调真 `TextRenderer.draw(String,…,NORMAL,backgroundColor,light)`（N2 的可编译直调形态）+ 私有 immediate（Tessellator 单例，MTR drawText 同款构造）+ 回调内当场 flush + wrapper 本体 TEXT 队列调度（Q 的时机原样）+ 顶部真实字符字形探针行（「底色R16测试」橙、脚本 1.725 缩放——Q 谱系仅换字形）+ 首橙/末白诊断条纹 + 空格 advance 归零时数字回退** | **不可见，结局③**（实机报告 2026-11-12：探针行、矩形、条纹全部不可见，只有橙色到站文字；日志证实 `channel R active (text-background rows, platform half)` + metrics（window 255x150px, fill 'space' advance 2, rows 15 x cols 129）齐现、无异常、无 degenerate——**字形探针以真实可见字形族死亡，彻底证伪 U+2588 栅格化理论**；探针行与可见脚本行的差异只剩两项：+0.45/16 平面 + 直接提交） |
| **第十七轮** | **本版** | **通道 S：底板行加入脚本自己的回放列表——`buildBackingRows`（renderGlass 在渲染线程构建：探针行 + 首橙/末白/中间近黑矩形行，字段与 R16 完全一致）+ `driveScript` 在捕获提交回调内（`saveRenderCalls` 的 takeFrom 交换之后、脚本 executor 线程上）轻 `ScriptRenderManager.jcm$inject(row, head)` 把行拼进实例回放列表（站台半插头、轨道半插尾）——底板行与脚本文本行**同一个 invoke 循环、同一次 `TransformCapturePool.acquire(scriptSide)` 快照、同一个 +0.25/16 平面、同一套 `run()→drawTransformed→scheduleRender(TEXT)` 提交**；平台/轨道门控在构建时（渲染线程）求值；异常安全；诊断件原样（探针改「底色R17测试」）** | **待实测** |

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

## 5.7 第十四轮实现要点（`drawBackingHolderText`，通道 P；build.95 实测证伪）

1. **逐句复刻，零自造环节**：回调内仅做相机侧门控（`along ≥ 0.25` 判轨道侧）后，依次执行 `transform.transform(graphicsHolderNew, offset)`（窗口左上角已烘焙进 SMT）→ `holder.scale(BASE_SCALE, BASE_SCALE, BASE_SCALE)` → `holder.scale(sx, sy, 1)` → `holder.drawText(BACKING_GLYPH_CHAR, 0, 0, color, false, MAX_RENDER_LIGHT)` → `pop`——与 `TextWrapper.drawTransformed`（可见文字的提交代码）语句一一对应；私有 immediate / 矩阵借用 / `createInstanceSafe` 全部不再出现，drawText 内部的一切（私有 immediate 构造、NORMAL 层、字体图集、同步 flush）由 MTR 自己的代码完成。
2. **U+2588 全块单字符**：字形本身就是实心矩形；advance 与 9px 行高运行时实测（`GraphicsHolder.getTextWidth`），非均匀拉伸铺满派生透明窗口。视觉为纯色块，无文字图案（用户约束为视觉约束）。源码中以 `"\u2588"` 转义书写，编码免疫。
3. **双侧调度**：站台半（黑）调度于 `replayScript` 之前（TEXT 队列 "" 列表 FIFO，黑底先落地、脚本文本后画其上）；轨道半（白）调度于 `replayScript` 之后（列表末尾最后 flush，白板盖住镜像内容）。
4. **诊断条纹**（仅站台半，本版临时）：顶边 6px 橙（`0xFC9700`，可见到站文字原色）+ 底边 6px 白，同一语句序列、同一拉伸机制——一次实测三分定位（见 §6）。
5. **异常安全**：drawText 链以 push/pop 深度计数器 + finally 兜底（泄漏的姿态会污染同帧后续所有绘制）；几何退化（advance=0 / 窗口空）显式 WARN 而非假面包屑（第十二轮的教训：面包屑在 if 块外，draw 被跳过也会记 active）。
6. **通道 N / N2 整体移除**（实机证伪）；载体 B 原样保留。新通道零原生 Minecraft 类引用（全部走 MTR 映射层 API），六个 MC 版本 × 双加载器无 `#if` 直编。
7. **实测结果（2026-11-12，build.95）**：**完整执行、依然不可见**（未装 / 已装 ImmediatelyFast 均测，两份日志）。由此通道 P 与可见文字的剩余差异收敛为**四项**：① 重载与字体——`drawText(String, …)`（原版默认字体）vs 可见文字的 `drawText(MutableText, …)` + `TextUtil.withFontStyle(mtr:mtr)` 样式（MTR 自带 **TTF 字体**：`mtr:mtr` 首个 provider 为 `noto-sans-semibold.ttf`，其 cmap **含 U+2588**，2026-11-12 取证实测——MTR 仓库 `fabric/src/main/font_template.json` 证实 mtr:mtr = ttf provider（Noto Sans + Noto Serif CJK TC）；`useMTRFont` 配置无默认值（false），开着则可见文字骑 TTF 字形）；② 几何——单字形 ~28×16 非均匀拉伸 vs 未拉伸文本行；③ 色彩——纯黑 vs 可见橙；④ 提交者——手写回调体 vs wrapper 本体。第十五轮将四项一次性全消。

---

## 5.8 第十五轮实现要点（`drawBackingWrapperText`，通道 Q）

1. **底板就是脚本文本**：`TextWrapper.create().text("\u2588"×N).pos(0, y).size(wPx, rowH).stretchXY().color(色)` 后直接 `run(world, transform.copy(), facing, MAX_RENDER_LIGHT)`——`PIDSDrawCall.run` 本体链（super.run → SMT 追加 `jcm$position`（BASE_SCALE+translate）→ `drawTransformed`（真正的 `scheduleRender(TEXT)` + 回调））与 replayScript **同一个类、同一个方法链**，仅字段值不同；格式化（`getFormattedText` → `TextUtil.withFontStyle(mtr:mtr)`）在 drain 时由 wrapper 自己完成，与可见文字逐字节同源。
2. **从 renderAll 头部提交（关键时机）**：不能在队列回调内部再 `run()` wrapper——**Iris 下 `MainRenderer.render` 每帧跑两次（阴影 pass + 主 pass），各自快照并清空 `RENDERS`**（`MainRendererPSDMixin` 在两处 HEAD 都注入）；回调内调度的 wrapper 行会落进下一帧**阴影 pass** 的快照、被阴影图消费掉、永不上屏。故相机侧门控提前到**调度时**用玩家坐标求值（`along = dot(player − blockCenter, facing) ≥ 0.25` 判轨道侧）；玩家坐标与相机仅第三人称跨栏构图时有偏差（罕见、cosmetic——且可见文字因同层继承剔除态本就无轨道侧视图）。
3. **未拉伸行 + wrapper 自带 fit**：每行 `ceil(wPx/advance)` 个全块字符，`stretchXY()` 仅裁掉 ≤1 字符溢出（scaleX ∈ (8/9, 1]），末行剩余高度被 stretchXY 的 y 分量压缩——零手写缩放；advance 以 wrapper 同源样式一次性实测（`TextUtil.withFont(...)` 后的 `getTextWidth`），mtr 字体开/关均与实际渲染一致。
4. **每行独立 SMT 副本**：`run()` 向传入的 SMT **追加** `jcm$position` 消费者（replay 路径为此给每个捕获调用发 `TransformCapturePool` 快照）——共享实例会累积每行平移；`transform.copy()` 每行一份（测试版开销；量产后可入池）。
5. **近黑填充 + 诊断条纹**：站台半首行橙（`0xFC9700`）/末行白/中间近黑（`0xFF0A0A0A`——若第十四轮的条纹其实可见而仅纯黑死亡，则近黑直接绕过该失败族，视觉仍为纯黑）；轨道半全白。三分诊断不变：全不可见 ⇒ 路径；仅条纹 ⇒ 色彩；全部可见 ⇒ 已修复。
6. **面包屑与 metrics**：`[JCM] PSD backing channel Q active (wrapper text rows, platform|track half)` + metrics 行（窗口尺寸 / advance（styled 与 vanilla 两个值）/ **mtr 字体开闭** / 行×列 / 条纹说明）——两个 advance 值与字体状态可直接判定 U+2588 实际由哪个字体族提供，为下一轮（如需）预留证据。

### 5.9 第十六轮实现要点（`buildBackingRows` 前身 `drawBackingTextRows` + `TextWrapper.jcm$backgroundColor`，通道 R——build.97 实测证伪）

1. **底板 = 脚本文本自己的背景矩形**：每行是真实 `TextWrapper`——`create().text(空格×N).fontMC().color(c).jcm$backgroundColor(c).pos(0,y).size(wPx,rowH).stretchXY()` 后 `run(world, transform.copy(), facing, MAX_RENDER_LIGHT)`。`TextWrapper.drawText` 检测到 `jcm$bgColor != 0` 时改走 `jcm$drawTextWithBackground`：回调持有者自己的矩阵（`NativeMatrixApplier.jcm$getMatrixStack()`）+ 私有 `VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer())`（MTR `drawText` 反汇编实证的同款构造；TEXT 队列无预热、单例无 batch 冲突；IF 下工厂被 @Overwrite、text 层走池化 buffer 与 drawText 无差别）+ 直调真 `TextRenderer.draw(String, x, y, color, shadow, model, immediate, NORMAL, backgroundColor, light)` + 回调内 `immediate.draw()` 当场 flush。`#if LOADER`/`#if MC_VERSION` 守卫逐字沿用第十二轮 N2 已在六版本双加载器上编译通过的形态（forge 侧 `Font.drawInBatch` + `MultiBufferSource.immediate(Tesselator.getInstance().getBuilder())`）。
2. **字形免疫**：空格字形栅格化为空（矩形照样画）；矩形由 vanilla 合成 `SpecialGlyphs.WHITE` × 背景色 ARGB 构成——字体包无法损坏它。填充字符 advance 以 `GraphicsHolder.getTextWidth(" ")` 一次性实测（与绘制同走原版默认字体、`fontMC()` 空样式，测量与绘制永不脱节）；advance < 1 时回退数字 "0" 并记录 metrics。
3. **行几何**：9px 行距 × ~11px 矩形高（y−1..y+10）→ 上下各重叠 ~2px 的无缝色场；列数 `ceil((wPx+2)/advance)`（+2 覆盖 Drawer 的 ±1px 边距），wrapper 自己的 stretchXY 裁 ≤1 字符溢出；末行压到剩余高度。
4. **诊断件**（站台半）：顶部一行**字形探针**（`"\u5E95\u8272R16\u6D4B\u8BD5"` 橙 0xFFFC9700、脚本自己的 1.725 行缩放——第十五轮通道 Q 谱系原样、仅换字形这一变量）；矩形行首条橙 / 末条白。三分：全可见 ⇒ 修复；仅探针可见 ⇒ 背景矩形路径死亡；全不可见 ⇒ 提交上下文问题（§7.0-17 预案）。
5. **调度与门控原样**：renderAll 头部提交、站台半先于 `replayScript`（同 `""` key FIFO，脚本内容画于其上）、轨道半全白行后置（盖镜像）、调度时玩家侧门控（`along ≥ 0.25`）、`ShaderDetector` 光影门控、载体 B 与无光影观感零改动。
6. **面包屑**：`[JCM] PSD backing channel R active (text-background rows, platform|track half)` + metrics（窗口 / 填充字符与 advance / 行×列 / 探针与条纹说明）。**实测（2026-11-12，build.97）：结局③**——探针行（真实字形）与矩形、条纹全部不可见；面包屑 + metrics 齐现、无异常。⇒字形内容被排除，定罪剩余两项差异（+0.45/16 平面、直接提交）；`TextWrapper.jcm$backgroundColor` 背景矩形机制本身仍未被单独验证（第十七轮继续携带，作三选一诊断的一分支）。

### 5.10 第十七轮实现要点（`buildBackingRows` + `injectBackingRows` + `ScriptRenderManager.jcm$inject`，通道 S）

1. **底板行 = 脚本自己的回放列表成员**：`renderGlass` 在渲染线程调用 `buildBackingRows`（原 `drawBackingTextRows` 的纯构建形态——不再 `run()`、不再 `transform.copy()`，返回 `ObjectArrayList<RenderDrawCall<?>>`；门控/几何/填充字符/探针/条纹逻辑与 R16 逐字一致，探针改「底色R17测试」）；两半各建一份，另一侧为 null（玩家侧门控在构建时求值）。
2. **捕获提交回调内注入**：`driveScript` 新增两参重载，在 `invokeRenderFunctions` 的完成回调里（脚本 executor 线程，`saveRenderCalls` 的 takeFrom 交换**之后**）调 `injectBackingRows`：站台半倒序 `jcm$inject(row, head=true)` 插头（保持探针在首、矩形自上而下的构建序），轨道半顺序 append 尾。`ScriptRenderManager.jcm$inject` 为新增 `@ApiInternal synchronized` 方法（validate + add(0)/add），注入行与捕获行同受下一帧 takeFrom 的 release+清空管理（TextWrapper.release 对非池化实例为 no-op，GC 兜底），无累积。
3. **零上下文差异**：注入行走 `invoke` 的同一循环——每行各自的 `TransformCapturePool.acquire(scriptSide)`（+0.25/16 平面，与脚本文本同一变换链：anchor → rotateY → frameHeight → rotateZ → −0.5 → scriptZ → canvasLeft/Top → BASE_SCALE+pos）→ `run()` → `drawTransformed` → `scheduleRender(TEXT, "")`。与可见脚本文本行的差异只剩 wrapper 字段值（填充串/颜色/位置/尺寸）。
4. **时序安全**：脚本是 executor 异步执行（`invokeRenderFunctions` 提交任务，回调在任务尾部）；若任务在途，本帧 replay 重放的是上一次完成捕获的完整列表（含当时的注入行）——列表永远是「底板+脚本」的完整快照，不存在底板单飞或重复注入（takeFrom 先 release+clear 旧表再交接）。脚本 fail-cooldown（4s）期间回调不再跑，旧列表冻结重放——底板与文字一致地冻结。
5. **同平面共存**：底板矩形与脚本文本同在 +0.25/16、TEXT 队列 LEQUAL：站台半先 flush（写深度）→ 脚本文字同深度 LEQUAL 相等通过 → 画于其上；矩形行间 2px 重叠同色无缝；1px 上下越界被门框不透明部分深度剪裁（与脚本文字像素同等待遇）。脚本的 LIGHT 队列图片在 ordinal 0 先 flush，会被 TEXT 队列的底板盖住——但光影下图片本就不可见（十三轮实证），且通道 S 仅光影门控开启；无光影观感仍由载体 B + 深度分层独撑。
6. **原生脚本限制**：`parsedScript == null` 的纯 native 预设不走 JS 实例，无注入点（native replay 自有链路）——用户场景（内置 pids_1a.js，JS）不受影响；文档记录该限制。
7. **面包屑（改在注入点打）**：`[JCM] PSD backing channel S active (replay-injected rows, platform|track half)`（首次成功拼接时，executor 线程）+ metrics 行（渲染线程构建时拼好、随 platform half 首次注入时打出）；异常安全 try/catch + `failed once` WARN；degenerate（advance≤0）WARN 保留在构建侧。

---

## 6. 本版（第十七轮）验证流程（给用户 / 测试者）

1. 下载 Complete Build 产物（版本号 `2.3.0-1.0.0-build.98`，构建自本提交；下文面包屑均为 `channel S`）。
2. **无光影**：观感不变（载体 B 独撑；`channel S` 面包屑**不应**出现）。
3. **开 Iris + ComplementaryUnbound r5.5.1**（用户主环境），站台侧看绑定脚本的屏蔽门玻璃，回报三选一：
   - ① **黑底、顶部橙色条纹与「底色R17测试」橙色文字行、底部白条全部出现（到站文字照旧）** → 已修复（条纹与探针为诊断件，下版移除）；`latest.log` 应有 `channel S active (replay-injected rows, platform half)` 与 metrics 行（填充字符 / advance / 行×列）。
   - ② **只有「底色R17测试」文字行可见、黑底与橙白条纹仍缺** → 回放上下文已修复，死亡的是 `TextRenderer.draw` 的 backgroundColor 背景矩形分支（空格填充/Effect 四边形在该栈上不渲染）→ §7.0-18 预案 A（可见字形深色字符行）。
   - ③ **连「底色R17测试」文字行也不可见、只有橙色到站文字** → 注入行与脚本行的差异只剩 wrapper 字段值（填充串/颜色/位置）——「内容字符串本身被区别对待」的荒谬结论成立，或注入链路未生效（先查面包屑）→ §7.0-18 预案 B（RenderDoc 截帧）；请附 `latest.log`。
4. **轨道侧**（乘车回头看）：应见纯白面板（`channel S active (… track half)`）。
5. 性能：应无可感知帧率变化（每段每帧每侧 ~17 行 + 1 探针行，与脚本文字行同量级；若修复则下版池化 wrapper）。
6. **日志分流**：无 `channel S active` 且无 `failed once`/`degenerate` → 行未构建或未注入——检查无光影环境（预期不出现）、绑定与窗口推导（`PSDClientBindings`/`PSDWindowBounds`）、脚本实例是否存活（脚本报错会冻结回调，底板随之不发）；有 `failed once` → 附日志（注入异常，代码侧问题）。

---

## 7. 若本版仍不可见——后续候选与诊断

### 7.0-17 第十七轮（已执行，本版）：把底板行注入脚本的回放列表

原预案（预置于第十六轮后）已在本版实现并有两处落地修正：① 方法名落地为 `ScriptRenderManager.jcm$inject(RenderDrawCall, boolean atHead)`（比预案的 `jcm$prepend` 多了尾插，轨道半共用）；② 注入点不在 driveScript 主体（渲染线程）而在 `invokeRenderFunctions` 的**完成回调**内（脚本 executor 线程）——脚本捕获是异步任务，takeFrom 交换发生在任务尾部，注入必须紧跟其后才能落进本帧列表。底板行与脚本行同 invoke 循环、同 TransformCapturePool 快照、同 scriptSide（+0.25/16）平面回放，且**连平面差异一并消除**（预案原文只消除提交上下文；实测代码审阅发现第十六轮探针行的两项幸存差异是「平面 + 提交」，通道 S 一次全消）。若本版仍不可见（结局③），则只剩「wrapper 字段值」这一变量——见 §7.0-18。

### 7.0-18 第十八轮预案（2026-11-12，第十七轮后预置）

- **结局②（探针可见、矩形不可见）——预案 A，可见字形深色字符行**：背景矩形分支（空格 advance 驱动的 Effect 四边形）在该栈上不渲染。用**可见字形**铺底：填充字符换成与到站文字同族且视觉致密的字符（如全角「口」「■」需实测——U+2588 已死，勿再用），颜色近黑 0xFF0A0A0A，行距 9px 重叠铺满；或退一步用 `marquee` 机制。视觉约束（纯色块不带文字纹理感）需用户同意。同场可留一条背景矩形行做 A/B 对照，一次实测定位丢弃闻值（纯字形行可见 ⇒ Effect 四边形分支死；两者都不可见 ⇒ 空格串本身被优化——换字符即消）。
- **结局③（全部不可见）——预案 B，RenderDoc 截帧**：注入行与脚本行已零上下文差异，只剩字段值。「内容字符串本身被光影管线区别对待」无任何已知机制可解释（字形 UV/图集页/程序全同族）——必须截帧取证：`iris.debug` / RenderDoc 捕获一帧，过滤 `rendertype_text` 绘制调用，比对到站文字行与「底色R17测试」行的顶点数据、纹理绑定与程序 uniform。若截帧里注入行**根本不在** draw call 里 → 回放列表/调度侧还有未发现的过滤（检查 takeFrom 时序、instance 失效重建）；若在但被丢弃 → 着色器侧分支（Complementary 对特定颜色/uv/顶点属性的处理），此时逐字段克隆到站文字行的 wrapper 配置二分定位。
- **结局①（全部可见）**：移除条纹与探针、池化 wrapper、发布；补 README 版本表与 CHANGELOG。

### 7.0-16 第十六轮之后的形势判断（2026-11-12，预置）

通道 R 已把「字形栅格化」从底板中完全剥离（vanilla 合成白色区域 × 背景色）。若结局②（探针可见、矩形不可见）：字形路径在本上下文**能活**（通道 Q 之死确系 U+2588 栅格化），死的是 `TextRenderer.draw` 的 backgroundColor 分支——候选：① 背景矩形的 Effect 四边形在 Iris GLYPH 扩展格式下的 normal 回填/绕向问题（renderEffect 的顶点序与字形不同——`BakedGlyph.renderEffect` 反汇编在 /home/z/psd16 可复验）；② Complementary 的 EntitiesTrans→gbuffers_water 程序对「纯色不透明四边形」的分支处理（Q-3 RenderDoc / Q-5 换包对照可定位）；③ 退路：探针行证明「真实字符字形」在本上下文可见——第十七轮可直接用**深色字符行**做底色（视觉约束的最终妥协方案，需用户同意）或 §7.0-17 的注入变体。若结局①：移除条纹与探针、池化 wrapper 与 SMT 副本、发布。

### 7.0-15 第十五轮之后的形势判断（2026-11-12，预置）

通道 Q 已把可见文字与底板之间的**全部已知差异**消除（同 wrapper、同字体样式、同未拉伸几何、近黑色、同队列同时机同 key 提交）。若仍不可见（结局③），则说明差异不在提交侧，而在**内容本身**——脚本行与底板行唯一剩下的区别是**字符串内容与颜色**（脚本行 = 到站文字字符 + 橙色；底板行 = U+2588×N + 近黑/白/橙）。候选（按序）：

- **Q-1 字符内容 A/B**：底板行改用与脚本完全相同的字符集（如一行「讲淀黄庄」文字以近黑着色铺满——临时诊断版）——若可见则 U+2588 字形本身在该字体栈/光影下有问题（换字符：`█` → `▉`/`■`/全角空格底色）。
- **Q-2 色彩阶梯**（若结局②）：0x0A0A0A → 0x1E1E1E → 0x2A2A2A → 深灰蓝（PIDS 黑底常见色）逐步试探，定位丢弃阈值。
- **Q-3 RenderDoc / `iris.debug` 截帧**（P-5 延续）：确认底板行字形是否进入 draw call、被哪个程序消费、输出什么颜色。
- **Q-4 终极方案（P-6 延续）**：修改 `TextWrapper`/`PSDScriptContext`，让脚本自己的到站文字携带 `backgroundColor`（背景矩形随每个字形行自动画出——nametag 同机制，`rendertype_text_background` 程序）——观感与文字行绑定（底色只在有文字处出现），需用户同意。
- **Q-5 无 IF + 其他光影包矩阵**（P-7 延续）：BSL/Photon + Iris 复测，定位 Complementary 特有 vs Iris 通用。

若结局①（全部可见）：移除条纹行、池化 wrapper 与 SMT 副本、发布。

### 7.0-14 第十四轮之后的形势判断（2026-11-11，已被 build.95 实测取代）

先明确两个本轮新确立的现场事实（改写了问题模型）：

- **事实 A：用户的 PSD 脚本是纯文字脚本**（内置 PIDS 1A，`pids_1a.js` 只提交 `TextWrapper` 文字）——所以十三轮以来「透过窗口可见」的从来只有 drawText 输出；脚本图片（LIGHT 队列 beacon-beam、缓冲 OPAQUE 段）在用户光影下**从未被测试过**。
- **事实 B：MTR 车辆与载体 B 走同一条缓冲 OPAQUE 段管线**（Iris 1.7.6 `iris-batched-entity-rendering`：`MixinRenderBuffers` 把世界实体源换成 `FullyBufferedMultiBufferSource`，`MixinLevelRenderer` 在 translucent 相位点按 `TransparencyType` 分批 flush）——车辆在开阔空间可见、载体 B 在玻璃后不可见 ⇒ **丢弃发生在「半透明玻璃面板之后的实体阶段内容」这一维度上**，而非某条具体管线。

按 §6 的三选一回报续诊：

- **结局②（条纹可见、黑底缺失）——色彩问题实锤**：同路径同语句，唯一差异是 `0xFF000000` vs `0xFC9700/0xFFFFFFFF`。候选修法（按序）：P-1 直接改用非纯黑（`0xFF0A0A0A` 起步试探，或深灰）——若有效则是光影包对纯黑像素的着色器侧处理；P-2 用 RenderDoc/截帧定位 gbuffers_entities 对纯黑的实际输出；P-3 换光影包对照（BSL/Photon）确认是否 Complementary 特有。
- **结局③（三者全不可见）——路径仍有未知差异**：通道 P 与可见文字的剩余差异只有字符内容、拉伸系数与提交者（我们的回调 vs replayScript 的回调，同队列同 key）。候选（按序）：P-4 用**完全相同参数**在两个回调里各画一条对照条纹（同字符、同色、同尺度——隔离「提交者」变量，需临时改代码）；P-5 Iris `iris.debug` / RenderDoc 截帧确认字形是否进入 draw call；P-6 修改 mtrscripting 的 TextWrapper 传 `backgroundColor`（把脚本文本本身带背景色——终极候选，观感与文字绑定需用户同意）；P-7 无 IF + 不同光影包矩阵（IF 非前置依赖，但其在场时接管 immediate 工厂，无 IF 复测可洗清最后一层变量）。
- **结局①（全部可见）**：移除诊断条纹，发布。

### 7.1 先查日志分流（本版）

- **无 `channel S active` 且无 `degenerate`/`failed once`**：行未构建或未注入——依次检查：无光影环境（预期不出现）、门控（`shaderPack`、`MIN_WINDOW`、玩家侧 `along` 计算）、绑定层（`PSDClientBindings`、`MainRendererPSDMixin`）、窗口推导（`PSDWindowBounds`）、脚本实例存活（脚本进入 fail-cooldown 时回调不再跑，底板与文字一起冻结——若到站文字仍在滚动则非此项）、脚本任务长期在途（慢脚本：底板延迟一帧生效，长时间在途则需脚本侧排查）。
- **有 `degenerate: fill advance N — nothing drawn`**：advance 量宽失败（字体栈异常）——把 advance 值附在报告里。
- **有 `failed once: <异常>`**：注入异常（代码侧）——附日志全文。
- **`channel S active` 两半都出现且无异常、仍不可见** → 进入 7.0-18。

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
- **手写回调复刻 TextWrapper 语句序列 + 持有者 drawText(String) + 单字形拉伸**（第十四轮通道 P，build.95）——面包屑 + metrics 齐现、无异常、未装 / 已装 IF 均不可见。**手写路径全部证伪；不要再手写任何 drawText 复刻。**
- **ImmediatelyFast 是唯一根因的假设**（第十四轮两份日志实证：未装 / 已装均不显示）——IF 也非本模组及其上游的前置依赖，修复不得依赖其存在或缺失；其接管 immediate 工厂的效应已被第十三轮取证纳入模型（对 drawText 与我们的构造无差别）。
- **「任何完整字体的 U+2588 都是实心块」的假设**（第十四/十五轮的前提）——用户字体栈实测 advance 9（字形存在）但两轮 U+2588 载体全隐形，而缺失码点会渲染**可见的**豆腐框（SpecialGlyphs.MISSING 反汇编）⇒ 当时的唯一自洽解释是**该字体包的 U+2588 栅格化为全透明**。**不要再把 U+2588（或任何未经该环境实证的字符）用作底色载体**。（**第十七轮补录：U+2588 理论本身已被第十六轮探针行证伪**——真实可见字形族的探针行同样死；U+2588 实测不可用的结论仍然成立，但死因是平面/提交，不是字形。）
- **「字形内容/颜色是第十五/六轮的致死变量」（U+2588 栅格化理论）**——build.97 探针行（「底色R16测试」：与可见到站文字同字形族、同色、同字体、同 wrapper 机器、同队列同 key）不可见而脚本自己的 CJK 行可见 ⇒ 字形内容不是变量；真正的两项幸存差异是 **+0.45/16 底板平面**与**从 renderAll 直接 `run()` 的提交**——第十七轮通道 S 将两者一并消除。
- **「+0.45/16 平面上的任何载体在该光影栈下都不可见」（观察性结论，机理未明）**——十一至十六轮在该平面上的全部载体（手写 quad、真字形机器、holder drawText、wrapper 行、背景矩形、探针行）无一存活，而 +0.25/16 平面上的脚本文本从未失效；无光影时 +0.45/16 的载体 B 可见（深度/几何无碍）。机理候选（未验证，RenderDoc 级）：用户包内置扩散板（+0.5/16..+1/16）在光影下的深度/折射副作用、Iris 实体批处理对近平面剪裁/深度斜率的处理。**在该栈上不要再往 +0.45/16 放任何新载体；底板一律走脚本平面。**
- **「两个 text 层都带 DepthTest always」（第十一轮实证②的前半）**——第二轮取证证伪：NORMAL text 层无深度相位（继承 LEQUAL+写深度）；always + 只写颜色是 SEE_THROUGH 变体的相位。文字在玻璃后可见靠绘制顺序（实体阶段先画、半透明玻璃后画），不靠深度 always。**不要再基于「text 层无深度」做深度相关推论**。

---

## 8. 构建 / 部署备忘（本仓库工具链）

- **版本**：`gradle.properties` 的 `mod_version=2.3.0-1.0.0`；Complete Build 产物版本号 = `<基础版本>-build.<github.run_number>`（例：`2.3.0-1.0.0-build.91`）。Release Build 全平台全量发布（勿动）；Complete Build 仅 3 平台原生桥（Win x64 / Linux x64 / Android ARM64，按用户 2026-11-10 要求精简）。
- **提交**：本机 git push 网络不通——用 Git Data API 三步提交（参见 `/home/z/my-project/push_round12.py`：GET 当前 HEAD → POST blob/tree/commit → PATCH ref）。token 在 `/home/z/.gh/token`（2026-11-11 更新）。
- **触发构建**：workflow_dispatch `Complete Build`，入参只传 `{"mod-version": "2.3.0-1.0.0"}`（**不要**传 retention-days 等额外字段，会 422）。
- **监视**：`/home/z/my-project/scripts/watch_build_run.py <runId>`；构建产物在 run artifacts。
- **反汇编工具链**（本轮自建，/tmp 清理后需重建）：Python class 解析器 `/home/z/psd11/classdump.py`（修复了 Code 属性头 u4 多读的 bug）；原版类按 HTTP range 从 piston-data 拉（`/home/z/psd11/vanilla_fetch.py`）；MTR 4.0.5 jar 同法从 Modrinth CDN 拉（`/home/z/psd11/mr_fetch.py`）；MTR 源码直接从 GitHub `Minecraft-Transit-Railway/Minecraft-Transit-Railway` master 取（`fabric/src/main/java/org/mtr/mod/render/*.java`）；Yarn 映射 `maven.fabricmc.net` 的 tiny.gz。
- **第十三轮新增源码库存**（`/home/z/psd12/`）：Iris 1.20.1 分支关键源码（`irismixin_MixinGameRenderer.java`、`iris_IrisRenderingPipeline.java`、`ShaderKey/ShaderMap/ShaderAccess`、`MixinBufferBuilder/IrisVertexFormats/BufferBuilderPolygonView/NormalHelper` 等）；ImmediatelyFast v1.5.5 关键源码（`if_MixinVertexConsumerProvider.java`、`if_BatchableBufferSource.java`、`if_IrisCompat.java`）；ComplementaryUnbound r5.5.1 解包（`comp/shaders/`）；Iris 1.7.6 jar（`iris.jar`，取自 Modrinth CDN，大文件需 curl 分段拉取）。**（注意：/home/z/psd11 与 /home/z/psd12 已随 2026-11-12 沙盒重置丢失，GitHub token 同失——下文 psd16 为重建版）**
- **第十六轮工具链**（`/home/z/psd16/`）：自制 Python class 解析器 + 字节码反汇编器 `classdump.py`（支持 CP 解析 / Code 属性 / 主流操作码 / GitHub blob-API 拉取源码）；原版 1.20.1 `client.jar`（piston-data）+ ProGuard `client_mappings.txt`（混淆名对照：Font=eov、FontSet=ern、BakedGlyph=err、BakedGlyph$Effect=err$a、SpecialGlyphs=ert、RenderType=fkf、Font$StringRenderOutput=eov$b）；MTR 4.0.6 分支（≈4.0.5）关键类源码（MainRenderer/MoreRenderLayers/QueuedRenderLayer/StoredMatrixTransformations，经 blob API）。核心反汇编结论见 §2.2。
- **关键 Yarn/intermediary 对照**（1.20.1）：`RenderLayer=fkf(class_1921)`、`RenderPhase=fke(class_4668)`、`TextRenderer=eov(class_327)`、`TextLayerType: field_33993=NORMAL, field_33994=SEE_THROUGH, field_33995=POLYGON_OFFSET`、`GlyphRenderer=err(class_382)`、`VertexConsumer=ein(class_4588)`、`BufferBuilder=eie(class_287)`、`RenderLayer.getText=m(fkf).r(method_23028)(Identifier)acq(class_2960)`、`getBeaconBeam=e(method_23592)`、`getEntityCutout=c(method_23576)`；phase：`fke.h=TRANSLUCENT`、`fke.c=NO_TRANSPARENCY`、`fke.ay=DepthTest("always",519)`、`fke.aD=WriteMask(true,false)`、`fke.as=Lightmap(true)`。

---

*文档生成于第十一轮修复（2026-11-10）；第十二轮（2026-11-11）、第十三轮取证（2026-11-11）与第十四轮（2026-11-11，通道 P + 诊断条纹）更新；第十五轮（2026-11-12，通道 Q：真实 TextWrapper 行，build.95 证伪通道 P 后四变量一次性全消）；第十六轮（2026-11-12，第二轮取证 §2.2 + 通道 R：脚本文本自己的背景矩形，build.96 证伪通道 Q 后字形免疫载体 + 字形探针）；第十七轮（2026-11-12，通道 S：底板行注入脚本回放列表——build.97 结局③证伪字形理论后，平面 + 提交两项幸存差异一次全消）。维护者：BugQuantumCore / Z.ai 会话记录。*
