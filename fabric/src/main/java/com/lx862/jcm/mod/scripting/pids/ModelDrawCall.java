package com.lx862.jcm.mod.scripting.pids;

import com.lx862.mtrscripting.scripting.util.NewModel;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.OptimizedRenderer;
import org.mtr.mod.client.CustomResourceLoader;
import org.mtr.mod.render.DynamicVehicleModel;
import org.mtr.mod.resource.OptimizedModelWrapper;

import static com.lx862.jcm.mod.render.RenderHelper.MAX_RENDER_LIGHT;

/** This is only for implementation testing, DO NOT RELY ON THIS IN YOUR SCRIPT as it will likely be removed in the future */
public class ModelDrawCall extends PIDSDrawCall {
    public NewModel model;

    protected ModelDrawCall() {
        super(100, 25);
    }

    public static ModelDrawCall create() {
        return new ModelDrawCall();
    }

    public static ModelDrawCall create(String comment) {
        return create();
    }

    public ModelDrawCall model(NewModel model) {
        this.model = model;
        return this;
    }

    @Override
    protected void validate() {
        if(this.model == null) throw new IllegalArgumentException("Model must be filled");
    }

    @Override
    protected void drawTransformed(GraphicsHolder graphicsHolder, Direction facing) {
        final DynamicVehicleModel dynamicVehicleModel = this.model.getDynamicVehicleModel();
        OptimizedModelWrapper optimizedModelWrapper = this.model.getOptimizedModel();
        if(OptimizedRenderer.hasOptimizedRendering()) {
            if(optimizedModelWrapper != null) {
                CustomResourceLoader.OPTIMIZED_RENDERER_WRAPPER.queue(optimizedModelWrapper, graphicsHolder, MAX_RENDER_LIGHT);
            }
        } else {
            if(dynamicVehicleModel != null) {
                dynamicVehicleModel.render(graphicsHolder, MAX_RENDER_LIGHT, 0, 1, 1, 1, 1);
            }
        }
    }
}
