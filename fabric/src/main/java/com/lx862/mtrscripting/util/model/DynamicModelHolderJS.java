package com.lx862.mtrscripting.util.model;

import com.mojang.blaze3d.systems.RenderSystem;
import org.mtr.mod.client.CustomResourceLoader;

public class DynamicModelHolderJS {
    private ModelJS uploadedModel;

    public void uploadLater(RawModelJS modelData) {
        if(modelData == null) throw new IllegalArgumentException("modelData cannot be null!");

        RenderSystem.recordRenderCall(() -> {
            ModelJS lastUploadedModel = uploadedModel;
            CustomResourceLoader.OPTIMIZED_RENDERER_WRAPPER.beginReload();
            uploadedModel = new ModelJS(modelData);
            if (lastUploadedModel != null) lastUploadedModel.close();
            CustomResourceLoader.OPTIMIZED_RENDERER_WRAPPER.finishReload();
        });
    }

    public ModelJS getUploadedModel() {
        return uploadedModel;
    }

    public void close() {
        // TODO: No-op for now
    }
}
