package com.lx862.jcm.mixin.modded.mtrpatch;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.mtr.mapping.render.model.Face;
import org.mtr.mapping.render.model.RawMesh;
import org.mtr.mapping.render.vertex.Vertex;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(value = RawMesh.class, remap = false)
public class RawMeshMixin {
    @Shadow
    @Final
    public List<Vertex> vertices;

    @Shadow
    @Final
    public List<Face> faces;

    /**
     * Vertex distinction was done in OptimizedModel. Doing it again in upload() would waste time.
     */
    @WrapOperation(method = "upload(Lorg/mtr/mapping/render/model/Mesh;Lorg/mtr/mapping/render/vertex/VertexAttributeMapping;)V", at = @At(value = "INVOKE", target = "Lorg/mtr/mapping/render/model/RawMesh;distinct()V"))
    private void skipRawMeshDistinct(RawMesh instance, Operation<Void> original) {
        // Do not invoke original call
    }

    @Inject(method = "distinct", at = @At("HEAD"), cancellable = true)
    private void fastDistinct(CallbackInfo ci) {
        ci.cancel();

        final ObjectArrayList<Vertex> distinctVertices = new ObjectArrayList<>(vertices.size());
        final Object2IntOpenHashMap<Vertex> verticesLookup = new Object2IntOpenHashMap<>(vertices.size());
        final ObjectOpenHashSet<Face> distinctFaces = new ObjectOpenHashSet<>(faces.size());

        for(Face face : faces) {
            for (int i = 0; i < face.vertices.length; i++) {
                final Vertex vertex = vertices.get(face.vertices[i]);
                int newIndex = verticesLookup.computeIfAbsent(vertex, (v) -> {
                    distinctVertices.add(vertex);
                    return distinctVertices.size() - 1;
                });

                face.vertices[i] = newIndex;
            }
            distinctFaces.add(face);
        }

        vertices.clear();
        vertices.addAll(distinctVertices);
        faces.clear();
        faces.addAll(distinctFaces);
    }
}
