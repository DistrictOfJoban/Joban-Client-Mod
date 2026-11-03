package com.lx862.jcm.mod.item;

import com.lx862.jcm.mod.gui.screen.PSDScriptSelectionScreen;
import com.lx862.jcm.mod.scripting.psd.PSDWrapper;
import org.mtr.mapping.holder.ActionResult;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.ItemSettings;
import org.mtr.mapping.holder.ItemUsageContext;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.Screen;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.ItemExtension;
import org.mtr.mod.block.BlockPSDAPGGlassBase;
import org.mtr.mod.block.BlockPSDTop;
import org.mtr.mod.block.IBlock;

import javax.annotation.Nullable;

/**
 * The "PSD Script Editor" (屏蔽门脚本编辑器) — right-click any platform screen
 * door glass (or glass end, PSD or APG, from MTR or any addon building on
 * MTR's PSD classes) to bind a {@code psdScripts} entry of type "glass" or a
 * scripted PIDS preset onto it, and any psd_top block to bind a type "top"
 * entry or PIDS preset. Lives in the same creative tab as MTR's brush.
 *
 * <p>Glass columns are two block halves — right-clicking either half opens
 * the selection screen for the whole column and a choice binds BOTH halves
 * (each half renders its own canvas; the editor resolves and passes the
 * partner position along).</p>
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
                MinecraftClient.getInstance().openScreen(new Screen(new PSDScriptSelectionScreen(pos, findPartnerHalf(world, pos, state), PSDWrapper.PSDType.GLASS)));
            }
            return ActionResult.SUCCESS;
        }
        if (block instanceof BlockPSDTop) {
            if (world.isClient()) {
                MinecraftClient.getInstance().openScreen(new Screen(new PSDScriptSelectionScreen(pos, null, PSDWrapper.PSDType.TOP)));
            }
            return ActionResult.SUCCESS;
        }
        return super.useOnBlock2(context);
    }

    /** The other half of a double glass block, or null when there is none. */
    @Nullable
    private static BlockPos findPartnerHalf(World world, BlockPos pos, BlockState state) {
        final boolean upper = IBlock.getStatePropertySafe(state, IBlock.HALF) == IBlock.DoubleBlockHalf.UPPER;
        final BlockPos partner = upper ? pos.down() : pos.up();
        return world.getBlockState(partner).getBlock().data instanceof BlockPSDAPGGlassBase ? partner : null;
    }
}
