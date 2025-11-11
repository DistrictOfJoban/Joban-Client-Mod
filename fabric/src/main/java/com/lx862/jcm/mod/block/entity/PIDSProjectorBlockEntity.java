package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.registry.BlockEntities;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.CompoundTag;

public class PIDSProjectorBlockEntity extends PIDSBlockEntity {
    private double x;
    private double y;
    private double z;
    private double rotateX;
    private double rotateY;
    private double rotateZ;
    private double scale;

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
        this.x = compoundTag.getDouble("x1");
        this.y = compoundTag.getDouble("y1");
        this.z = compoundTag.getDouble("z1");
        this.rotateX = compoundTag.getDouble("rotateX");
        this.rotateY = compoundTag.getDouble("rotateY");
        this.rotateZ = compoundTag.getDouble("rotateZ");
        this.scale = compoundTag.getDouble("scale");
    }

    @Override
    public void writeCompoundTag(CompoundTag compoundTag) {
        super.writeCompoundTag(compoundTag);
        compoundTag.putDouble("x1", this.x);
        compoundTag.putDouble("y1", this.y);
        compoundTag.putDouble("z1", this.z);
        compoundTag.putDouble("rotateX", this.rotateX);
        compoundTag.putDouble("rotateY", this.rotateY);
        compoundTag.putDouble("rotateZ", this.rotateZ);
        compoundTag.putDouble("scale", this.scale);
    }

    @Override
    public String getPIDSType() {
        return "pids_projector";
    }

    @Override
    public String getDefaultPresetId() {
        return "rv_pids";
    }

    @Override
    public int getRowAmount() {
        return 4;
    }

    @Override
    public boolean isKeyBlock() {
        return true; // We only have a single block
    }

    public void setData(String[] customMessages, LongAVLTreeSet filteredPlatforms, boolean[] rowHidden, boolean hidePlatformNumber, String pidsPresetId, double x, double y, double z, double rotateX, double rotateY, double rotateZ, double scale) {
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

    public double getX() { return x; }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public double getScale() { return scale; }

    public double getRotateX() { return rotateX; }
    public double getRotateY() { return rotateY; }
    public double getRotateZ() { return rotateZ; }
}
