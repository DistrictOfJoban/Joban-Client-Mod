package com.lx862.jcm.mixin.modded.mtrpatch;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.mtr.mapping.render.model.RawMesh;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = RawMesh.class, remap = false)
public class RawMeshMixin {
    /**
     * Vertex distinction was done in OptimizedModel. Doing it again in upload() would waste time.
     */
    @WrapOperation(method = "upload(Lorg/mtr/mapping/render/model/Mesh;Lorg/mtr/mapping/render/vertex/VertexAttributeMapping;)V", at = @At(value = "INVOKE", target = "Lorg/mtr/mapping/render/model/RawMesh;distinct()V"))
    private void skipRawMeshDistinct(RawMesh instance, Operation<Void> original) {
        // Do not invoke original call
    }
}
