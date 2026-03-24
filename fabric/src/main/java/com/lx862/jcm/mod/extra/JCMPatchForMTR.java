package com.lx862.jcm.mod.extra;

import com.lx862.jcm.mixin.modded.mtrpatch.OptimizedRendererAccessorMixin;
import com.lx862.jcm.mixin.modded.mtrpatch.OptimizedRendererWrapperAccessorMixin;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.OptimizedModel;
import org.mtr.mapping.mapper.OptimizedRenderer;
import org.mtr.mapping.render.shader.ModShaderHandler;
import org.mtr.mapping.render.shader.ShaderManager;
import org.mtr.mod.client.CustomResourceLoader;
import org.mtr.mod.resource.CustomResourceTools;

import java.nio.file.FileSystems;
import java.util.Locale;

public class JCMPatchForMTR {
    public static final String[] mtr3GangwayTextures = {"a_train", "e44", "k_train_ael", "k_train", "k_train_tcl", "m_train", "r211", "s_train", "sp1900"};
    public static final String[] mtr3BarrierTextures = {"london_underground_d78", "mlr", "r179"};
    private static boolean cachedRenderingShadow = false;

    /**
     * Reload optimized rendering shaders.<br>
     * This was originally a part of {@link OptimizedRenderer#beginReload()}, however reloading shaders is quite heavy and memory intensive.<br>
     * It should only be done once during resource reload, hence it has been moved here to increase model loading speed.
     */
    public static void reloadOptimizedRenderingShader() {
        OptimizedRenderer optimizedRenderer = ((OptimizedRendererWrapperAccessorMixin)(Object) CustomResourceLoader.OPTIMIZED_RENDERER_WRAPPER).getOptimizedRenderer();
        ShaderManager shaderManager = ((OptimizedRendererAccessorMixin)(Object) optimizedRenderer).getShaderManager();
        shaderManager.reloadShaders();
    }

    /**
     * Resolve from a relative path to an absolute path.<br>
     * Unlike MTR's method, this can resolve path traversals like ../, which is used in NTE resource packs
     * @param basePath The base identifier path
     * @param resource The relative path
     * @param extension The file extension
     * @return An identifier with the absolute path.
     */
    public static Identifier resolveRelativePath(String basePath, String resource, String extension) {
        if (resource.contains(":")) { // Assume it is already an identifier
            return CustomResourceTools.formatIdentifierWithDefault(resource, extension);
        } else {
            Identifier basePathId = new Identifier(basePath);
            String relative = resource.toLowerCase(Locale.ROOT).replace('\\', '/');

            String resolvedPath = FileSystems.getDefault().getPath(basePathId.getPath()).getParent().resolve(relative)
                    .normalize().toString().replace('\\', '/');
            return CustomResourceTools.formatIdentifier(basePathId.getNamespace() + ":" + resolvedPath, extension);
        }
    }

    /**
     * MTR doesn't recognize several legacy NTE OBJ material names, this method resolves the legacy name and return an appropriate ShaderType.
     * @param type Legacy NTE OBJ material name
     * @return ShaderType if legacy NTE material is matched. Null otherwise.
     */
    public static OptimizedModel.ShaderType getNTEShaderTypeMigration(String type) {
        switch(type.toLowerCase(Locale.ENGLISH)) {
            case "exteriortranslucent":
                return OptimizedModel.ShaderType.TRANSLUCENT;
            case "lighttranslucent":
                return OptimizedModel.ShaderType.TRANSLUCENT_GLOWING;
            case "interiortranslucent":
                return OptimizedModel.ShaderType.TRANSLUCENT_BRIGHT;
        }
        return null;
    }

    public static void updateRenderingShadow() {
        cachedRenderingShadow = ModShaderHandler.renderingShadows();
    }

    public static boolean isRenderingShadow() {
        return cachedRenderingShadow;
    }
}
