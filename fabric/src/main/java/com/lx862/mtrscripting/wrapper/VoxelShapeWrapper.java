package com.lx862.mtrscripting.wrapper;

import org.mtr.mapping.holder.Block;
import org.mtr.mapping.holder.VoxelShape;
import org.mtr.mapping.holder.VoxelShapes;

public class VoxelShapeWrapper {
    private final VoxelShape impl;

    public VoxelShapeWrapper(VoxelShape impl) {
        this.impl = impl;
    }

    public static VoxelShapeWrapper empty() {
        return new VoxelShapeWrapper(VoxelShapes.empty());
    }

    public static VoxelShapeWrapper fullCube() {
        return new VoxelShapeWrapper(VoxelShapes.fullCube());
    }

    public static VoxelShapeWrapper create(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return new VoxelShapeWrapper(Block.createCuboidShape(minX, minY, minZ, maxX, maxY, maxZ));
    }

    public VoxelShapeWrapper combine(VoxelShapeWrapper other) {
        return new VoxelShapeWrapper(VoxelShapes.union(this.impl, other.impl));
    }

    public VoxelShape impl() {
        return this.impl;
    }
}
