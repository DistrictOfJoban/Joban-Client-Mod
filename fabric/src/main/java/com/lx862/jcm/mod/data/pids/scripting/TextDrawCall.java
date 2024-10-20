package com.lx862.jcm.mod.data.pids.scripting;

import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.MutableText;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.render.StoredMatrixTransformations;

public class TextDrawCall extends DrawCall implements RenderHelper {
    private final TextWrapper textWrapper;

    public TextDrawCall(TextWrapper textWrapper, StoredMatrixTransformations storedMatrixTransformations) {
        super(storedMatrixTransformations);
        this.textWrapper = textWrapper;
    }

    @Override
    protected void drawTransformed(GraphicsHolder graphicsHolder) {
        MutableText finalText = TextHelper.literal(textWrapper.str);
        if(textWrapper.fontId != null) {
            finalText = TextUtil.withFont(finalText, new Identifier(textWrapper.fontId));
        }

        int totalW = GraphicsHolder.getTextWidth(finalText);
        int startX = 0;
        if(textWrapper.alignment == 0) {
            startX -= totalW / 2;
        } else if(textWrapper.alignment == 1) {
            startX -= totalW;
        }

        graphicsHolder.drawText(finalText, startX, 0, textWrapper.color, textWrapper.shadow, MAX_RENDER_LIGHT);
    }
}
