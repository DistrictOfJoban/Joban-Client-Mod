package com.lx862.jcm.mod.scripting.pids;

import com.lx862.jcm.mod.util.CachedIdentifiers;
import com.lx862.jcm.mod.util.TextUtil;
import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.mod.util.JSUtil;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.holder.Style;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.Init;
import org.mtr.mod.InitClient;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.QueuedRenderLayer;
import org.mtr.mod.render.StoredMatrixTransformations;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.List;

import static com.lx862.jcm.mod.render.RenderHelper.MAX_RENDER_LIGHT;

public class TextWrapper extends PIDSDrawCall<TextWrapper> {
    protected String textContent;
    protected Identifier fontId;
    protected QueuedRenderLayer renderType;
    protected boolean shadow;
    protected boolean styleItalic;
    protected boolean styleBold;
    protected double scale;
    protected int overflowMode;
    protected int alignment;
    protected int color;
    protected double lineHeight;
    protected double marqueeProgressOverride;
    protected double marqueeDurationOverride;
    protected boolean naturalLight;

    /** Round 16 (channel R): when non-zero, every line ALSO carries a
     *  nametag-style background rectangle in this colour, drawn by the
     *  REAL TextRenderer through its {@code backgroundColor} parameter —
     *  a BakedGlyph$Effect quad over the font atlas's vanilla-synthesized
     *  WHITE glyph region, on the very same RenderLayer/texture/program
     *  the glyphs themselves ride. The fill is therefore COMPLETELY
     *  glyph-independent: no character rasterization is involved in the
     *  rectangle (only the string's advances size it), which is what makes
     *  it immune to the font-pack glyph corruption that killed rounds
     *  14-15's U+2588 rows (see PSD-SHADER-BACKING.md round 16). Zero =
     *  off (default); set from the PSD backing carrier only. */
    protected int jcm$bgColor;

    /** Exact-size, glyph-free fill used by the PSD backing. An empty text
     *  run still emits the vanilla background Effect quad, whose bounds
     *  are [-1, 1] x [-1, 9]. Scaling those bounds avoids rounding a TTF
     *  space's fractional advance once per column. */
    private boolean jcm$bgRectangle;

    /** Marquee glyph cache: per-character formatted texts and measured
     *  widths, rebuilt only when the marquee string <i>or its style</i>
     *  changes (the formatted text embeds fontId/bold/italic, so the cache
     *  key must cover them — a JS script may legally mutate those on a
     *  wrapper it holds across frames). The old loop formatted and measured
     *  EVERY character TWICE per frame (Style + MutableText +
     *  String.valueOf per char — a 30-char marquee ≈ 120 objects per wrapper
     *  per frame). */
    private String jcm$marqueeStr;
    private Identifier jcm$marqueeFont;
    private boolean jcm$marqueeBold, jcm$marqueeItalic;
    private MutableText[] jcm$marqueeTexts = EMPTY_TEXTS;
    private int[] jcm$marqueeWidths = EMPTY_WIDTHS;

    /** Free-list for the NATIVE replay path only: JS scripts may legally
     *  hold a TextWrapper across frames, so create() keeps allocating and
     *  release() ignores instances that never came from createPooled();
     *  the native record replay (NativeContentDriver.apply) owns its
     *  wrappers strictly within one frame and recycles them here. */
    private static final ArrayDeque<TextWrapper> NATIVE_POOL = new ArrayDeque<>();
    private static final int NATIVE_POOL_CAP = 64;
    private boolean jcm$nativePooled;
    private boolean jcm$inPool;

    private static final MutableText[] EMPTY_TEXTS = new MutableText[0];
    private static final int[] EMPTY_WIDTHS = new int[0];
    private static final Identifier DEFAULT_FONT = new Identifier(Init.MOD_ID, "mtr");

    protected TextWrapper() {
        super(100, 25);
        this.alignment = -1;
        this.scale = 1;
        this.lineHeight = 1;
        this.marqueeDurationOverride = -1;
        this.marqueeProgressOverride = -1;
        this.renderType = QueuedRenderLayer.TEXT;
        this.fontId = DEFAULT_FONT;
    }

    public static TextWrapper create() {
        return new TextWrapper();
    }

    public static TextWrapper create(String comment) {
        return create();
    }

    /** Pooled acquisition for the native record replay (see NATIVE_POOL).
     *  Public: NativeContentDriver lives in com.lx862.jcm.nativeapi. */
    public static TextWrapper createPooled() {
        final TextWrapper pooled = NATIVE_POOL.pollFirst();
        if (pooled != null) {
            pooled.jcm$inPool = false;
            pooled.jcm$resetDefaults();
            return pooled;
        }
        final TextWrapper fresh = new TextWrapper();
        fresh.jcm$nativePooled = true;
        return fresh;
    }

    @Override
    public void release() {
        if (!this.jcm$nativePooled || this.jcm$inPool) return;
        this.jcm$inPool = true;
        if (NATIVE_POOL.size() < NATIVE_POOL_CAP) NATIVE_POOL.offerFirst(this);
    }

    private void jcm$resetDefaults() {
        resetDefaults(100, 25);
        this.textContent = null;
        this.fontId = DEFAULT_FONT;
        this.renderType = QueuedRenderLayer.TEXT;
        this.shadow = false;
        this.styleItalic = false;
        this.styleBold = false;
        this.scale = 1;
        this.overflowMode = 0;
        this.alignment = -1;
        this.color = 0;
        this.lineHeight = 1;
        this.marqueeDurationOverride = -1;
        this.marqueeProgressOverride = -1;
        this.naturalLight = false;
        this.jcm$bgColor = 0;
        this.jcm$bgRectangle = false;
        this.jcm$marqueeStr = null;
        this.jcm$marqueeFont = null;
        this.jcm$marqueeBold = false;
        this.jcm$marqueeItalic = false;
        this.jcm$marqueeTexts = EMPTY_TEXTS;
        this.jcm$marqueeWidths = EMPTY_WIDTHS;
    }

    public TextWrapper text(String str) {
        this.textContent = str;
        return this;
    }

    public TextWrapper scale(double i) {
        this.scale = i;
        return this;
    }

    public TextWrapper leftAlign() {
        this.alignment = -1;
        return this;
    }

    public TextWrapper centerAlign() {
        this.alignment = 0;
        return this;
    }

    public TextWrapper rightAlign() {
        this.alignment = 1;
        return this;
    }

    public TextWrapper shadowed() {
        this.shadow = true;
        return this;
    }

    public TextWrapper stretchXY() {
        this.overflowMode = 1;
        return this;
    }

    public TextWrapper scaleXY() {
        this.overflowMode = 2;
        return this;
    }

    public TextWrapper wrapText() {
        this.overflowMode = 3;
        return this;
    }

    public TextWrapper lineHeight(double lineHeightFactor) {
        this.lineHeight = lineHeightFactor;
        return this;
    }

    public TextWrapper marquee() {
        return marquee(-1);
    }

    public TextWrapper marquee(double durationTickOverride) {
        this.overflowMode = 4;
        this.marqueeDurationOverride = durationTickOverride;
        return this;
    }

    public TextWrapper withMarqueeProgress(double marqueeProgressOverride) {
        this.marqueeProgressOverride = marqueeProgressOverride;
        return this;
    }

    public TextWrapper fontMC() {
        this.fontId = null;
        return this;
    }

    public TextWrapper font(String fontId) {
        /* Yanyang: absolute ids hit the render-thread identifier cache;
           "./"-relative ids (upstream) are context-dependent — bypass it. */
        return font(fontId.startsWith("./") ? JSUtil.parseId(fontId) : CachedIdentifiers.of(fontId));
    }

    public TextWrapper font(Identifier fontId) {
        this.fontId = fontId;
        return this;
    }

    public TextWrapper italic() {
        this.styleItalic = true;
        return this;
    }

    public TextWrapper bold() {
        this.styleBold = true;
        return this;
    }

    public TextWrapper color(int color) {
        this.color = color;
        return this;
    }

    public TextWrapper naturalLight() {
        this.naturalLight = true;
        return this;
    }

    /** Round 16 (channel R): a nametag-style background rectangle behind
     *  every line, drawn by the real TextRenderer's own {@code
     *  backgroundColor} mechanism on the glyphs' very RenderLayer — see
     *  the field javadoc. The glyph tint should normally be set to the
     *  SAME colour ({@link #color(int)}) so any glyph that does render
     *  blends seamlessly into the rectangle. */
    @ApiInternal
    public TextWrapper jcm$backgroundColor(int color) {
        this.jcm$bgColor = color;
        return this;
    }

    /** Draws one background rectangle covering {@code pos .. pos + size}.
     *  Uses the real text renderer's background path, with no font metrics
     *  or glyphs. The ordinary text API retains its existing semantics. */
    @ApiInternal
    public TextWrapper jcm$backgroundRectangle(int color) {
        this.jcm$bgColor = color;
        this.jcm$bgRectangle = true;
        return this;
    }

    public TextWrapper renderType(String renderType) {
        this.renderType = QueuedRenderLayer.valueOf(renderType);
        return this;
    }

    public int measureWidth() {
        if(textContent == null) throw new IllegalStateException("Text is not set!");
        return GraphicsHolder.getTextWidth(getFormattedText(textContent));
    }

    @Override
    public void validate() {
        if (this.jcm$bgRectangle) {
            if (!Double.isFinite(this.w) || !Double.isFinite(this.h) || this.w <= 0 || this.h <= 0) {
                throw new IllegalArgumentException("Background rectangle size must be finite and positive");
            }
        } else if (this.textContent == null) {
            throw new IllegalArgumentException("Text must be filled");
        }
    }

    @Override
    protected void drawTransformed(StoredMatrixTransformations storedMatrixTransformations, Direction facing, int light) {
        final int textLight = this.naturalLight ? light : MAX_RENDER_LIGHT;
        MainRenderer.scheduleRender(this.renderType, (graphicsHolderNew, offset) -> {
//          graphicsHolderNew.push(); // Applied with storedMatrixTransformations.transform
            storedMatrixTransformations.transform(graphicsHolderNew, offset);
            try {
                if (this.jcm$bgRectangle) {
                    // TextRenderer's empty-run background spans [-1,1] x
                    // [-1,9]. Map it exactly onto [0,w] x [0,h], retaining
                    // the normal text depth and the caller's face transform.
                    graphicsHolderNew.scale((float) (this.w / 2), (float) (this.h / 10), 1);
                    graphicsHolderNew.translate(1, 1, 0);
                    jcm$drawTextWithBackground(graphicsHolderNew, "", 0, 0,
                            this.jcm$bgColor, this.jcm$bgColor, false, textLight);
                    return;
                }
                graphicsHolderNew.scale((float)this.scale, (float)this.scale, (float)this.scale);

                List<MutableText> texts = new ArrayList<>();
                final MutableText originalText = getFormattedText(textContent);

                int actualW = GraphicsHolder.getTextWidth(originalText);
                int actualH = 9;

                if(this.overflowMode == 1) { // Stretch XY
                    if(actualW > this.w) {
                        graphicsHolderNew.scale((float)(this.w / actualW), 1, 1);
                    }
                    if(actualH > this.h) {
                        graphicsHolderNew.scale(1, (float)(this.h / actualH), 1);
                    }
                } else if(this.overflowMode == 2) { // Scale XY
                    double minScale = Math.min(actualW > this.w ? this.w / actualW : 1, actualH > this.h ? this.h / actualH : 1);
                    graphicsHolderNew.translate(0, (this.h - (actualH * minScale)) / 2, 0); // Center it vertically
                    graphicsHolderNew.scale((float)minScale, (float)minScale, 0);
                }

                if(this.overflowMode == 3) { // Wrap Text
                    StringBuilder curLine = new StringBuilder();
                    int wSoFar = 0;
                    for(int i = 0; i < this.textContent.length(); i++) {
                        char c = this.textContent.charAt(i);
                        wSoFar += GraphicsHolder.getTextWidth(String.valueOf(c));
                        if(wSoFar > this.w) {
                            texts.add(getFormattedText(curLine.toString()));
                            curLine = new StringBuilder(String.valueOf(c));
                            wSoFar = 0;
                        } else {
                            curLine.append(c);
                        }
                    }
                    if(curLine.length() > 0) {
                        texts.add(getFormattedText(curLine.toString()));
                    }
                } else {
                    texts.add(getFormattedText(this.textContent));
                }

                if(this.overflowMode == 4 && actualW > this.w) { // Marquee
                    drawMarqueeText(graphicsHolderNew, texts.get(0).getString(), this.color, this.shadow, textLight, this.marqueeDurationOverride, this.marqueeProgressOverride);
                } else {
                    int i = 0;
                    for(MutableText text : texts) {
                        drawText(graphicsHolderNew, text, (int)(i*(9*this.lineHeight)), this.color, this.shadow, textLight);
                        i++;
                    }
                }
            } finally {
                graphicsHolderNew.pop();
            }
        });
    }

    private void drawText(GraphicsHolder graphicsHolder, MutableText text, int y, int color, boolean shadow, int light) {
        int startX = 0;
        int totalW = GraphicsHolder.getTextWidth(text);
        if(this.alignment == 0) {
            startX -= totalW / 2;
        } else if(this.alignment == 1) {
            startX -= totalW;
        }
        if(this.jcm$bgColor != 0) {
            /* Round 16 (channel R): route the line through the real
               TextRenderer with a backgroundColor — MTR's GraphicsHolder
              .drawText hardcodes bg=0, so the call is issued directly
               here with the holder's own matrix, over the same private
               immediate construction drawText itself uses (round-11
               disassembly), synchronously flushed inside this callback.
               The background rectangle is a BakedGlyph$Effect quad on the
               glyphs' own RenderLayer — glyph-independent by construction. */
            jcm$drawTextWithBackground(graphicsHolder, text.getString(), startX, y, color, this.jcm$bgColor, shadow, light);
        } else {
            graphicsHolder.drawText(text, startX, y, color, shadow, light);
        }
    }

    /** The round-16 background-rect draw: TextRenderer.draw(String, …,
     * backgroundColor, light) on the callback holder's matrix, through a
     * private Immediate over the shared Tessellator buffer — the exact
     * construction every MTR GraphicsHolder.drawText performs internally
     * (round-11 bytecode forensics), synchronously flushed before the
     * callback returns, exactly like drawText's own discipline. The
     * loader/version guards mirror the proven round-12 (channel N2)
     * invocation, which shipped on all six MC versions × both loaders. */
    private void jcm$drawTextWithBackground(GraphicsHolder graphicsHolder, String text, int x, int y, int color, int backgroundColor, boolean shadow, int light) {
#if LOADER == "fabric"
        final net.minecraft.client.util.math.MatrixStack matrices =
                ((com.lx862.jcm.nativeapi.NativeMatrixApplier) (Object) graphicsHolder).jcm$getMatrixStack();
        final net.minecraft.client.render.VertexConsumerProvider.Immediate immediate =
                net.minecraft.client.render.VertexConsumerProvider.immediate(net.minecraft.client.render.Tessellator.getInstance().getBuffer());
#if MC_VERSION >= "11903"
        final org.joml.Matrix4f model = matrices.peek().getPositionMatrix();
        net.minecraft.client.MinecraftClient.getInstance().textRenderer.draw(text, x, y, color, shadow, model, immediate, net.minecraft.client.font.TextRenderer.TextLayerType.NORMAL, backgroundColor, light);
#else
#if MC_VERSION <= "11701"
        final net.minecraft.util.math.Matrix4f model = matrices.peek().getModel();
#else
        final net.minecraft.util.math.Matrix4f model = matrices.peek().getPositionMatrix();
#endif
        net.minecraft.client.MinecraftClient.getInstance().textRenderer.draw(text, x, y, color, shadow, model, immediate, false, backgroundColor, light);
#endif
        immediate.draw();
#else
        final com.mojang.blaze3d.vertex.PoseStack matrices =
                ((com.lx862.jcm.nativeapi.NativeMatrixApplier) (Object) graphicsHolder).jcm$getMatrixStack();
        final net.minecraft.client.renderer.MultiBufferSource.BufferSource immediate =
                net.minecraft.client.renderer.MultiBufferSource.immediate(com.mojang.blaze3d.vertex.Tesselator.getInstance().getBuilder());
#if MC_VERSION >= "11903"
        final org.joml.Matrix4f model = matrices.last().pose();
        net.minecraft.client.Minecraft.getInstance().font.drawInBatch(text, x, y, color, shadow, model, immediate, net.minecraft.client.gui.Font.DisplayMode.NORMAL, backgroundColor, light);
#else
        final com.mojang.math.Matrix4f model = matrices.last().pose();
        net.minecraft.client.Minecraft.getInstance().font.drawInBatch(text, x, y, color, shadow, model, immediate, false, backgroundColor, light);
#endif
        immediate.endBatch();
#endif
    }

    private void drawMarqueeText(GraphicsHolder graphicsHolder, String str, int color, boolean shadow, int light, double durationOverride, double progressOverride) {
        /* Glyph cache: format + measure each character once per CONTENT
         *  or STYLE change instead of twice per frame. */
        if (this.jcm$marqueeStr == null || !this.jcm$marqueeStr.equals(str)
                || this.jcm$marqueeTexts.length < str.length()
                || this.jcm$marqueeFont != this.fontId
                || this.jcm$marqueeBold != this.styleBold
                || this.jcm$marqueeItalic != this.styleItalic) {
            final int len = str.length();
            this.jcm$marqueeStr = str;
            this.jcm$marqueeFont = this.fontId;
            this.jcm$marqueeBold = this.styleBold;
            this.jcm$marqueeItalic = this.styleItalic;
            if (this.jcm$marqueeTexts.length < len) {
                this.jcm$marqueeTexts = new MutableText[len];
                this.jcm$marqueeWidths = new int[len];
            }
            for (int i = 0; i < len; i++) {
                this.jcm$marqueeTexts[i] = getFormattedText(String.valueOf(str.charAt(i)));
                this.jcm$marqueeWidths[i] = GraphicsHolder.getTextWidth(this.jcm$marqueeTexts[i]);
            }
        }
        final MutableText[] charTexts = this.jcm$marqueeTexts;
        final int[] textWidths = this.jcm$marqueeWidths;

        /* textWidth accounts for the duration where the text starts to disappear by clipping to the left edge */
        double textWidth = 0;
        for (int i = 0; i < str.length(); i++) {
            textWidth += textWidths[i];
        }

        /* + w accounts for the duration where the text starts to appear from the right edge */
        int fullWidth = (int)(textWidth + (int)this.w);
        double cycleDuration = durationOverride != -1 ? (durationOverride * 20) : str.length() * 10;
        double marqueeProgress = progressOverride != -1 ? progressOverride : (InitClient.getGameTick() % cycleDuration) / cycleDuration;

        double wSoFar = w - (fullWidth * marqueeProgress);
        for (int i = 0; i < str.length(); i++) {
            final MutableText tx = charTexts[i];

            if(wSoFar >= 0 && wSoFar <= this.w) {
                graphicsHolder.push();
                graphicsHolder.translate(wSoFar, 0, 0);
                graphicsHolder.drawText(tx, 0, 0, color, shadow, light);
                graphicsHolder.pop();
            }

            wSoFar += textWidths[i];
        }
    }

    private MutableText getFormattedText(String str) {
        final Style fontStyle;
        if(this.fontId != null) {
            fontStyle = TextUtil.withFontStyle(this.fontId).withBold(this.styleBold).withItalic(this.styleItalic);
        } else {
            fontStyle = Style.getEmptyMapped().withBold(this.styleBold).withItalic(this.styleItalic);
        }

        return TextHelper.setStyle(TextHelper.literal(str), fontStyle);
    }
}
