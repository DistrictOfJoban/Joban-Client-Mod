package com.lx862.jcm.test;

import com.lx862.jcm.mod.scripting.psd.*;
import org.mtr.mapping.holder.*;
import org.mtr.mod.block.*;

public final class PSDWallTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static BlockState glass(Direction facing, IBlock.DoubleBlockHalf half) {
        return new BlockState(new BlockPSDAPGGlassBase()).with(BlockPSDAPGBase.FACING, facing).with(IBlock.HALF, half);
    }

    private static void door(World world, BlockPos pos, Direction facing, IBlock.EnumSide side, boolean locked, double value) {
        world.blocks.put(pos, new BlockState(new BlockPSDAPGDoorBase())
                .with(BlockPSDAPGBase.FACING, facing).with(IBlock.HALF, IBlock.DoubleBlockHalf.LOWER)
                .with(BlockPSDAPGDoorBase.SIDE, side).with(BlockPSDAPGDoorBase.UNLOCKED, !locked));
        world.entities.put(pos, new BlockEntity(new BlockPSDAPGDoorBase.BlockEntityBase(value)));
    }

    private static void checkWall(Direction facing) {
        final World world = new World();
        final BlockPos anchor = new BlockPos(10, 2, 10);
        final Direction along = facing.rotateYClockwise();
        world.blocks.put(anchor, glass(facing, IBlock.DoubleBlockHalf.LOWER));
        door(world, anchor.offset(along, 1), facing, IBlock.EnumSide.LEFT, false, 0);
        door(world, anchor.offset(along, 2), facing, IBlock.EnumSide.RIGHT, false, 0);
        door(world, anchor.offset(along, 3), facing, IBlock.EnumSide.LEFT, true, 1);
        door(world, anchor.offset(along, 4), facing, IBlock.EnumSide.RIGHT, true, 1);
        final PSDWallScanner.WallRun run = PSDWallScanner.scan(world, anchor);
        check(run.doors.size() == 2, facing + ": adjacent pairs must remain distinct units");
        check(!run.doors.get(0).open && !run.doors.get(0).locked, facing + ": next pair contaminated first pair's state");
        check(run.doors.get(1).open && run.doors.get(1).locked, facing + ": second pair lost its own state");
        final BlockPos first = anchor.offset(along, 1), last = anchor.offset(along, 2);
        final double expectedCenter = along.getAxis() == Axis.X
                ? (first.getX() + last.getX()) / 2D + 0.5
                : (first.getZ() + last.getZ()) / 2D + 0.5;
        check(run.doors.get(0).alongCenter == expectedCenter, facing + ": negative-axis center is shifted");

        final World nextDimension = new World();
        nextDimension.blocks.put(anchor, glass(facing, IBlock.DoubleBlockHalf.LOWER));
        check(PSDWallScanner.scan(nextDimension, anchor).doors.isEmpty(), "world replacement reused cached door geometry");
    }

    private static void checkSections() {
        final World world = new World();
        final BlockPos anchor = new BlockPos(0, 0, 0);
        final Direction facing = Direction.NORTH;
        world.blocks.put(anchor, glass(facing, IBlock.DoubleBlockHalf.LOWER));
        world.blocks.put(anchor.offset(Direction.EAST), glass(facing, IBlock.DoubleBlockHalf.LOWER));
        world.blocks.put(anchor.offset(Direction.EAST, 2), glass(Direction.SOUTH, IBlock.DoubleBlockHalf.LOWER));
        world.blocks.put(anchor.offset(Direction.WEST), glass(facing, IBlock.DoubleBlockHalf.UPPER));
        world.blocks.put(anchor.up(), glass(Direction.SOUTH, IBlock.DoubleBlockHalf.UPPER));
        final PSDRuns.Run run = PSDRuns.glassRun(world, anchor, facing);
        check(run.start.equals(anchor) && run.width == 2, "glass run crossed facing/half boundary");
        check(PSDRuns.glassRunPositions(world, run, facing).size() == 2, "editor bound mismatched upper glass");
        world.blocks.put(anchor, new BlockState(new BlockPSDTop()).with(BlockPSDTop.FACING, facing));
        world.blocks.put(anchor.offset(Direction.EAST), new BlockState(new BlockPSDTop()).with(BlockPSDTop.FACING, Direction.SOUTH));
        check(PSDRuns.topRun(world, anchor, facing).width == 1, "top run crossed facing boundary");
    }

    public static void main(String[] args) {
        for (Direction facing : Direction.values()) checkWall(facing);
        checkSections();
        System.out.println("PSD wall regression: all orientations, adjacent door states, dimension replacement and section boundaries passed");
    }
}
