package com.lx862.jcm.mod.extra;

import com.lx862.jcm.mixin.modded.mtrpatch.OptimizedRendererAccessorMixin;
import com.lx862.jcm.mixin.modded.mtrpatch.OptimizedRendererWrapperAccessorMixin;
import it.unimi.dsi.fastutil.longs.Long2IntArrayMap;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.OptimizedRenderer;
import org.mtr.mapping.render.shader.ModShaderHandler;
import org.mtr.mapping.render.shader.ShaderManager;
import org.mtr.mod.client.CustomResourceLoader;
import org.mtr.mod.resource.CustomResourceTools;

import java.nio.file.FileSystems;
import java.util.Locale;

public class JCMPatchForMTR {
    public static Identifier LIFT_DING_SOUND = new Identifier("minecraft:block.note_block.pling");
    private static boolean cachedRenderingShadow = false;
    private static Long2IntArrayMap liftInstructions = new Long2IntArrayMap();

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

    public static void updateRenderingShadow() {
        cachedRenderingShadow = ModShaderHandler.renderingShadows();
    }

    public static boolean isRenderingShadow() {
        return cachedRenderingShadow;
    }

    public static boolean liftJustArrived(long liftId, int currentInstructionSize) {
        long lastValue = liftInstructions.getOrDefault(liftId, currentInstructionSize);
        boolean liftJustArrived = currentInstructionSize == 0 && lastValue == 1;
        liftInstructions.put(liftId, currentInstructionSize);
        return liftJustArrived;
    }
}
