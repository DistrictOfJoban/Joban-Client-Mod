package com.lx862.jcm.mixin.modded.mtr;

import org.mtr.mapping.render.model.Face;
import org.mtr.mapping.render.model.RawMesh;
import org.mtr.mapping.render.vertex.Vertex;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(value = RawMesh.class, remap = false)
public interface RawMeshAccessor {
    @Accessor("vertices")
    @Mutable
    void setVertices(List<Vertex> vertices);

    @Accessor("faces")
    @Mutable
    void setFaces(List<Face> faces);
}
