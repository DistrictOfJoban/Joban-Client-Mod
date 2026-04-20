package com.lx862.mtrscripting.core.util.model;

import com.lx862.mtrscripting.core.annotation.ValueNullable;
import com.lx862.mtrscripting.mod.MTRScriptingMod;
import org.apache.commons.lang3.StringUtils;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.OptimizedModel;
import org.mtr.mapping.mapper.ResourceManagerHelper;
import org.mtr.mod.resource.CustomResourceTools;
import org.mtr.mod.resource.OptimizedModelWrapper;

import java.util.HashMap;
import java.util.Map;

public class ModelManagerJS {
    private static final Map<Identifier, RawModelJS> modelDataCache = new HashMap<>();
    private static final Map<RawModelJS, ModelJS> modelCache = new HashMap<>();

    private ModelManagerJS() {
    }

    @Deprecated
    public static RawModelJS loadRawModel(@ValueNullable Object resourceManager, Identifier id, @ValueNullable Object atlasManager) {
        return loadModel(id, false);
    }

    @Deprecated
    public static Map<String, RawModelJS> loadPartedRawModel(@ValueNullable Object resourceManager, Identifier id, @ValueNullable Object atlasManager) {
        return loadModelParts(id, false);
    }

    @Deprecated
    public static ModelJS uploadVertArrays(RawModelJS modelData) {
        return upload(modelData);
    }

    public static RawModelJS loadModel(Identifier id) {
        return loadModel(id, true);
    }

    public static RawModelJS loadModel(Identifier id, boolean flipTextureV) {
        if(modelDataCache.containsKey(id)) return modelDataCache.get(id);

        try {
            RawModelJS modelData = new RawModelJS();
            Map<String, RawModelJS> modelDataParts = loadModelParts(id, flipTextureV);
            modelDataParts.values().forEach(modelData::append);
            modelDataCache.put(id, modelData);
            return modelData;
        } catch (Exception e) {
            MTRScriptingMod.LOGGER.error("", e);
            return null;
        }
    }

    public static Map<String, RawModelJS> loadModelParts(Identifier modelLocation, boolean flipTextureV) {
        String idStr = modelLocation.getNamespace() + ":" + modelLocation.getPath();

        final boolean isObj = idStr.endsWith(".obj");
        final Identifier textureId = CustomResourceTools.formatIdentifierWithDefault("", "png");

        if (isObj) {
            Map<String, RawModelJS> rawModels = new Object2ObjectOpenHashMap<>();
            Map<String, OptimizedModel.ObjModel> models = OptimizedModel.ObjModel.loadModel(
                    ResourceManagerHelper.readResource(CustomResourceTools.formatIdentifierWithDefault(idStr, "obj")),
                    mtlString -> ResourceManagerHelper.readResource(CustomResourceTools.getResourceFromSamePath(idStr, mtlString, "mtl")),
                    textureString -> StringUtils.isEmpty(textureString) ? OptimizedModelWrapper.WHITE_TEXTURE : StringUtils.equals(textureString, "default.png") ? textureId : CustomResourceTools.getResourceFromSamePath(idStr, textureString, "png"),
                    null, true, flipTextureV
            );
            for(Map.Entry<String, OptimizedModel.ObjModel> modelEntry : models.entrySet()) {
                rawModels.put(modelEntry.getKey(), new RawModelJS(modelEntry.getValue()));
            }
            return rawModels;
        } else {
            throw new IllegalArgumentException("Unsupported model format " + idStr + ".");
        }
    }

    public static RawModelJS loadModelPart(String modelName, Identifier modelLocation) {
        return loadModelPart(modelName, modelLocation, true);
    }

    public static RawModelJS loadModelPart(String modelName, Identifier modelLocation, boolean flipTextureV) {
        return loadModelParts(modelLocation, flipTextureV).get(modelName);
    }

    public static ModelJS upload(RawModelJS rawModel) {
        if(modelCache.containsKey(rawModel)) return modelCache.get(rawModel);
        ModelJS uploadedModel = ModelJS.uploadRawModel(rawModel);
        modelCache.put(rawModel, uploadedModel);
        return uploadedModel;
    }

    public static void reset() {
        modelCache.values().forEach(ModelJS::close);
        modelCache.clear();
        modelDataCache.clear();
    }
}
