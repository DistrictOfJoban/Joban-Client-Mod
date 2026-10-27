package com.lx862.jcm.mod.scripting.pids;

import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.util.CachedIdentifiers;
import com.lx862.mtrscripting.mod.util.JSUtil;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.QueuedRenderLayer;
import org.mtr.mod.render.StoredMatrixTransformations;

import java.util.ArrayDeque;

import static com.lx862.jcm.mod.render.RenderHelper.*;

public class TextureWrapper extends PIDSDrawCall<TextureWrapper> {
    protected Identifier textureId;
    protected int color;
    protected boolean naturalLight = false;
    protected float u1;
    protected float v1;
    protected float u2;
    protected float v2;
    protected QueuedRenderLayer renderType;

    /** Free-list for the NATIVE replay path only (see TextWrapper.NATIVE_POOL
     *  for the rationale: JS scripts may hold wrappers across frames, so
     *  only instances acquired via createPooled are ever recycled). */
    private static final ArrayDeque<TextureWrapper> NATIVE_POOL = new ArrayDeque<>();
    private static final int NATIVE_POOL_CAP = 64;
    private boolean jcm$nativePooled;
    private boolean jcm$inPool;

    protected TextureWrapper() {
        super(20, 20);
        this.textureId = null;
        this.color = ARGB_WHITE;
        this.u1 = 0;
        this.v1 = 0;
        this.u2 = 1;
        this.v2 = 1;
        this.renderType = QueuedRenderLayer.LIGHT_2;
    }

    public static TextureWrapper create() {
        return new TextureWrapper();
    }

    public static TextureWrapper create(String comment) {
        return create();
    }

    /** Pooled acquisition for the native record replay. Public:
     *  NativeContentDriver lives in com.lx862.jcm.nativeapi. */
    public static TextureWrapper createPooled() {
        final TextureWrapper pooled = NATIVE_POOL.pollFirst();
        if (pooled != null) {
            pooled.jcm$inPool = false;
            pooled.jcm$resetDefaults();
            return pooled;
        }
        final TextureWrapper fresh = new TextureWrapper();
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
        resetDefaults(20, 20);
        this.textureId = null;
        this.color = ARGB_WHITE;
        this.u1 = 0;
        this.v1 = 0;
        this.u2 = 1;
        this.v2 = 1;
        this.naturalLight = false;
        this.renderType = QueuedRenderLayer.LIGHT_2;
    }

    public TextureWrapper texture(String id) {
        /* Yanyang: absolute ids hit the render-thread identifier cache;
           "./"-relative ids (upstream) are context-dependent — bypass it. */
        return texture(id.startsWith("./") ? JSUtil.parseId(id) : CachedIdentifiers.of(id));
    }

    public TextureWrapper texture(Identifier id) {
        this.textureId = id;
        return this;
    }

    public TextureWrapper color(int color) {
        this.color = color;
        return this;
    }

    public TextureWrapper uv(float u1, float v1, float u2, float v2) {
        this.u1 = u1;
        this.v1 = v1;
        this.u2 = u2;
        this.v2 = v2;
        return this;
    }

    public TextureWrapper uv(float u2, float v2) {
        this.u1 = 0;
        this.v1 = 0;
        this.u2 = u2;
        this.v2 = v2;
        return this;
    }

    public TextureWrapper naturalLight() {
        this.naturalLight = true;
        return this;
    }

    public TextureWrapper renderType(String renderType) {
        this.renderType = QueuedRenderLayer.valueOf(renderType);
        return this;
    }

    @Override
    public void validate() {
        if(this.textureId == null) throw new IllegalArgumentException("texture must be filled");
    }

    @Override
    protected void drawTransformed(StoredMatrixTransformations storedMatrixTransformations, Direction facing, int light) {
        MainRenderer.scheduleRender(this.textureId, false, this.renderType, (graphicsHolderNew, offset) -> {
//          graphicsHolderNew.push(); // Applied with storedMatrixTransformations.transform
            storedMatrixTransformations.transform(graphicsHolderNew, offset);
            RenderHelper.drawTexture(graphicsHolderNew, 0, 0, 0, (float)this.w, (float)this.h, this.u1, this.v1, this.u2, this.v2, facing, ARGB_BLACK + this.color, this.naturalLight ? light : MAX_RENDER_LIGHT);
            graphicsHolderNew.pop();
        });
    }
}
