package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.registry.BlockEntities;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.CompoundTag;

public class PIDSProjectorBlockEntity extends PIDSBlockEntity {
    private float x;
    private float y;
    private float z;
    private float rotateX;
    private float rotateY;
    private float rotateZ;
    private float scale;

    public PIDSProjectorBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockEntities.PIDS_PROJECTOR.get(), blockPos, blockState);
        this.x = 0;
        this.y = 0;
        this.z = 0;
        this.scale = 1.0f;
        this.rotateX = 0;
        this.rotateY = 0;
        this.rotateZ = 0;
    }

    @Override
    public void readCompoundTag(CompoundTag compoundTag) {
        super.readCompoundTag(compoundTag);

        this.x = compoundTag.getFloat("x1");
        this.y = compoundTag.getFloat("y1");
        this.z = compoundTag.getFloat("z1");
        this.rotateX = compoundTag.getFloat("rotateX");
        this.rotateY = compoundTag.getFloat("rotateY");
        this.rotateZ = compoundTag.getFloat("rotateZ");
        this.scale = compoundTag.getFloat("scale");
    }

    @Override
    public void writeCompoundTag(CompoundTag compoundTag) {
        super.writeCompoundTag(compoundTag);

        compoundTag.putFloat("x1", this.x);
        compoundTag.putFloat("y1", this.y);
        compoundTag.putFloat("z1", this.z);
        compoundTag.putFloat("rotateX", this.rotateX);
        compoundTag.putFloat("rotateY", this.rotateY);
        compoundTag.putFloat("rotateZ", this.rotateZ);
        compoundTag.putFloat("scale", this.scale);
    }

    @Override
    public String getDefaultPresetId() {
        return "rv_pids";
    }

    @Override
    public int getRowAmount() {
        return 4;
    }

    public void setData(String[] customMessages, LongAVLTreeSet filteredPlatforms, boolean[] rowHidden, boolean hidePlatformNumber, String pidsPresetId, float x, float y, float z, float rotateX, float rotateY, float rotateZ, float scale) {
        super.setData(customMessages, filteredPlatforms, rowHidden, hidePlatformNumber, pidsPresetId);

        this.x = x;
        this.y = y;
        this.z = z;
        this.scale = scale;
        this.rotateX = rotateX;
        this.rotateY = rotateY;
        this.rotateZ = rotateZ;

        this.markDirty2();
    }

    public float getX() { return x; }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public float getScale() { return scale; }

    public float getRotateX() { return rotateX; }
    public float getRotateY() { return rotateY; }
    public float getRotateZ() { return rotateZ; }
}
