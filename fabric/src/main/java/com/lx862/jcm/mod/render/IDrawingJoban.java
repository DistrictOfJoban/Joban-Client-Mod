package com.lx862.jcm.mod.render;

import com.lx862.jcm.mod.JCMClient;
import org.mtr.libraries.it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.LightmapTextureManager;
import org.mtr.mapping.holder.OrderedText;
import org.mtr.mapping.holder.Style;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.config.Config;
import org.mtr.mod.data.IGui;

import javax.annotation.Nullable;

/**
 * Copied from MTR's IDrawing, but just for rendering with a specific font other than the hardcoded mtr one
 */
public interface IDrawingJoban {
    static void drawStringWithFont(GraphicsHolder graphicsHolder, String text, Identifier font, float x, float y, int light) {
        drawStringWithFont(graphicsHolder, text, font, IGui.HorizontalAlignment.CENTER, IGui.VerticalAlignment.CENTER, x, y, -1, -1, 1, IGui.ARGB_WHITE, true, light, null);
    }

    static void drawStringWithFont(GraphicsHolder graphicsHolder, String text, Identifier font, IGui.HorizontalAlignment horizontalAlignment, IGui.VerticalAlignment verticalAlignment, float x, float y, float maxWidth, float maxHeight, float scale, int textColor, boolean shadow, int light, @Nullable DrawingCallback drawingCallback) {
        drawStringWithFont(graphicsHolder, text, font, horizontalAlignment, verticalAlignment, horizontalAlignment, x, y, maxWidth, maxHeight, scale, textColor, shadow, light, drawingCallback);
    }

    static void drawStringWithFont(GraphicsHolder graphicsHolder, String text, Identifier font, IGui.HorizontalAlignment horizontalAlignment, IGui.VerticalAlignment verticalAlignment, IGui.HorizontalAlignment xAlignment, float x, float y, float maxWidth, float maxHeight, float scale, int textColor, boolean shadow, int light, @Nullable DrawingCallback drawingCallback) {
        drawStringWithFont(graphicsHolder, text, font, horizontalAlignment, verticalAlignment, xAlignment, x, y, maxWidth, maxHeight, scale, textColor, textColor, 2, shadow, light, drawingCallback);
    }

    static void drawStringWithFont(GraphicsHolder graphicsHolder, String text, Identifier font, IGui.HorizontalAlignment horizontalAlignment, IGui.VerticalAlignment verticalAlignment, IGui.HorizontalAlignment xAlignment, float x, float y, float maxWidth, float maxHeight, float scale, int textColorCjk, int textColor, float fontSizeRatio, boolean shadow, int light, @Nullable DrawingCallback drawingCallback) {
        final Style style = Config.getClient().getUseMTRFont() ? Style.getEmptyMapped().withFont(font) : Style.getEmptyMapped();

        while (text.contains("||")) {
            text = text.replace("||", "|");
        }
        final String[] stringSplit = text.split("\\|");

        final BooleanArrayList isCJKList = new BooleanArrayList();
        final ObjectArrayList<OrderedText> orderedTexts = new ObjectArrayList<>();
        int totalHeight = 0, totalWidth = 0;
        for (final String stringSplitPart : stringSplit) {
            final boolean isCJK = IGui.isCjk(stringSplitPart);
            isCJKList.add(isCJK);

            final OrderedText orderedText = TextHelper.mutableTextToOrderedText(TextHelper.setStyle(TextHelper.literal(stringSplitPart), style));
            orderedTexts.add(orderedText);

            totalHeight += Math.round(IGui.LINE_HEIGHT * (isCJK ? fontSizeRatio : 1));
            final int width = (int) Math.ceil(GraphicsHolder.getTextWidth(orderedText) * (isCJK ? fontSizeRatio : 1));
            if (width > totalWidth) {
                totalWidth = width;
            }
        }

        if (maxHeight >= 0 && totalHeight / scale > maxHeight) {
            scale = totalHeight / maxHeight;
        }

        graphicsHolder.push();

        final float totalWidthScaled;
        final float scaleX;
        if (maxWidth >= 0 && totalWidth > maxWidth * scale) {
            totalWidthScaled = maxWidth * scale;
            scaleX = totalWidth / maxWidth;
        } else {
            totalWidthScaled = totalWidth;
            scaleX = scale;
        }
        graphicsHolder.scale(1 / scaleX, 1 / scale, 1 / scale);

        float offset = verticalAlignment.getOffset(y * scale, totalHeight);
        for (int i = 0; i < orderedTexts.size(); i++) {
            final boolean isCJK = isCJKList.getBoolean(i);
            final float extraScale = isCJK ? fontSizeRatio : 1;
            if (isCJK) {
                graphicsHolder.push();
                graphicsHolder.scale(extraScale, extraScale, 1);
            }

            final float xOffset = horizontalAlignment.getOffset(xAlignment.getOffset(x * scaleX, totalWidth), GraphicsHolder.getTextWidth(orderedTexts.get(i)) * extraScale - totalWidth);

            final float shade = light == GraphicsHolder.getDefaultLight() ? 1 : Math.min(LightmapTextureManager.getBlockLightCoordinates(light) / 16F * 0.1F + 0.7F, 1);
            final int a = ((isCJK ? textColorCjk : textColor) >> 24) & 0xFF;
            final int r = (int) ((((isCJK ? textColorCjk : textColor) >> 16) & 0xFF) * shade);
            final int g = (int) ((((isCJK ? textColorCjk : textColor) >> 8) & 0xFF) * shade);
            final int b = (int) (((isCJK ? textColorCjk : textColor) & 0xFF) * shade);

            graphicsHolder.drawText(orderedTexts.get(i), Math.round(xOffset / extraScale), Math.round(offset / extraScale), (a << 24) + (r << 16) + (g << 8) + b, shadow, light);

            if (isCJK) {
                graphicsHolder.pop();
            }

            offset += IGui.LINE_HEIGHT * extraScale;
        }

        graphicsHolder.pop();

        if (drawingCallback != null) {
            final float x1 = xAlignment.getOffset(x, totalWidthScaled / scale);
            final float y1 = verticalAlignment.getOffset(y, totalHeight / scale);
            drawingCallback.drawingCallback(x1, y1, x1 + totalWidthScaled / scale, y1 + totalHeight / scale);
        }
    }

    @FunctionalInterface
    interface DrawingCallback {
        void drawingCallback(float x1, float y1, float x2, float y2);
    }
}
