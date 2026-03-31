package com.lx862.jcm.mod.scripting.mtr.util;

import com.lx862.jcm.mixin.modded.mtr.ObjModelAccessor;
import com.lx862.mtrscripting.util.ScriptVector3f;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.mapper.OptimizedModel;

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

    public void setAllRenderType(String renderType) {
        this.renderType = renderType;
    }
}