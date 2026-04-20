package com.lx862.mtrscripting.core.util.model;

import com.mojang.blaze3d.systems.RenderSystem;
import org.mtr.mod.client.CustomResourceLoader;

public class DynamicModelHolderJS {
    private ModelJS uploadedModel;

    public DynamicModelHolderJS() {
    }

    public void uploadLater(RawModelJS modelData) {
        if(modelData == null) throw new IllegalArgumentException("modelData cannot be null!");

        RenderSystem.recordRenderCall(() -> {
            ModelJS lastUploadedModel = uploadedModel;
            CustomResourceLoader.OPTIMIZED_RENDERER_WRAPPER.beginReload();
            uploadedModel = ModelJS.uploadRawModel(modelData);
            if (lastUploadedModel != null) lastUploadedModel.close();
            CustomResourceLoader.OPTIMIZED_RENDERER_WRAPPER.finishReload();
        });
    }

    public ModelJS getUploadedModel() {
        return uploadedModel;
    }

    public void close() {
        uploadedModel.close();
    }
}
