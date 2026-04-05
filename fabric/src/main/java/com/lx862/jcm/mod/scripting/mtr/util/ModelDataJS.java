package com.lx862.jcm.mod.scripting.mtr.util;

import com.lx862.jcm.mixin.modded.mtr.ObjModelAccessor;
import com.lx862.jcm.mixin.modded.mtr.RawMeshAccessor;
import com.lx862.mtrscripting.util.ScriptVector3f;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.OptimizedModel;
import org.mtr.mapping.render.batch.MaterialProperties;
import org.mtr.mapping.render.model.RawMesh;

import java.util.Collection;
import java.util.List;

public class ModelDataJS {
    public final List<OptimizedModel.ObjModel> models;
    public String renderType;

    public ModelDataJS() {
        this.models = new ObjectArrayList<>();
        this.renderType = "EXTERIOR";
    }

    public ModelDataJS(OptimizedModel.ObjModel... models) {
        this();
        this.models.addAll(new ObjectArrayList<>(models));
    }

    public ModelDataJS(Collection<OptimizedModel.ObjModel> models) {
        this();
        this.models.addAll(models);
    }

    public void append(ModelDataJS other) {
        this.models.addAll(other.models);
    }

    public void applyTranslation(float x, float y, float z) {
        this.models.forEach((rawModel) -> rawModel.applyTranslation(x, y, z));
    }

    @Deprecated
    public void applyRotation(ScriptVector3f vector3f, float angle) {
        float fX = angle * vector3f.x();
        float fY = angle * vector3f.y();
        float fZ = angle * vector3f.z();

        applyRotation(fX, fY, fZ);
    }

    public void applyRotation(float x, float y, float z) {
        this.models.forEach((rawModel) -> rawModel.applyRotation(x, y, z));
    }

    public void applyScale(float x, float y, float z) {
        this.models.forEach((rawModel) -> rawModel.applyScale(x, y, z));
    }

    public void applyMirror(boolean x, boolean y, boolean z) {
        this.models.forEach((rawModel) -> rawModel.applyMirror(x, y, z));
    }

    public void applyUVMirror(boolean u, boolean v) {
        this.models.forEach(model -> {
            ((ObjModelAccessor)(Object)model).getRawMeshes().forEach(rawMesh -> {
                rawMesh.applyUVMirror(u, v);
            });
        });
    }

    public ModelDataJS copy() {
        List<OptimizedModel.ObjModel> newModels = models.stream().map(
                e -> {
                    ObjModelAccessor accessor = ((ObjModelAccessor) (Object) e);
                    List<RawMesh> meshes = accessor.getRawMeshes().stream().map(f -> new RawMesh(f.materialProperties.shaderType, f)).toList();
                    return ObjModelAccessor.createNew(meshes, false, e.getMinX(), e.getMinY(), e.getMinZ(), e.getMaxX(), e.getMaxY(), e.getMaxZ());
                }).toList();

        return new ModelDataJS(newModels);
    }

    public ModelDataJS copyForMaterialChanges() {
        List<OptimizedModel.ObjModel> newModels = models.stream().map(
                e -> {
                    ObjModelAccessor accessor = ((ObjModelAccessor) (Object) e);
                    List<RawMesh> meshes = accessor.getRawMeshes().stream().map(f -> {
                        MaterialProperties newProp = new MaterialProperties(f.materialProperties.shaderType, f.materialProperties.getTexture(), f.materialProperties.vertexAttributeState.color);
                        RawMesh newMesh = new RawMesh(newProp);
                        /* Change the list reference */
                        ((RawMeshAccessor)(Object)newMesh).setVertices(f.vertices);
                        ((RawMeshAccessor)(Object)newMesh).setFaces(f.faces);

                        return newMesh;
                    }).toList();
                    return ObjModelAccessor.createNew(meshes, false, e.getMinX(), e.getMinY(), e.getMinZ(), e.getMaxX(), e.getMaxY(), e.getMaxZ());
                }).toList();

        return new ModelDataJS(newModels);
    }

    public void replaceTexture(String oldTexture, Identifier newTexture) {
        models.stream().forEach(e -> {
            ((ObjModelAccessor) (Object) e).getRawMeshes().stream().forEach(f -> {
                Identifier id = f.materialProperties.getTexture();
                if (id == null) return;

                String oldPath = id.getPath();
                if (oldPath.substring(oldPath.lastIndexOf("/") + 1).equals(oldTexture)) {
                    f.materialProperties.setTexture(newTexture);
                }
            });
        });
    }

    public void replaceAllTexture(Identifier id) {
        models.stream().forEach(e -> {
            ((ObjModelAccessor) (Object) e).getRawMeshes().stream().forEach(f -> f.materialProperties.setTexture(id));
        });
    }

    public void setAllRenderType(String renderType) {
        this.renderType = renderType;
    }
}