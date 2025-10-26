package com.lx862.jcm.mod.scripting.mtr.eyecandy;

import com.lx862.mtrscripting.util.Vector3dWrapper;
import org.mtr.mapping.holder.World;
import org.mtr.mod.block.BlockEyeCandy;

public class EyecandyBlockEntityWrapper {
    private final BlockEyeCandy.BlockEntity be;

    public EyecandyBlockEntityWrapper(BlockEyeCandy.BlockEntity be) {
        this.be = be;
    }

    public String getModelId() {
        return be.getModelId();
    }

    public float getTranslateX() {
        return be.getTranslateX();
    }

    public float getTranslateY() {
        return be.getTranslateY();
    }

    public float getTranslateZ() {
        return be.getTranslateZ();
    }

    public float getRotateX() {
        return be.getRotateX();
    }

    public float getRotateY() {
        return be.getRotateY();
    }

    public float getRotateZ() {
        return be.getRotateZ();
    }

    public boolean getFullBrightness() {
        return be.getFullBrightness();
    }

    public Vector3dWrapper pos() {
        Vector3dWrapper realPos = new Vector3dWrapper(be.getPos2());
        realPos.add(getTranslateX(), getTranslateY(), getTranslateZ());
        return realPos;
    }

    public Vector3dWrapper offsetPos() {
        return new Vector3dWrapper(getTranslateX(), getTranslateY(), getTranslateZ());
    }

    public Vector3dWrapper blockPos() {
        return new Vector3dWrapper(be.getPos2());
    }

    public World getWorld() {
        return be.getWorld2();
    }
}
