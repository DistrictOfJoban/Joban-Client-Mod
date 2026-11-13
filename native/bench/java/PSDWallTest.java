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

    private static void checkSectionLength(Direction facing, int startCoordinate, int length) {
        final World glassWorld = new World(), topWorld = new World();
        final BlockPos origin = new BlockPos(0, 4, 0);
        final Direction along = facing.rotateYClockwise();
        final int endCoordinate = startCoordinate + length - 1;
        for (int coordinate = startCoordinate; coordinate <= endCoordinate; coordinate++) {
            final BlockPos pos = origin.offset(along, coordinate);
            glassWorld.blocks.put(pos, glass(facing, IBlock.DoubleBlockHalf.LOWER));
            topWorld.blocks.put(pos, new BlockState(new BlockPSDTop()).with(BlockPSDTop.FACING, facing));
            topWorld.blocks.put(pos.down(), glass(facing, IBlock.DoubleBlockHalf.LOWER));
        }
        PSDClientBindings.bindings.clear();
        final BlockPos bound = origin.offset(along, startCoordinate);
        PSDClientBindings.bindings.put(bound, new PSDBinding(bound, "test:top", false, PSDWrapper.PSDType.TOP));
        for (int coordinate = startCoordinate; coordinate <= endCoordinate; coordinate++) {
            final BlockPos pos = origin.offset(along, coordinate);
            final int gridStart = Math.floorDiv(coordinate, 64) * 64;
            final int expectedStart = length <= 64 ? startCoordinate : Math.max(startCoordinate, gridStart);
            final int expectedEnd = length <= 64 ? endCoordinate : Math.min(endCoordinate, gridStart + 63);
            final BlockPos expectedAnchor = origin.offset(along, expectedStart);
            glassWorld.reads = 0;
            final PSDRuns.Run glassRun = PSDRuns.glassRun(glassWorld, pos, facing);
            check(glassRun.start.equals(expectedAnchor) && glassRun.width == expectedEnd - expectedStart + 1,
                    facing + ": glass anchor/width depends on binding position at " + coordinate + " in length " + length);
            check(glassWorld.reads <= 140, facing + ": glass segmentation performed an unbounded scan");
            final PSDRuns.Run topRun = PSDRuns.topRun(topWorld, pos, facing);
            check(topRun.start.equals(expectedAnchor) && topRun.width == glassRun.width,
                    facing + ": top and glass disagree on deterministic section at " + coordinate);
            if (length > 64) {
                check(expectedStart >= gridStart && expectedEnd < gridStart + 64,
                        facing + ": long section crossed its aligned interval");
            }
            final boolean sameCanvas = length <= 64 || Math.floorDiv(coordinate, 64) == Math.floorDiv(startCoordinate, 64);
            check(PSDRuns.topGroupHasScriptBinding(topWorld, pos, facing) == sameCanvas,
                    facing + ": a different canvas suppressed this top panel's vanilla content at " + coordinate);
        }
        PSDClientBindings.bindings.clear();
    }

    public static void main(String[] args) {
        for (Direction facing : Direction.values()) {
            checkWall(facing);
            checkSectionLength(facing, 60, 8);   // Short sections cross grid lines unchanged.
            checkSectionLength(facing, -17, 64); // Exactly the limit is still one canvas.
            checkSectionLength(facing, 17, 65);
            checkSectionLength(facing, -70, 130);
        }
        checkSections();
        System.out.println("PSD wall regression: all orientations, door states, world replacement and deterministic 8/64/65/130-block sections passed");
    }
}
