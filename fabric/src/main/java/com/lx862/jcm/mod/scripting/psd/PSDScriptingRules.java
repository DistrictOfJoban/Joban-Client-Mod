package com.lx862.jcm.mod.scripting.psd;

import com.lx862.jcm.mixin.mtrscripting.BlockPSDAPGBaseAccessor;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.World;
import org.mtr.mod.block.BlockPSDAPGBase;
import org.mtr.mod.block.BlockPSDAPGDoorBase;
import org.mtr.mod.block.BlockPSDAPGGlassBase;

/**
 * Which PSD surfaces may carry scripts (Yanyang): <b>full-height platform
 * screen doors only</b>. Half-height gates (MTR's APG, JCM's DRL twin and
 * any addon building on MTR's PSD classes, such as the Tianjin Metro mod)
 * have their scripting support removed — the editor does not open on them,
 * the server rejects bind packets for them and the renderer skips any
 * legacy binding still stored on them.
 *
 * <p>The half/full decision reuses MTR's own semantic,
 * {@code BlockPSDAPGBase#isAPG()} — the method MTR's
 * {@code getOutlineShape2} uses to give half-height gates their short upper
 * half. It is {@code protected}, so it is invoked through the
 * {@link BlockPSDAPGBaseAccessor} mixin invoker. This is the one check that
 * classifies MTR's own gates, JCM's DRL gates (which extend MTR's APG
 * blocks) and third-party gates alike: every Tianjin Metro APG block —
 * doors, glass and glass ends — overrides {@code isAPG()} to
 * {@code true}, while its full-height PSD blocks extend the PSD bases
 * directly and inherit {@code false}.</p>
 */
public final class PSDScriptingRules {

    private PSDScriptingRules() {
    }

    /**
     * Whether the wall block is a <b>half-height</b> gate (APG-style):
     * MTR's {@code isAPG()} semantic, honouring addon overrides.
     */
    public static boolean isHalfHeightPsd(Object block) {
        return block instanceof BlockPSDAPGBase && ((BlockPSDAPGBaseAccessor) block).jcm$invokeIsAPG();
    }

    /**
     * A glass (or glass end — glass ends extend the glass base) block that
     * is eligible for "glass" script bindings: a <b>full-height</b> pane.
     */
    public static boolean isScriptableGlass(Object block) {
        return block instanceof BlockPSDAPGGlassBase && !isHalfHeightPsd(block);
    }

    /**
     * A wall block (door / glass / glass end) that is <b>full-height</b> —
     * the only wall kind a {@code psd_top} band may be scripted on.
     */
    public static boolean isFullHeightWallBlock(Object block) {
        if (!(block instanceof BlockPSDAPGDoorBase || block instanceof BlockPSDAPGGlassBase)) return false;
        return !isHalfHeightPsd(block);
    }

    /**
     * Whether a {@code psd_top} block at {@code pos} is eligible for "top"
     * script bindings: the block directly below must be a full-height PSD
     * wall block. A half-height gate's hand-placed top band — and any
     * floating psd_top — carries no script.
     */
    public static boolean isScriptableTop(World world, BlockPos pos) {
        return isFullHeightWallBlock(world.getBlockState(pos.down()).getBlock().data);
    }
}
