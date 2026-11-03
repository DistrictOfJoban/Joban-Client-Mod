package com.lx862.jcm.mod.scripting.psd;

import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.core.data.Platform;
import org.mtr.core.data.Position;
import org.mtr.core.data.Route;
import org.mtr.core.data.TransportMode;
import org.mtr.core.operation.CarDetails;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mod.block.BlockPSDAPGBase;
import org.mtr.mod.block.BlockPSDAPGDoorBase;
import org.mtr.mod.block.BlockPSDTop;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.client.CustomResourceLoader;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Walks the contiguous PSD wall run a bound glass/top block belongs to and
 * derives the data the PSD scripting API exposes:
 *
 * <ul>
 *   <li>the wall's facing and its block span (glass / door / glass-end blocks
 *       chained along the platform edge);</li>
 *   <li>the door units — each unit is one sliding panel group of adjacent
 *       {@code psd_door} blocks (a LEFT+RIGHT pair, or LEFT+CENTER+RIGHT for
 *       the 3-block style) — numbered 1..N along the wall, with their current
 *       open/locked state read straight off the door block entities;</li>
 *   <li>an <b>approximate</b> prediction of which door units a train will
 *       open: MTR stops a dwelling vehicle roughly centered on the platform
 *       midpoint, so a unit is served when its center lies within half the
 *       train length of the platform midpoint (plus a tolerance for the
 *       coupling gaps). The train length is summed from the per-car vehicle
 *       resources via the arrival's {@link CarDetails}; when the resource is
 *       unknown, a per-car fallback of {@value #FALLBACK_CAR_LENGTH} blocks
 *       is used.</li>
 * </ul>
 *
 * <p>Runs are cached per anchor position with a short TTL — the walk is a
 * handful of block reads, but the door open state must be re-read every frame,
 * so the cached run keeps positions/units and only refreshes the live values.
 * All access is from the client render thread.</p>
 */
public final class PSDWallScanner {

    /** Per-car fallback length (blocks) when the vehicle resource is unknown. */
    private static final double FALLBACK_CAR_LENGTH = 20;
    /** Extra tolerance around the half-train span (coupling gaps, rounding). */
    private static final double PREDICTION_TOLERANCE = 1.5;
    /** Wall geometry re-scan interval. */
    private static final long RESCAN_NANOS = 1_000_000_000L;
    /** Hard cap of one wall run — protects against pathological loops. */
    private static final int MAX_RUN_BLOCKS = 1024;

    private static final Map<BlockPos, CachedRun> CACHE = new HashMap<>();

    private PSDWallScanner() {
    }

    public static final class DoorUnit {
        /** Lower-half position of the unit's first door block. */
        public final BlockPos pos;
        /** 1-based index counted in wall direction (facing.rotateYClockwise()). */
        public final int number;
        /** World along-axis coordinate of the unit center (axis = rotateYClockwise of facing). */
        public final double alongCenter;
        public boolean open;
        public boolean locked;

        DoorUnit(BlockPos pos, int number, double alongCenter) {
            this.pos = pos;
            this.number = number;
            this.alongCenter = alongCenter;
        }
    }

    public static final class WallRun {
        public final boolean valid;
        /** Facing of the wall (points toward the track, like the block property). */
        public final Direction facing;
        public final List<DoorUnit> doors;
        /** Current max door value over the wall doors, 0..1. */
        public double doorValue;
        /** The glass-level (lower half) anchor block this run was walked from. */
        public final BlockPos glassAnchor;

        WallRun(boolean valid, Direction facing, List<DoorUnit> doors, BlockPos glassAnchor) {
            this.valid = valid;
            this.facing = facing;
            this.doors = doors;
            this.glassAnchor = glassAnchor;
        }
    }

    private static final class CachedRun {
        WallRun run;
        long scannedAtNanos;
    }

    /** Drops the whole cache (world leave / resource reload). */
    public static void reset() {
        CACHE.clear();
    }

    /**
     * Scans (or re-reads) the wall run for a bound block. {@code anchor} may
     * be a PSD glass lower/upper half, a door half, or a psd_top block; the
     * walk itself always happens at the glass level.
     */
    public static WallRun scan(World world, BlockPos anchor) {
        final BlockPos glassAnchor = resolveGlassAnchor(world, anchor);
        if (glassAnchor == null) {
            return new WallRun(false, Direction.NORTH, new ArrayList<>(), anchor);
        }

        final long now = System.nanoTime();
        CachedRun cached = CACHE.get(glassAnchor);
        if (cached == null || now - cached.scannedAtNanos > RESCAN_NANOS) {
            cached = new CachedRun();
            cached.run = walk(world, glassAnchor);
            cached.scannedAtNanos = now;
            CACHE.put(glassAnchor, cached);
        }
        refreshDoorStates(world, cached.run);
        return cached.run;
    }

    /** The glass-level (lower half) block under a glass/door half or a psd_top. */
    @Nullable
    private static BlockPos resolveGlassAnchor(World world, BlockPos anchor) {
        final BlockState anchorState = world.getBlockState(anchor);
        final Object anchorBlock = anchorState.getBlock().data;
        if (anchorBlock instanceof BlockPSDAPGDoorBase || anchorBlock instanceof BlockPSDAPGBase) {
            final boolean upper = IBlock.getStatePropertySafe(anchorState, IBlock.HALF) == IBlock.DoubleBlockHalf.UPPER;
            return upper ? anchor.down() : anchor;
        }
        if (anchorBlock instanceof BlockPSDTop) {
            for (int down = 1; down <= 3; down++) {
                final BlockPos below = anchor.down(down);
                final BlockState belowState = world.getBlockState(below);
                if (belowState.getBlock().data instanceof BlockPSDAPGBase) {
                    final boolean upper = IBlock.getStatePropertySafe(belowState, IBlock.HALF) == IBlock.DoubleBlockHalf.UPPER;
                    return upper ? below.down() : below;
                }
            }
        }
        return null;
    }

    private static WallRun walk(World world, BlockPos glassAnchor) {
        final BlockState anchorState = world.getBlockState(glassAnchor);
        if (!(anchorState.getBlock().data instanceof BlockPSDAPGBase)) {
            return new WallRun(false, Direction.NORTH, new ArrayList<>(), glassAnchor);
        }

        final Direction facing = IBlock.getStatePropertySafe(anchorState, BlockPSDAPGBase.FACING);
        final Direction wallAxis = facing.rotateYClockwise();

        // Walk both directions from the anchor while the chain stays PSD wall blocks.
        final ObjectArrayList<BlockPos> rightSide = new ObjectArrayList<>(); // +wallAxis, nearest first
        BlockPos cursor = glassAnchor.offset(wallAxis);
        while (isWallBlock(world, cursor) && rightSide.size() < MAX_RUN_BLOCKS) {
            rightSide.add(cursor);
            cursor = cursor.offset(wallAxis);
        }
        final ObjectArrayList<BlockPos> leftSide = new ObjectArrayList<>(); // -wallAxis, nearest first
        cursor = glassAnchor.offset(wallAxis.getOpposite());
        while (isWallBlock(world, cursor) && leftSide.size() < MAX_RUN_BLOCKS) {
            leftSide.add(cursor);
            cursor = cursor.offset(wallAxis.getOpposite());
        }

        // Assemble in wall direction (leftmost -> rightmost).
        final List<BlockPos> run = new ArrayList<>(leftSide.size() + 1 + rightSide.size());
        for (int i = leftSide.size() - 1; i >= 0; i--) run.add(leftSide.get(i));
        run.add(glassAnchor);
        run.addAll(rightSide);

        // Group door blocks into units and number them along the run.
        final List<DoorUnit> doors = new ArrayList<>();
        DoorUnit current = null;
        double currentFirstAlong = 0;
        int unitWidth = 0;
        for (BlockPos pos : run) {
            final BlockState state = world.getBlockState(pos);
            if (state.getBlock().data instanceof BlockPSDAPGDoorBase
                    && IBlock.getStatePropertySafe(state, IBlock.HALF) == IBlock.DoubleBlockHalf.LOWER) {
                final IBlock.EnumSide side = IBlock.getStatePropertySafe(state, BlockPSDAPGDoorBase.SIDE);
                if (current == null || side == IBlock.EnumSide.LEFT) {
                    currentFirstAlong = alongOf(pos, wallAxis);
                    unitWidth = 1;
                    current = new DoorUnit(pos, doors.size() + 1, currentFirstAlong + 0.5);
                    doors.add(current);
                } else {
                    // MIDDLE / RIGHT panel of the same unit — widen its center.
                    unitWidth++;
                    current = replaceCenter(doors, current, currentFirstAlong + unitWidth * 0.5);
                }
            } else {
                current = null;
                unitWidth = 0;
            }
        }

        return new WallRun(true, facing, doors, glassAnchor);
    }

    private static DoorUnit replaceCenter(List<DoorUnit> doors, DoorUnit unit, double newCenter) {
        if (unit.alongCenter == newCenter) return unit;
        final DoorUnit replacement = new DoorUnit(unit.pos, unit.number, newCenter);
        replacement.open = unit.open;
        replacement.locked = unit.locked;
        doors.set(doors.size() - 1, replacement);
        return replacement;
    }

    private static boolean isWallBlock(World world, BlockPos pos) {
        final Object block = world.getBlockState(pos).getBlock().data;
        return block instanceof BlockPSDAPGBase;
    }

    private static double alongOf(BlockPos pos, Direction wallAxis) {
        return wallAxis.getAxis() == Direction.Axis.X ? pos.getX() : pos.getZ();
    }

    private static double alongOf(Position position, Direction wallAxis) {
        return wallAxis.getAxis() == Direction.Axis.X ? position.getX() : position.getZ();
    }

    /** Re-reads the live door values/states of a cached run (every frame). */
    private static void refreshDoorStates(World world, WallRun run) {
        double maxValue = 0;
        for (DoorUnit door : run.doors) {
            // A unit's panels share the door value through the pair lookup in
            // BlockPSDAPGDoorBase.BlockEntityBase.getDoorValue(); reading each
            // lower-half block of the unit's span covers both panel blocks.
            double value = 0;
            boolean locked = false;
            BlockPos cursor = door.pos;
            for (int i = 0; i < 3; i++) { // a unit is at most 3 blocks wide
                final BlockState state = world.getBlockState(cursor);
                if (!(state.getBlock().data instanceof BlockPSDAPGDoorBase)) break;
                locked |= !IBlock.getStatePropertySafe(state, BlockPSDAPGDoorBase.UNLOCKED);
                final org.mtr.mapping.holder.BlockEntity blockEntity = world.getBlockEntity(cursor);
                if (blockEntity != null && blockEntity.data instanceof BlockPSDAPGDoorBase.BlockEntityBase) {
                    value = Math.max(value, ((BlockPSDAPGDoorBase.BlockEntityBase) blockEntity.data).getDoorValue());
                }
                cursor = cursor.offset(run.facing.rotateYClockwise());
            }
            door.open = value > 0;
            door.locked = locked;
            maxValue = Math.max(maxValue, value);
        }
        run.doorValue = maxValue;
    }

    /**
     * The door unit numbers that will open for one arrival. For a train
     * already dwelling at the platform the currently-open units are the
     * truth; otherwise the centered-train approximation decides.
     *
     * @param arrived  whether the arrival's stop time has passed (dwell)
     * @param cars     the arrival's per-car details (may be empty)
     * @param carCount the arrival's car count
     * @param platform the platform the PSD serves (may be null — then only
     *                 actually-open doors are reported)
     * @param route    the arrival's route (for the vehicle resource lookup)
     */
    public static List<Integer> predictDoorNumbers(PSDWallScanner.WallRun run, boolean arrived, List<CarDetails> cars, int carCount, @Nullable Platform platform, @Nullable Route route, List<Integer> currentlyOpenNumbers) {
        /* A dwelling train reports the REAL open doors — but between arrival
           and the door animation starting (and again while closing) the wall
           reads fully closed; fall back to the prediction in that window so
           scripts keep showing which doors WILL open. */
        if (arrived && !currentlyOpenNumbers.isEmpty()) return currentlyOpenNumbers;
        if (run.doors.isEmpty() || platform == null) return new ArrayList<>();

        final double trainLength = trainLength(cars, carCount, route);
        final double platformMidAlong = alongOf(platform.getMidPosition(), run.facing.rotateYClockwise());
        final double halfSpan = trainLength / 2 + PREDICTION_TOLERANCE;

        final List<Integer> result = new ArrayList<>();
        for (DoorUnit door : run.doors) {
            if (!door.locked && Math.abs(door.alongCenter - platformMidAlong) <= halfSpan) {
                result.add(door.number);
            }
        }
        return result;
    }

    private static double trainLength(List<CarDetails> cars, int carCount, @Nullable Route route) {
        if (cars.isEmpty()) {
            return Math.max(1, carCount) * FALLBACK_CAR_LENGTH;
        }
        double total = 0;
        for (CarDetails car : cars) {
            final double[] length = {FALLBACK_CAR_LENGTH};
            final TransportMode mode = route != null ? route.getTransportMode() : TransportMode.TRAIN;
            try {
                CustomResourceLoader.getVehicleById(mode, car.getVehicleId(), details -> {
                    if (details.left() != null) length[0] = details.left().getLength();
                });
            } catch (Exception e) {
                JCMLogger.warn("Could not resolve vehicle length for \"{}\" (PSD door prediction): {}", car.getVehicleId(), e.getMessage());
            }
            total += length[0];
        }
        return total;
    }
}
