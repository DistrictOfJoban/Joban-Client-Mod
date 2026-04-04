package com.lx862.jcm.mod.scripting.mtr.util;

import com.mojang.blaze3d.systems.RenderSystem;
import org.mtr.mod.client.CustomResourceLoader;

public class DynamicModelHolderJS {
    private ModelJS uploadedModel;

    public void uploadLater(ModelDataJS modelData) {
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
