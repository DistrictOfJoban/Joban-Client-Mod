package com.lx862.jcm.mod.item;

import com.lx862.jcm.mod.network.gui.ClientHelper;
import com.lx862.jcm.mod.scripting.psd.PSDRuns;
import com.lx862.jcm.mod.scripting.psd.PSDScriptingRules;
import com.lx862.jcm.mod.scripting.psd.PSDWrapper;
import org.mtr.mapping.holder.ActionResult;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.ItemSettings;
import org.mtr.mapping.holder.ItemUsageContext;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.ItemExtension;
import org.mtr.mod.block.BlockPSDAPGBase;
import org.mtr.mod.block.BlockPSDAPGGlassBase;
import org.mtr.mod.block.BlockPSDTop;
import org.mtr.mod.block.IBlock;

import java.util.List;

/**
 * The "PSD Script Editor" (屏蔽门脚本编辑器) — right-click any platform screen
 * door glass (or glass end, PSD or APG, from MTR or any addon building on
 * MTR's PSD classes) to bind a {@code psdScripts} entry of type "glass" or a
 * scripted PIDS preset onto it, and any psd_top block to bind a type "top"
 * entry or PIDS preset. Lives in the same creative tab as MTR's brush.
 *
 * <p>Yanyang.6: the editor manages the <b>whole surface the clicked block
 * belongs to</b>, not just the clicked column — the renderer draws one merged
 * canvas per glass segment / psd_top group, so a choice (including the
 * "no script" entry) binds or unbinds <b>every block of that segment or
 * group</b>: both halves of every glass column, or the group's psd_top
 * blocks. Previously the binding lived only on the clicked column's two
 * halves, and trying to unbind from any other block of the same glass left
 * the (visually merged) script running with no way to remove it.</p>
 */
public class ItemPSDScriptEditor extends ItemExtension {

    public ItemPSDScriptEditor(ItemSettings itemSettings) {
        super(itemSettings.maxCount(1));
    }

    @Override
    public ActionResult useOnBlock2(ItemUsageContext context) {
        final World world = context.getWorld();
        final BlockPos pos = context.getBlockPos();
        final BlockState state = world.getBlockState(pos);
        final Object block = state.getBlock().data;

        if (block instanceof BlockPSDAPGGlassBase) {
            /* Yanyang: half-height gates (MTR's APG, JCM's DRL twin, addon
               gates like the Tianjin Metro ones) carry no scripts — only
               full-height PSD panes do. */
            if (PSDScriptingRules.isHalfHeightPsd(block)) {
                return ActionResult.PASS;
            }
            if (world.isClient()) {
                final Direction facing = IBlock.getStatePropertySafe(state, BlockPSDAPGBase.FACING);
                /* The segment is identified by its LOWER row — clicking
                   either half of any column resolves to the same segment. */
                BlockPos base = pos;
                final BlockState lowerState = world.getBlockState(pos.down());
                if (IBlock.getStatePropertySafe(state, IBlock.HALF) == IBlock.DoubleBlockHalf.UPPER
                        && PSDRuns.isGlass(lowerState)
                        && IBlock.getStatePropertySafe(lowerState, IBlock.HALF) == IBlock.DoubleBlockHalf.LOWER
                        && IBlock.getStatePropertySafe(lowerState, BlockPSDAPGBase.FACING) == facing) {
                    base = pos.down();
                }
                final PSDRuns.Run run = PSDRuns.glassRun(world, base, facing);
                final List<BlockPos> positions = PSDRuns.glassRunPositions(world, run, facing);
                /* Yanyang/hotfix: the selection screen lives in the client
                   hierarchy (extends net.minecraft Screen), so referencing it
                   from this common, server-loaded item made the JVM verifier
                   pull net.minecraft.class_437 while DEFINING the item class
                   on a dedicated server — NoClassDefFoundError at
                   Items.<clinit> (reported on JCM 2.3.0-1.0.1+1.20.1, a pure
                   Fabric server). The opener now lives in ClientHelper, the
                   same client-isolation pattern as the GUI packets: the helper
                   is only ever loaded from inside this world.isClient()
                   branch, so a dedicated server never defines it. */
                ClientHelper.openPSDScriptSelectionScreen(positions, PSDWrapper.PSDType.GLASS);
            }
            return ActionResult.SUCCESS;
        }
        if (block instanceof BlockPSDTop) {
            /* Yanyang: a psd_top band is only scriptable above a FULL-height
               PSD wall — the band a half-height gate's top run would sit on
               (or a hand-placed floating one) carries no script. */
            if (!PSDScriptingRules.isScriptableTop(world, pos)) {
                return ActionResult.PASS;
            }
            if (world.isClient()) {
                final Direction facing = IBlock.getStatePropertySafe(state, BlockPSDTop.FACING);
                final PSDRuns.Run run = PSDRuns.topRun(world, pos, facing);
                final List<BlockPos> positions = PSDRuns.topRunPositions(run, facing);
                ClientHelper.openPSDScriptSelectionScreen(positions, PSDWrapper.PSDType.TOP);
            }
            return ActionResult.SUCCESS;
        }
        /* ABI 16: the three FangSu-anchored block-script kinds — right-click
           a FangSu level crossing / ticket machine / announcement speaker
           to bind a levelCrossingScripts / fareMachineScripts /
           announcementScripts entry (client-local, visual-only — see
           NativeBlockScriptBindings). The anchor probe is reflective, so a
           dedicated server or a world without FangSu just falls through. */
        if (world.isClient()) {
            final int anchorKind = com.lx862.jcm.mod.scripting.block.NativeBlockScriptBindings.anchorKindOf(pos);
            if (anchorKind != 0) {
                ClientHelper.openBlockScriptSelectionScreen(pos, kindName(anchorKind));
                return ActionResult.SUCCESS;
            }
        }
        return super.useOnBlock2(context);
    }

    private static String kindName(int anchorKind) {
        switch (anchorKind) {
            case 1: return com.lx862.jcm.mod.scripting.block.NativeBlockScriptBindings.KIND_LEVEL_CROSSING;
            case 2: return com.lx862.jcm.mod.scripting.block.NativeBlockScriptBindings.KIND_FARE_MACHINE;
            case 3: return com.lx862.jcm.mod.scripting.block.NativeBlockScriptBindings.KIND_ANNOUNCEMENT;
            default: return "";
        }
    }
}
