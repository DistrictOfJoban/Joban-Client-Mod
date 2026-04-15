package com.lx862.mtrscripting.util.model;

import com.lx862.jcm.mixin.modded.mtr.*;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.OptimizedModel;
import org.mtr.mapping.mapper.OptimizedRenderer;
import org.mtr.mapping.render.batch.MaterialProperties;
import org.mtr.mapping.render.model.RawModel;
import org.mtr.mapping.render.object.VertexArray;
import org.mtr.mod.client.CustomResourceLoader;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.QueuedRenderLayer;
import org.mtr.mod.render.StoredMatrixTransformations;
import org.mtr.mod.resource.OptimizedModelWrapper;

import java.util.List;

public class ModelJS {
    private final OptimizedModelWrapper model;

    public ModelJS(RawModelJS rawModel) {
        RawModel mdl = rawModel.rawModel;
        List<VertexArray> vertexArrays = mdl.upload(OptimizedModelAccessor.getDefaultMapping());
        OptimizedModel optimizedModel = new OptimizedModel(vertexArrays);
        this.model = OptimizedModelWrapperAccessor.createNew(optimizedModel);
    }

    private ModelJS(OptimizedModelWrapper model) {
        this.model = model;
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

    public void replaceAllTexture(Identifier id) {
        OptimizedModel optimizedModel = ((OptimizedModelWrapperAccessor)(Object)model).getOptimizedModel();
        List<VertexArray> vertexArrays = ((OptimizedModelAccessor)(Object)optimizedModel).getUploadedParts();

        vertexArrays.forEach(e -> {
            e.materialProperties.setTexture(id);
        });
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
        OptimizedModel optimizedModel = ((OptimizedModelWrapperAccessor)(Object)model).getOptimizedModel();
        List<VertexArray> vertexArrays = ((OptimizedModelAccessor)(Object)optimizedModel).getUploadedParts();
        vertexArrays.forEach(VertexArray::close);
    }
}
