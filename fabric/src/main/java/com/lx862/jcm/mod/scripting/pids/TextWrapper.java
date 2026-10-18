package com.lx862.jcm.mod.scripting.pids;

import com.lx862.jcm.mod.util.CachedIdentifiers;
import com.lx862.jcm.mod.util.TextUtil;
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
        return font(CachedIdentifiers.of(fontId));
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
        if(this.textContent == null) throw new IllegalArgumentException("Text must be filled");
    }

    @Override
    protected void drawTransformed(StoredMatrixTransformations storedMatrixTransformations, Direction facing, int light) {
        final int textLight = this.naturalLight ? light : MAX_RENDER_LIGHT;
        MainRenderer.scheduleRender(this.renderType, (graphicsHolderNew, offset) -> {
//          graphicsHolderNew.push(); // Applied with storedMatrixTransformations.transform
            storedMatrixTransformations.transform(graphicsHolderNew, offset);
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
            graphicsHolderNew.pop();
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
        graphicsHolder.drawText(text, startX, y, color, shadow, light);
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
