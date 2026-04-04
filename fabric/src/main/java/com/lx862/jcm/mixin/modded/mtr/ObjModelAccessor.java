package com.lx862.jcm.mixin.modded.mtr;

import org.mtr.mapping.mapper.OptimizedModel;
import org.mtr.mapping.render.model.RawMesh;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(value = OptimizedModel.ObjModel.class, remap = false)
public interface ObjModelAccessor {
    @Invoker("<init>")
    static OptimizedModel.ObjModel createNew(List<RawMesh> rawMeshes, boolean flipTextureV, float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        throw new AssertionError();
    }

    @Accessor
    List<RawMesh> getRawMeshes();
}