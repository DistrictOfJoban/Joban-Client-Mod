package com.lx862.jcm.mod.scripting.mtr.util;

import com.lx862.mtrscripting.ScriptManager;
import org.apache.commons.lang3.StringUtils;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.OptimizedModel;
import org.mtr.mapping.mapper.ResourceManagerHelper;
import org.mtr.mod.resource.CustomResourceTools;
import org.mtr.mod.resource.OptimizedModelWrapper;

import java.util.Map;

public class ModelManager {
    @Deprecated
    public static ScriptRawModel loadRawModel(Object o, Identifier id, Object atlasManager) {
        return loadModel(id, false);
    }

    @Deprecated
    public static Map<String, ScriptRawModel> loadPartedRawModel(Object o, Identifier id, Object atlasManager) {
        return loadModelParts(id, false);
    }

    public static ScriptRawModel loadModel(Identifier id, boolean flipV) {
        try {
            ScriptRawModel rawModel = new ScriptRawModel();
            Map<String, ScriptRawModel> rawModels = loadModelParts(id, flipV);
            rawModels.values().forEach(rawModel::append);
            return rawModel;
        } catch (Exception e) {
            ScriptManager.LOGGER.error("", e);
            return null;
        }
    }

    public static Map<String, ScriptRawModel> loadModelParts(Identifier modelLocation, boolean flipTextureV) {
        String idStr = modelLocation.getNamespace() + ":" + modelLocation.getPath();

        final boolean isObj = idStr.endsWith(".obj");
        final Identifier textureId = CustomResourceTools.formatIdentifierWithDefault("", "png");

        if (isObj) {
            Map<String, ScriptRawModel> rawModels = new Object2ObjectOpenHashMap<>();
            Map<String, OptimizedModel.ObjModel> models = OptimizedModel.ObjModel.loadModel(
                    ResourceManagerHelper.readResource(CustomResourceTools.formatIdentifierWithDefault(idStr, "obj")),
                    mtlString -> ResourceManagerHelper.readResource(CustomResourceTools.getResourceFromSamePath(idStr, mtlString, "mtl")),
                    textureString -> StringUtils.isEmpty(textureString) ? OptimizedModelWrapper.WHITE_TEXTURE : StringUtils.equals(textureString, "default.png") ? textureId : CustomResourceTools.getResourceFromSamePath(idStr, textureString, "png"),
                    null, true, flipTextureV
            );
            for(Map.Entry<String, OptimizedModel.ObjModel> modelEntry : models.entrySet()) {
                rawModels.put(modelEntry.getKey(), new ScriptRawModel(modelEntry.getValue()));
            }
            return rawModels;
        } else {
            throw new IllegalArgumentException("Unsupported model format " + idStr + ".");
        }
    }

    public static ScriptRawModel loadModelPart(String modelName, Identifier modelLocation, boolean flipTextureV) {
        return loadModelParts(modelLocation, flipTextureV).get(modelName);
    }

    @Deprecated
    public static ScriptModelCluster uploadVertArrays(ScriptRawModel scriptRawModel) {
        return upload(scriptRawModel);
    }

    public static ScriptModelCluster upload(ScriptRawModel scriptRawModel) {
        return new ScriptModelCluster(scriptRawModel);
    }
}
