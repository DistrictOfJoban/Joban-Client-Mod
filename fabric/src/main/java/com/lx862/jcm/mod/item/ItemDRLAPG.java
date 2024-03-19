package com.lx862.jcm.mod.item;

import com.lx862.jcm.mod.registry.Blocks;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ItemExtension;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.block.BlockPSDAPGBase;
import org.mtr.mod.block.BlockPSDTop;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.block.TripleHorizontalBlock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Copied from MTR to replace hardcoded block/item reference to our DRL APG
 */
public class ItemDRLAPG extends ItemExtension implements IBlock {
    private final EnumPSDAPGItem item;
    private final EnumPSDAPGType type;

    public ItemDRLAPG(EnumPSDAPGItem item, EnumPSDAPGType type, ItemSettings itemSettings) {
        super(itemSettings);
        this.item = item;
        this.type = type;
    }

    @Nonnull
    @Override
    public ActionResult useOnBlock2(ItemUsageContext context) {
        final int horizontalBlocks = item.isDoor ? type.isOdd ? 3 : 2 : 1;
        if (blocksNotReplaceable(context, horizontalBlocks, type.isPSD ? 3 : 2, getBlockStateFromItem().getBlock())) {
            return ActionResult.FAIL;
        }

        final World world = context.getWorld();
        final Direction playerFacing = context.getPlayerFacing();
        final BlockPos pos = context.getBlockPos().offset(context.getSide());

        for (int x = 0; x < horizontalBlocks; x++) {
            final BlockPos newPos = pos.offset(playerFacing.rotateYClockwise(), x);

            for (int y = 0; y < 2; y++) {
                final BlockState state = getBlockStateFromItem().with(new Property<>(BlockPSDAPGBase.FACING.data), playerFacing.data).with(new Property<>(HALF.data), y == 1 ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER);
                if (item.isDoor) {
                    BlockState neighborState = state.with(new Property<>(SIDE.data), x == 0 ? EnumSide.LEFT : EnumSide.RIGHT);
                    if (type.isOdd) {
                        neighborState = neighborState.with(new Property<>(TripleHorizontalBlock.CENTER.data), x > 0 && x < horizontalBlocks - 1);
                    }
                    world.setBlockState(newPos.up(y), neighborState);
                } else {
                    world.setBlockState(newPos.up(y), state.with(new Property<>(SIDE_EXTENDED.data), EnumSide.SINGLE));
                }
            }

            if (type.isPSD) {
                world.setBlockState(newPos.up(2), BlockPSDTop.getActualState(WorldAccess.cast(world), newPos.up(2)));
            }
        }

        context.getStack().decrement(1);
        return ActionResult.SUCCESS;
    }

    @Override
    public void addTooltips(ItemStack stack, @Nullable World world, List<MutableText> tooltip, TooltipContext options) {
        tooltip.add(TextHelper.translatable(type.isLift ? type.isOdd ? "tooltip.mtr.railway_sign_odd" : "tooltip.mtr.railway_sign_even" : "tooltip.mtr." + item.asString2()).formatted(TextFormatting.GRAY));
    }

    private BlockState getBlockStateFromItem() {
        switch (item) {
            case DOOR:
                return Blocks.APG_DOOR_DRL.get().getDefaultState();
            case GLASS:
                return Blocks.APG_GLASS_DRL.get().getDefaultState();
            case GLASS_END:
                return Blocks.APG_GLASS_END_DRL.get().getDefaultState();
            default:
                return org.mtr.mapping.holder.Blocks.getAirMapped().getDefaultState();
        }
    }

    public static boolean blocksNotReplaceable(ItemUsageContext context, int width, int height, @Nullable Block blacklistBlock) {
        final Direction facing = context.getPlayerFacing();
        final World world = context.getWorld();
        final BlockPos startingPos = context.getBlockPos().offset(context.getSide());

        for (int x = 0; x < width; x++) {
            final BlockPos offsetPos = startingPos.offset(facing.rotateYClockwise(), x);

            if (blacklistBlock != null) {
                final boolean isBlacklistedBelow = world.getBlockState(offsetPos.down()).isOf(blacklistBlock);
                final boolean isBlacklistedAbove = world.getBlockState(offsetPos.up(height)).isOf(blacklistBlock);
                if (isBlacklistedBelow || isBlacklistedAbove) {
                    return true;
                }
            }

            for (int y = 0; y < height; y++) {
                if (!world.getBlockState(offsetPos.up(y)).getBlock().equals(org.mtr.mapping.holder.Blocks.getAirMapped())) {
                    return true;
                }
            }
        }

        return false;
    }

    public enum EnumPSDAPGType {
        APG(false, false, false);

        private final boolean isPSD;
        private final boolean isOdd;
        private final boolean isLift;

        EnumPSDAPGType(boolean isPSD, boolean isOdd, boolean isLift) {
            this.isPSD = isPSD;
            this.isOdd = isOdd;
            this.isLift = isLift;
        }
    }

    public enum EnumPSDAPGItem implements StringIdentifiable {

        DOOR("psd_apg_door", true),
        GLASS("psd_apg_glass", false),
        GLASS_END("psd_apg_glass_end", false);

        private final String name;
        private final boolean isDoor;

        EnumPSDAPGItem(String name, boolean isDoor) {
            this.name = name;
            this.isDoor = isDoor;
        }

        @Nonnull
        @Override
        public String asString2() {
            return name;
        }
    }
}
