package com.lx862.jcm.mod.scripting.mtr.util;

import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.mapper.OptimizedModel;
import org.mtr.mapping.mapper.OptimizedRenderer;
import org.mtr.mod.client.CustomResourceLoader;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.QueuedRenderLayer;
import org.mtr.mod.render.StoredMatrixTransformations;
import org.mtr.mod.resource.OptimizedModelWrapper;
import org.mtr.mod.resource.RenderStage;

import java.util.List;
import java.util.stream.Collectors;

public class ScriptModelCluster {
    private final OptimizedModelWrapper model;

    public ScriptModelCluster(ScriptRawModel rawModel) {
        CustomResourceLoader.OPTIMIZED_RENDERER_WRAPPER.beginReload();
        List<OptimizedModel.ObjModel> models = rawModel.models;
        models.forEach(e -> e.addTransformation(RenderStage.EXTERIOR.shaderType, 0, 0, 0, false));
        this.model = OptimizedModelWrapper.fromObjModels(new ObjectArrayList<>(models.stream().map(OptimizedModelWrapper.ObjModelWrapper::new).collect(Collectors.toList())));
        CustomResourceLoader.OPTIMIZED_RENDERER_WRAPPER.finishReload();
    }

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

    public void close() {

    }
}
