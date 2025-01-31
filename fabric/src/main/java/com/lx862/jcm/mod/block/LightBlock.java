package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.JCMBlock;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.block.IBlock;

import java.util.List;

public class LightBlock extends JCMBlock {
    public static final IntegerProperty LIGHT_LEVEL = BlockProperties.LIGHT_LEVEL;

    public LightBlock(BlockSettings settings) {
        super(settings);
        setDefaultState2(getDefaultState2().with(new Property<>(LIGHT_LEVEL.data), 15));
    }

    @Override
    public void randomDisplayTick2(BlockState state, World world, BlockPos pos, Random random) {
        ClientPlayerEntity player = MinecraftClient.getInstance().getPlayerMapped();
        if(player != null && player.isHolding(this.asItem2())) {
            //TODO: world.addParticle, no particle in mappings yet
        }
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return VoxelShapes.fullCube();
    }

    @Override
    public VoxelShape getCollisionShape2(BlockState state, BlockView world, BlockPos pos, ShapeContext shapeContext) {
        return VoxelShapes.empty();
    }

    @Override
    public boolean isTranslucent2(BlockState state, BlockView world, BlockPos pos) {
        return true;
    }

    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if(world.isClient()) return ActionResult.SUCCESS;

        if(player.isHolding(this.asItem2())) {
            BlockState newState = state.cycle(new Property<>(LIGHT_LEVEL.data));
            world.setBlockState(pos, newState);
            player.sendMessage(Text.cast(TextUtil.translatable(TextCategory.HUD, "light_block.success", IBlock.getStatePropertySafe(newState, new Property<>(LIGHT_LEVEL.data)))), true);
            return ActionResult.SUCCESS;
        } else {
            return ActionResult.FAIL;
        }
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(LIGHT_LEVEL);
    }
}
