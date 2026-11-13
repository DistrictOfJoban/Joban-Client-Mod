package com.lx862.jcm.mod.item;

import com.lx862.jcm.mod.gui.screen.PSDScriptSelectionScreen;
import com.lx862.jcm.mod.scripting.psd.PSDRuns;
import com.lx862.jcm.mod.scripting.psd.PSDWrapper;
import org.mtr.mapping.holder.ActionResult;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.ItemSettings;
import org.mtr.mapping.holder.ItemUsageContext;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.Screen;
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
                MinecraftClient.getInstance().openScreen(new Screen(new PSDScriptSelectionScreen(positions, PSDWrapper.PSDType.GLASS)));
            }
            return ActionResult.SUCCESS;
        }
        if (block instanceof BlockPSDTop) {
            if (world.isClient()) {
                final Direction facing = IBlock.getStatePropertySafe(state, BlockPSDTop.FACING);
                final PSDRuns.Run run = PSDRuns.topRun(world, pos, facing);
                final List<BlockPos> positions = PSDRuns.topRunPositions(run, facing);
                MinecraftClient.getInstance().openScreen(new Screen(new PSDScriptSelectionScreen(positions, PSDWrapper.PSDType.TOP)));
            }
            return ActionResult.SUCCESS;
        }
        return super.useOnBlock2(context);
    }
}
