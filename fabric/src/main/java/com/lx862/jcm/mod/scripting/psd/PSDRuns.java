package com.lx862.jcm.mod.scripting.psd;

import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mod.block.BlockPSDAPGBase;
import org.mtr.mod.block.BlockPSDAPGDoorBase;
import org.mtr.mod.block.BlockPSDAPGGlassBase;
import org.mtr.mod.block.BlockPSDAPGGlassEndBase;
import org.mtr.mod.block.BlockPSDTop;
import org.mtr.mod.block.IBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Yanyang/PSD: wall-section walks for the merged-surface rendering.
 *
 * <p>A PSD wall is an alternating row of door runs and glass segments with a
 * {@code psd_top} band above it. When the player binds a script to one glass
 * column or one {@code psd_top} block, the <b>whole wall section it belongs
 * to</b> changes — the doors between glass segments, and the rest of the
 * psd_top band, keep their own content.</p>
 *
 * <h2>Why the wall below defines the psd_top sections</h2>
 * The sections are derived from the <b>wall blocks below the band</b>, not
 * from the psd_top blocks' own {@code SIDE_EXTENDED} markers: those are only
 * mirrors of a <b>full-height PSD</b> wall below (see
 * {@code BlockPSDTop.getActualState}, which reads the side from
 * {@code BlockPSDGlass}/{@code BlockPSDDoor}/{@code BlockPSDGlassEnd}).
 * Above an <b>APG</b> wall (half-height gates, like JCM's DRL APG) the band
 * is placed by hand and every psd_top keeps its default side — the markers
 * say nothing about the sections. The wall itself, however, always does:
 * every placement (MTR's {@code ItemPSDAPGBase} and JCM's DRL twin) lays a
 * door as one contiguous 2/3-wide run, glass fills between, and the brush
 * merges glass into contiguous segments. So "the brush-merged door's top
 * panels" is exactly the psd_top stretch sitting on one contiguous door
 * run, and "the glass segment's top panels" the stretch on one contiguous
 * glass/glass-end run.</p>
 *
 * <h2>Anchor convention</h2>
 * Both render transforms (glass face, psd_top band) are anchored at a block
 * position and draw their canvas toward {@code facing.rotateYClockwise()}
 * (the wall's "right", canvas +x); the canvas origin sits at the anchor
 * block's left edge. The anchor of a section is therefore its <b>left-most</b>
 * block — the last block reached walking {@code facing.rotateYCounterclockwise()}.</p>
 */
public final class PSDRuns {

    /** Hard cap of one section walk — protects against pathological loops. */
    private static final int MAX_RUN_BLOCKS = 64;

    /** One walked section: the left-most block (render anchor) and the width in blocks. */
    public static final class Run {
        public final BlockPos start;
        public final int width;

        Run(BlockPos start, int width) {
            this.start = start;
            this.width = width;
        }
    }

    /** The kind of wall section a band column sits on. */
    private enum SectionKind {DOOR, GLASS, OTHER}

    private PSDRuns() {
    }

    /**
     * The glass segment a bound glass block belongs to: the contiguous
     * glass/glass-end run at that level — doors and everything else break
     * the chain, so this is exactly the glass the brush merged between the
     * doors. The returned anchor is the segment's left-most block.
     */
    public static Run glassRun(World world, BlockPos pos, Direction facing) {
        final IBlock.DoubleBlockHalf half = IBlock.getStatePropertySafe(world.getBlockState(pos), IBlock.HALF);
        return walk(world, pos, facing, cursor -> {
            final BlockState state = world.getBlockState(cursor);
            return isGlassLike(state)
                    && IBlock.getStatePropertySafe(state, BlockPSDAPGBase.FACING) == facing
                    && IBlock.getStatePropertySafe(state, IBlock.HALF) == half;
        });
    }

    /** Whether the state is a PSD/APG glass or glass-end block (either
     *  half) — the blocks a glass segment chains through. */
    public static boolean isGlass(BlockState state) {
        return isGlassLike(state);
    }

    /** Every binding position of a glass segment: both halves of every
     *  column (the upper half only when the block above the lower half is
     *  glass-like too). Yanyang.6: the editor binds/unbinds the WHOLE
     *  segment — the renderer merges the segment into one canvas, so a
     *  binding that only covers the clicked column left the rest of the
     *  segment unmanageable from any other block. */
    public static List<BlockPos> glassRunPositions(World world, Run run, Direction facing) {
        final List<BlockPos> positions = new ArrayList<>();
        for (int i = 0; i < run.width; i++) {
            final BlockPos lower = run.start.offset(facing.rotateYClockwise(), i);
            positions.add(lower);
            final BlockPos upper = lower.up();
            final BlockState upperState = world.getBlockState(upper);
            if (isGlassLike(upperState)
                    && IBlock.getStatePropertySafe(upperState, BlockPSDAPGBase.FACING) == facing
                    && IBlock.getStatePropertySafe(upperState, IBlock.HALF) == IBlock.DoubleBlockHalf.UPPER) {
                positions.add(upper);
            }
        }
        return positions;
    }

    /** Every binding position of a psd_top group: the group's psd_top
     *  blocks (all of them are psd_top by the group walk). */
    public static List<BlockPos> topRunPositions(Run run, Direction facing) {
        final List<BlockPos> positions = new ArrayList<>();
        for (int i = 0; i < run.width; i++) {
            positions.add(run.start.offset(facing.rotateYClockwise(), i));
        }
        return positions;
    }

    /**
     * The psd_top group a bound top block belongs to: the contiguous
     * psd_top stretch that sits on <b>one wall section</b> — one contiguous
     * door run (the brush-merged door's top panels) or one contiguous glass
     * segment. The band's own SIDE_EXTENDED markers are NOT consulted (they
     * are only meaningful above a full-height PSD wall, see the class doc);
     * psd_tops without a PSD wall below fall back to the plain contiguous
     * psd_top run.
     */
    public static Run topRun(World world, BlockPos pos, Direction facing) {
        final SectionKind kind = sectionKind(world, pos.down());
        final Predicate<BlockPos> chained;
        if (kind == SectionKind.OTHER) {
            chained = cursor -> isPsdTopFacing(world.getBlockState(cursor), facing);
        } else {
            chained = cursor -> isPsdTopFacing(world.getBlockState(cursor), facing) && sectionKind(world, cursor.down()) == kind;
        }
        return walk(world, pos, facing, chained);
    }

    /**
     * Whether the psd_top group containing {@code pos} (see {@link #topRun})
     * carries any TOP script binding — the signal for
     * {@code RenderRouteBaseMixin} to drop MTR's own arrow/route content on
     * <b>that group only</b>: the script canvas spans exactly the group, so
     * the neighboring groups above other doors and glass segments keep MTR's
     * own arrows and route maps.
     */
    public static boolean topGroupHasScriptBinding(World world, BlockPos pos, Direction facing) {
        if (hasTopBinding(pos)) return true;
        if (!isPsdTop(world.getBlockState(pos))) return false;

        final SectionKind kind = sectionKind(world, pos.down());
        final Predicate<BlockPos> chained;
        if (kind == SectionKind.OTHER) {
            chained = cursor -> isPsdTopFacing(world.getBlockState(cursor), facing);
        } else {
            chained = cursor -> isPsdTopFacing(world.getBlockState(cursor), facing) && sectionKind(world, cursor.down()) == kind;
        }

        BlockPos cursor = pos.offset(facing.rotateYCounterclockwise());
        for (int i = 0; i < MAX_RUN_BLOCKS && chained.test(cursor); i++) {
            if (hasTopBinding(cursor)) return true;
            cursor = cursor.offset(facing.rotateYCounterclockwise());
        }
        cursor = pos.offset(facing.rotateYClockwise());
        for (int i = 0; i < MAX_RUN_BLOCKS && chained.test(cursor); i++) {
            if (hasTopBinding(cursor)) return true;
            cursor = cursor.offset(facing.rotateYClockwise());
        }
        return false;
    }

    /**
     * Walks the contiguous section containing {@code pos} along the wall
     * axis: counterclockwise to the section's left-most block (the anchor),
     * then clockwise for the width.
     */
    private static Run walk(World world, BlockPos pos, Direction facing, Predicate<BlockPos> chained) {
        if (!chained.test(pos)) {
            return new Run(pos, 1);
        }
        BlockPos start = pos;
        int width = 1;

        BlockPos cursor = pos.offset(facing.rotateYCounterclockwise());
        while (width < MAX_RUN_BLOCKS && chained.test(cursor)) {
            start = cursor;
            width++;
            cursor = cursor.offset(facing.rotateYCounterclockwise());
        }

        cursor = pos.offset(facing.rotateYClockwise());
        while (width < MAX_RUN_BLOCKS && chained.test(cursor)) {
            width++;
            cursor = cursor.offset(facing.rotateYClockwise());
        }
        return new Run(start, width);
    }

    /** The kind of wall section supporting a band column. */
    private static SectionKind sectionKind(World world, BlockPos pos) {
        final Object block = world.getBlockState(pos).getBlock().data;
        if (block instanceof BlockPSDAPGDoorBase) return SectionKind.DOOR;
        if (block instanceof BlockPSDAPGGlassBase || block instanceof BlockPSDAPGGlassEndBase) return SectionKind.GLASS;
        return SectionKind.OTHER;
    }

    private static boolean isGlassLike(BlockState state) {
        final Object block = state.getBlock().data;
        return block instanceof BlockPSDAPGGlassBase || block instanceof BlockPSDAPGGlassEndBase;
    }

    private static boolean isPsdTop(BlockState state) {
        return state.getBlock().data instanceof BlockPSDTop;
    }

    private static boolean isPsdTopFacing(BlockState state, Direction facing) {
        return isPsdTop(state) && IBlock.getStatePropertySafe(state, BlockPSDTop.FACING) == facing;
    }

    private static boolean hasTopBinding(BlockPos pos) {
        final PSDBinding binding = PSDClientBindings.get(pos);
        return binding != null && binding.type == PSDWrapper.PSDType.TOP;
    }
}
