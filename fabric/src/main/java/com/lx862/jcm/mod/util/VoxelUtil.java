package com.lx862.jcm.mod.util;

import org.mtr.mapping.holder.Box;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.VoxelShape;
import org.mtr.mapping.holder.VoxelShapes;

public class VoxelUtil {
    public static VoxelShape getShape16(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return VoxelShapes.cuboid(new Box(minX / 16, minY / 16, minZ / 16, maxX / 16, maxY / 16, maxZ / 16));
    }

    public static VoxelShape getDirectionalShape16(Direction direction, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        switch (direction) {
            case NORTH:
                return VoxelUtil.getShape16(minX, minY, minZ, maxX, maxY, maxZ);
            case SOUTH:
                return VoxelUtil.getShape16(16 - maxX, minY, 16 - maxZ, 16 - minX, maxY, 16 - minZ);
            case EAST:
                return VoxelUtil.getShape16(16 - maxZ, minY, minX, 16 - minZ, maxY, maxX);
            case WEST:
                return VoxelUtil.getShape16(minZ, minY, 16 - maxX, maxZ, maxY, 16 - minX);
            default:
                return VoxelShapes.empty();
        }
    }
}
