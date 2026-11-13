package com.lx862.jcm.mod.scripting.pids;

import net.minecraft.client.font.TextRenderer;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.StoredMatrixTransformations;

/** Tests production TextWrapper against the engine's background Effect bounds. */
public final class TextBackgroundTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static void near(double value, double expected) {
        check(Math.abs(value - expected) < 0.0001, value + " != " + expected);
    }

    private static GraphicsHolder flush(TextWrapper wrapper) {
        wrapper.validate();
        wrapper.drawTransformed(new StoredMatrixTransformations(), Direction.NORTH, 0);
        GraphicsHolder holder = new GraphicsHolder();
        MainRenderer.pending.accept(holder, null);
        check(holder.matrices.stack.isEmpty(), "matrix stack leaked");
        return holder;
    }

    public static void main(String[] args) {
        // Reproduce build.98's fractional-space truncation before checking the fix.
        int columns = (int) Math.ceil(257.0 / GraphicsHolder.getTextWidth(" "));
        check(GraphicsHolder.getTextWidth(" ".repeat(columns)) < 255,
                "fixture must reproduce a font with fractional space advances");

        GraphicsHolder.forbidMetrics = true;
        for (double[] dimensions : new double[][]{{255, 150}, {0.5, 0.25}, {999.75, 83.125}}) {
            int before = TextRenderer.calls;
            flush(TextWrapper.create().size(dimensions[0], dimensions[1]).jcm$backgroundRectangle(0xFF0A0A0A));
            check(TextRenderer.calls == before + 1, "one fill must use one draw");
            near(TextRenderer.left, 0);
            near(TextRenderer.top, 0);
            near(TextRenderer.right, dimensions[0]);
            near(TextRenderer.bottom, dimensions[1]);
            near(TextRenderer.z, .01);
            check(TextRenderer.background == 0xFF0A0A0A, "background tint lost");
            check(TextRenderer.text.isEmpty(), "fill emitted a glyph");
        }

        for (double invalid : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            try {
                TextWrapper.create().size(invalid, 10).jcm$backgroundRectangle(-1).validate();
                throw new AssertionError("invalid size accepted: " + invalid);
            } catch (IllegalArgumentException expected) { }
        }

        TextWrapper pooled = TextWrapper.createPooled().size(30, 20).jcm$backgroundRectangle(-1);
        flush(pooled);
        pooled.release();
        TextWrapper reused = TextWrapper.createPooled();
        check(reused == pooled, "fixture must reuse the same wrapper");
        GraphicsHolder.forbidMetrics = false;
        int before = TextRenderer.calls;
        flush(reused.text("ordinary text"));
        check(TextRenderer.calls == before, "pooled reset retained rectangle mode");
        check(GraphicsHolder.textCalls == 1, "ordinary text path was bypassed");

        TextWrapper rectangle = TextWrapper.create().jcm$backgroundRectangle(-1);
        rectangle.drawTransformed(new StoredMatrixTransformations(), Direction.NORTH, 0);
        GraphicsHolder holder = new GraphicsHolder();
        TextRenderer.fail = true;
        try {
            MainRenderer.pending.accept(holder, null);
            throw new AssertionError("renderer failure swallowed");
        } catch (IllegalStateException expected) {
            check(holder.matrices.stack.isEmpty(), "failed draw leaked its matrix push");
        } finally {
            TextRenderer.fail = false;
        }
        System.out.println("Text background coverage, font independence, pooling and matrix cleanup passed");
    }
}
