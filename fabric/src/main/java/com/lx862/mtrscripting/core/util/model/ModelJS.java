package com.lx862.mtrscripting.core.util.model;

import com.lx862.jcm.mixin.mtrscripting.OptimizedModelAccessor;
import com.lx862.jcm.mixin.mtrscripting.OptimizedModelWrapperAccessor;
import com.lx862.jcm.mixin.mtrscripting.GlStateTrackerAccessor;
import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.mod.util.JSUtil;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.OptimizedModel;
import org.mtr.mapping.mapper.OptimizedRenderer;
import org.mtr.mapping.render.batch.MaterialProperties;
import org.mtr.mapping.render.model.RawModel;
import org.mtr.mapping.render.object.VertexArray;
import org.mtr.mapping.render.tool.GlStateTracker;
import org.mtr.mod.client.CustomResourceLoader;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.QueuedRenderLayer;
import org.mtr.mod.render.StoredMatrixTransformations;
import org.mtr.mod.resource.OptimizedModelWrapper;

import java.util.List;

public class ModelJS {
    private final OptimizedModelWrapper model;

    private ModelJS(OptimizedModelWrapper model) {
        this.model = model;
    }

    @ApiInternal
    public static ModelJS uploadRawModel(RawModelJS rawModelJS) {
        // Native scripts upload during RenderVehicles.render(), outside MTR's
        // resource-reload scope. VertexArray/VertexBuffer require protection
        // even on the render thread. Leave an existing outer scope intact.
        final boolean ownsProtection = !GlStateTrackerAccessor.isStateProtected();
        if (ownsProtection) GlStateTracker.capture();
        try {
            RawModel rawModel = rawModelJS.rawModel;
            List<VertexArray> vertexArrays = rawModel.upload(OptimizedModelAccessor.getDefaultMapping());
            OptimizedModel optimizedModel = new OptimizedModel(vertexArrays);
            return new ModelJS(OptimizedModelWrapperAccessor.createNew(optimizedModel));
        } finally {
            if (ownsProtection) GlStateTracker.restore();
        }
    }

    public ModelJS copyForMaterialChanges() {
        OptimizedModel optimizedModel = ((OptimizedModelWrapperAccessor)(Object)model).getOptimizedModel();
        List<VertexArray> vertexArrays =
                ((OptimizedModelAccessor)(Object)optimizedModel).getUploadedParts().stream().map(e -> {
                    MaterialProperties newProp = new MaterialProperties(e.materialProperties.shaderType, e.materialProperties.getTexture(), e.materialProperties.vertexAttributeState.color);
                    return new VertexArray(e, newProp);
                }).toList();

        OptimizedModel newModel = new OptimizedModel(vertexArrays);
        OptimizedModelWrapper wrapper = OptimizedModelWrapperAccessor.createNew(newModel);
        return new ModelJS(wrapper);
    }

    public void replaceTexture(String oldTexture, String newTexture) {
        replaceTexture(oldTexture, JSUtil.parseId(newTexture));
    }

    public void replaceTexture(String oldTexture, Identifier newTexture) {
        OptimizedModel optimizedModel = ((OptimizedModelWrapperAccessor)(Object)model).getOptimizedModel();
        List<VertexArray> vertexArrays = ((OptimizedModelAccessor)(Object)optimizedModel).getUploadedParts();

        vertexArrays.forEach(e -> {
            Identifier id = e.materialProperties.getTexture();
            if (id == null) return;

            String oldPath = id.getPath();
            if (oldPath.substring(oldPath.lastIndexOf("/") + 1).equals(oldTexture)) {
                e.materialProperties.setTexture(newTexture);
            }
        });
    }

    public void replaceAllTexture(String id) {
        replaceAllTexture(JSUtil.parseId(id));
    }

    public void replaceAllTexture(Identifier id) {
        OptimizedModel optimizedModel = ((OptimizedModelWrapperAccessor)(Object)model).getOptimizedModel();
        List<VertexArray> vertexArrays = ((OptimizedModelAccessor)(Object)optimizedModel).getUploadedParts();

        vertexArrays.forEach(e -> {
            e.materialProperties.setTexture(id);
        });
    }

    @ApiInternal
    public void draw(StoredMatrixTransformations storedMatrixTransformations, int light) {
        if(OptimizedRenderer.hasOptimizedRendering()) {
            if(this.model != null) {
                MainRenderer.scheduleRender(QueuedRenderLayer.TEXT, (graphicsHolder, offset) -> {
                    storedMatrixTransformations.transform(graphicsHolder, offset);
                    CustomResourceLoader.OPTIMIZED_RENDERER_WRAPPER.queue(this.model, graphicsHolder, light);
                    graphicsHolder.pop();
                });
            }
        }
    }

    /* ---- ABI 16: tint / emissive / translucent model draws (native
       scripts' MTR_RECORD_FLAG_EMISSIVE / _TRANSLUCENT + the JcmDrawModel
       tint field). The tinted/translucent variant is a material-level
       COPY of the uploaded model — new VertexArray(part, newProps) — so
       the original stays untouched and shared. Cached single-entry:
       scripts draw a given model with one steady style, and a style change
       rebuilds once. */
    private volatile int tintedKey;
    private volatile ModelJS tintedCache;
    private static final int TINT_NONE = 0;

    /** Draws with an ARGB tint (0 = untinted) and/or the translucent
     *  material pass. Alpha below 255 forces the translucent pass (the
     *  opaque shaders ignore it). */
    @ApiInternal
    public void draw(StoredMatrixTransformations storedMatrixTransformations, int light, int colorARGB, boolean translucent) {
        if (colorARGB == TINT_NONE && !translucent) {
            draw(storedMatrixTransformations, light);
            return;
        }
        final int argb = colorARGB == TINT_NONE ? 0xFFFFFFFF : colorARGB;
        if (!translucent && (argb >>> 24) < 0xFF) translucent = true;
        final int key = argb | (translucent ? 0x100 : 0);
        ModelJS variant = tintedCache;
        if (variant == null || tintedKey != key) {
            variant = buildTintedVariant(argb, translucent);
            if (variant == null) {
                /* Shader/material copy failed (headless, odd part shape) —
                   degrade to the untinted draw instead of losing the model. */
                draw(storedMatrixTransformations, light);
                return;
            }
            tintedCache = variant;
            tintedKey = key;
        }
        variant.draw(storedMatrixTransformations, light);
    }

    private ModelJS buildTintedVariant(int argb, boolean translucent) {
        try {
            OptimizedModel optimizedModel = ((OptimizedModelWrapperAccessor)(Object)model).getOptimizedModel();
            List<VertexArray> vertexArrays =
                    ((OptimizedModelAccessor)(Object)optimizedModel).getUploadedParts().stream().map(e -> {
                        MaterialProperties oldProp = e.materialProperties;
                        /* The translucent pass swaps the shader type the way
                           a model JSON's render_stage would; the tint feeds
                           the vertex-colour attribute every stage reads. */
                        MaterialProperties newProp = new MaterialProperties(
                                translucent ? org.mtr.mod.resource.RenderStage.INTERIOR_TRANSLUCENT.shaderType
                                        : oldProp.shaderType,
                                oldProp.getTexture(), argb);
                        return new VertexArray(e, newProp);
                    }).toList();
            OptimizedModel newModel = new OptimizedModel(vertexArrays);
            return new ModelJS(OptimizedModelWrapperAccessor.createNew(newModel));
        } catch (Throwable t) {
            return null;
        }
    }

    @ApiInternal
    public OptimizedModelWrapper getUnderlyingModel() {
        return this.model;
    }

    public void close() {
        OptimizedModel optimizedModel = ((OptimizedModelWrapperAccessor)(Object)model).getOptimizedModel();
        List<VertexArray> vertexArrays = ((OptimizedModelAccessor)(Object)optimizedModel).getUploadedParts();
        vertexArrays.forEach(VertexArray::close);
    }
}
