package com.lx862.jcm.mod.scripting.mtr.util;

import com.lx862.mtrscripting.util.ScriptVector3f;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.mapper.OptimizedModel;

import java.util.List;

public class ScriptRawModel {
    public final List<OptimizedModel.ObjModel> models;

    public ScriptRawModel() {
        this.models = new ObjectArrayList<>();
    }

    public ScriptRawModel(OptimizedModel.ObjModel... models) {
        this();
        this.models.addAll(new ObjectArrayList<>(models));
    }

    public void append(ScriptRawModel other) {
        this.models.addAll(other.models);
    }

    public void applyTranslation(double x, double y, double z) {
        this.models.forEach((rawModel) -> rawModel.applyTranslation(x, y, z));
    }

    @Deprecated
    public void applyRotation(ScriptVector3f vector3f, double angle) {
        double fX = angle * vector3f.x();
        double fY = angle * vector3f.y();
        double fZ = angle * vector3f.z();

        applyRotation(fX, fY, fZ);
    }

    public void applyRotation(double x, double y, double z) {
        this.models.forEach((rawModel) -> rawModel.applyRotation(x, y, z));
    }

    public void applyScale(double x, double y, double z) {
        this.models.forEach((rawModel) -> rawModel.applyScale(x, y, z));
    }

    public void applyMirror(boolean x, boolean y, boolean z) {
        this.models.forEach((rawModel) -> rawModel.applyMirror(x, y, z));
    }
}