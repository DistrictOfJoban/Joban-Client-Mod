package com.lx862.jcm.mod.scripting.psd;

import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mod.block.BlockPSDAPGGlassBase;
import org.mtr.mod.block.BlockPSDAPGGlassEndBase;
import org.mtr.mod.block.BlockPSDTop;

/**
 * Yanyang/PSD: horizontal "run" walks for the merged-surface rendering.
 *
 * <p>A PSD wall placed with MTR's brush is one row of glass / door / glass-end
 * blocks with a {@code psd_top} band above it. When the player binds a script
 * to one glass column or one {@code psd_top} block, the <b>whole merged
 * surface</b> it belongs to must change — the white panel and the script
 * canvas span the entire contiguous glass segment, and the script canvas
 * spans the entire contiguous psd_top run (the same run MTR's own route map
 * texture spans — see {@code RenderRouteBase.getTextureNumber}, which walks
 * the identical chain to slice the run-wide texture per block).</p>
 *
 * <h2>Anchor convention</h2>
 * Both render transforms (glass face, psd_top band) are anchored at a block
 * position and draw their canvas toward {@code facing.rotateYClockwise()}
 * (the wall's "right", canvas +x); the canvas origin sits at the anchor
 * block's left edge. The anchor of a run is therefore its <b>left-most</b>
 * block — the last block reached walking {@code facing.rotateYCounterclockwise()}.</p>
 */
public final class PSDRuns {

    /** Hard cap of one run walk — protects against pathological loops. */
    private static final int MAX_RUN_BLOCKS = 64;

    /** One walked run: the left-most block (render anchor) and the width in blocks. */
    public static final class Run {
        public final BlockPos start;
        public final int width;

        Run(BlockPos start, int width) {
            this.start = start;
            this.width = width;
        }
    }

    private PSDRuns() {
    }

    /**
     * The contiguous glass segment a bound glass block belongs to: glass and
     * glass-end blocks chained along the wall axis at the same Y (doors and
     * everything else break the chain). The returned anchor is the segment's
     * left-most block.
     */
    public static Run glassRun(World world, BlockPos pos, Direction facing) {
        return walk(world, pos, facing, PSDRuns::isGlassLike);
    }

    /**
     * The contiguous {@code psd_top} run a bound top block belongs to — the
     * same chain MTR's route texture spans (every psd_top above the wall:
     * above glass, doors and glass ends alike).
     */
    public static Run topRun(World world, BlockPos pos, Direction facing) {
        return walk(world, pos, facing, state -> state.getBlock().data instanceof BlockPSDTop);
    }

    /**
     * Whether the contiguous psd_top run containing {@code pos} carries any
     * TOP script binding — the signal for {@code RenderRouteBaseMixin} to
     * drop MTR's own arrow/route content on <b>every</b> block of the band,
     * not just the bound one (the script canvas spans the whole run).
     */
    public static boolean topRunHasScriptBinding(World world, BlockPos pos, Direction facing) {
        BlockPos cursor = pos;
        for (int i = 0; i < MAX_RUN_BLOCKS; i++) {
            if (hasTopBinding(cursor)) return true;
            final BlockState state = world.getBlockState(cursor);
            if (!(state.getBlock().data instanceof BlockPSDTop)) break;
            cursor = cursor.offset(facing.rotateYCounterclockwise());
        }
        cursor = pos.offset(facing.rotateYClockwise());
        for (int i = 0; i < MAX_RUN_BLOCKS; i++) {
            final BlockState state = world.getBlockState(cursor);
            if (!(state.getBlock().data instanceof BlockPSDTop)) break;
            if (hasTopBinding(cursor)) return true;
            cursor = cursor.offset(facing.rotateYClockwise());
        }
        return false;
    }

    private interface ChainRule {
        boolean chained(BlockState state);
    }

    private static Run walk(World world, BlockPos pos, Direction facing, ChainRule rule) {
        if (!rule.chained(world.getBlockState(pos))) {
            return new Run(pos, 1);
        }
        BlockPos start = pos;
        int width = 1;
        BlockPos cursor = pos.offset(facing.rotateYCounterclockwise());
        while (width < MAX_RUN_BLOCKS && rule.chained(world.getBlockState(cursor))) {
            start = cursor;
            width++;
            cursor = cursor.offset(facing.rotateYCounterclockwise());
        }
        cursor = pos.offset(facing.rotateYClockwise());
        while (width < MAX_RUN_BLOCKS && rule.chained(world.getBlockState(cursor))) {
            width++;
            cursor = cursor.offset(facing.rotateYClockwise());
        }
        return new Run(start, width);
    }

    private static boolean isGlassLike(BlockState state) {
        final Object block = state.getBlock().data;
        return block instanceof BlockPSDAPGGlassBase || block instanceof BlockPSDAPGGlassEndBase;
    }

    private static boolean hasTopBinding(BlockPos pos) {
        final PSDBinding binding = PSDClientBindings.get(pos);
        return binding != null && binding.type == PSDWrapper.PSDType.TOP;
    }
}
