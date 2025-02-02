package com.lx862.mtrscripting.scripting.util;

import org.mtr.mapping.holder.Identifier;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import org.mtr.mapping.mapper.OptimizedRenderer;
import org.mtr.mapping.mapper.ResourceManagerHelper;
import org.mtr.mod.client.CustomResourceLoader;
import org.mtr.mod.render.DynamicVehicleModel;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.QueuedRenderLayer;
import org.mtr.mod.render.StoredMatrixTransformations;
import org.mtr.mod.resource.OptimizedModelWrapper;
import org.mtr.mod.resource.StoredModelResourceBase;

public class NewModel implements StoredModelResourceBase {
    private final ObjectObjectImmutablePair<OptimizedModelWrapper, DynamicVehicleModel> models;

    public NewModel(Identifier location, boolean flipV) {
        this.models = load(location.getNamespace() + ":" + location.getPath(), "", flipV, 0, ResourceManagerHelper::readResource);
    }

    @Override
    public OptimizedModelWrapper getOptimizedModel() {
        return models.left();
    }

    @Override
    public DynamicVehicleModel getDynamicVehicleModel() {
        return models.right();
    }

    public void draw(StoredMatrixTransformations storedMatrixTransformations, int light) {
        final DynamicVehicleModel dynamicVehicleModel = getDynamicVehicleModel();
        OptimizedModelWrapper optimizedModelWrapper = getOptimizedModel();
        if(OptimizedRenderer.hasOptimizedRendering()) {
            if(optimizedModelWrapper != null) {
                MainRenderer.scheduleRender(QueuedRenderLayer.TEXT, (graphicsHolder, offset) -> {
                    storedMatrixTransformations.transform(graphicsHolder, offset);
                    CustomResourceLoader.OPTIMIZED_RENDERER_WRAPPER.queue(optimizedModelWrapper, graphicsHolder, light);
                    graphicsHolder.pop();
                });
            }
        } else {
            if(dynamicVehicleModel != null) {
                MainRenderer.scheduleRender(QueuedRenderLayer.TEXT, (graphicsHolder, offset) -> {
                    storedMatrixTransformations.transform(graphicsHolder, offset);
                    dynamicVehicleModel.render(graphicsHolder, light, 0, 1, 1, 1, 1);
                    graphicsHolder.pop();
                });
            }
        }
    }
}
