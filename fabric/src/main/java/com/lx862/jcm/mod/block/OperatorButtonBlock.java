package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.WallAttachedBlock;
import com.lx862.jcm.mod.block.entity.OperatorButtonBlockEntity;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.network.gui.OperatorButtonGUIPacket;
import com.lx862.jcm.mod.registry.Networking;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockWithEntity;
import org.mtr.mapping.registry.ItemRegistryObject;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.Items;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.item.ItemDepotDriverKey;
import org.mtr.mod.item.ItemDriverKey;

import java.util.List;

public class OperatorButtonBlock extends WallAttachedBlock implements BlockWithEntity {
    public static final ItemRegistryObject[] ACCEPTED_KEYS = {org.mtr.mod.Items.BASIC_DRIVER_KEY, org.mtr.mod.Items.GUARD_KEY, org.mtr.mod.Items.ADVANCED_DRIVER_KEY, Items.CREATIVE_DRIVER_KEY};
    public static final BooleanProperty POWERED = BlockProperties.POWERED;
    private final int poweredDuration;

    public OperatorButtonBlock(BlockSettings settings, int poweredDuration) {
        super(settings);
        setDefaultState2(getDefaultState2().with(new Property<>(POWERED.data), false));
        this.poweredDuration = poweredDuration;
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return IBlock.getVoxelShapeByDirection(5, 5, 0, 11, 11.5, 0.2, IBlock.getStatePropertySafe(state, FACING));
    }

    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if(world.isClient()) return ActionResult.SUCCESS;
        BlockEntity thisBE = world.getBlockEntity(pos);
        OperatorButtonBlockEntity be = thisBE == null ? null : (OperatorButtonBlockEntity)thisBE.data;

        IBlock.checkHoldingBrush(world, player, () -> {
            Networking.sendPacketToClient(player, new OperatorButtonGUIPacket(pos, be.getKeyRequirements()));
        }, () -> {
            boolean pass = false;
            ItemStack[] itemStacks = new ItemStack[]{player.getMainHandStack(), player.getOffHandStack()};
            for(ItemStack stack : itemStacks) {
                if(be == null) {
                    if(stack.getItem().data instanceof ItemDriverKey) {
                        pass = true;
                    }
                } else {
                    if(be.canOpen(stack)) {
                        pass = true;
                    }
                }
                // Expiry check
                if(stack.getItem().data instanceof ItemDepotDriverKey && stack.getOrCreateTag().getLong("expiry_time") < System.currentTimeMillis()) {
                    pass = false;
                }
                if(pass) break;
            }

            if(pass) {
                setPowered(world, state, pos, true);
                scheduleBlockTick(world, pos, new Block(this), poweredDuration);
            } else {
                player.sendMessage(Text.cast(TextUtil.translatable(TextCategory.HUD, "operator_button.fail").formatted(TextFormatting.RED)), true);
            }
        });

        return ActionResult.SUCCESS;
    }

    @Override
    public void scheduledTick2(BlockState state, ServerWorld serverWorld, BlockPos pos, Random random) {
        if (state.get(new Property<>(POWERED.data))) {
            setPowered(World.cast(serverWorld), state, pos, false);
        }
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(POWERED);
    }


    @Override
    public boolean emitsRedstonePower2(BlockState state) {
        return true;
    }

    @Override
    public int getWeakRedstonePower2(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return IBlock.getStatePropertySafe(state, POWERED) ? 15 : 0;
    }

    @Override
    public int getStrongRedstonePower2(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return IBlock.getStatePropertySafe(state, POWERED) ? 15 : 0;
    }

    @Override
    public BlockEntityExtension createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new OperatorButtonBlockEntity(blockPos, blockState);
    }

    private void setPowered(World world, BlockState blockState, BlockPos pos, boolean powered) {
        world.setBlockState(pos, blockState.with(new Property<>(POWERED.data), powered));
        updateNearbyBlock(world, pos, Direction.convert(blockState.get(new Property<>(FACING.data))));
    }

    private void updateNearbyBlock(World world, BlockPos pos, Direction blockFacing) {
        world.updateNeighbors(pos.offset(blockFacing), new Block(this));
    }
}
