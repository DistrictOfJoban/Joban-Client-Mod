package com.lx862.jcm.mod.scripting.mtr.util;

import com.lx862.jcm.mixin.modded.mtr.*;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.OptimizedModel;
import org.mtr.mapping.mapper.OptimizedRenderer;
import org.mtr.mapping.render.batch.MaterialProperties;
import org.mtr.mapping.render.object.VertexArray;
import org.mtr.mod.client.CustomResourceLoader;
import org.mtr.mod.render.MainRenderer;
import org.mtr.mod.render.QueuedRenderLayer;
import org.mtr.mod.render.StoredMatrixTransformations;
import org.mtr.mod.resource.OptimizedModelWrapper;
import org.mtr.mod.resource.RenderStage;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ModelJS {
    private final OptimizedModelWrapper model;

    public ModelJS(ModelDataJS rawModel) {
        List<OptimizedModel.ObjModel> models = rawModel.models;
        models.forEach(e -> e.addTransformation(stringToRenderStage(rawModel.renderType).shaderType, 0, 0, 0, false));
        this.model = OptimizedModelWrapper.fromObjModels(new ObjectArrayList<>(models.stream().map(OptimizedModelWrapper.ObjModelWrapper::new).collect(Collectors.toList())));
    }

    private ModelJS(OptimizedModelWrapper model) {
        this.model = model;
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

    private static RenderStage stringToRenderStage(String s) {
        s = s
                .replace("exteriortranslucent", "interior_translucent")
                .replace("interiortranslucent", "interior_translucent")
                .replace("lighttranslucent", "always_on_light")
                .toUpperCase(Locale.ROOT);
        return RenderStage.valueOf(s);
    }
}
