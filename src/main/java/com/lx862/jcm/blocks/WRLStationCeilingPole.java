package com.lx862.jcm.blocks;

import com.lx862.jcm.blocks.base.PoleBlock;
import com.lx862.jcm.blocks.data.BlockProperties;
import com.lx862.jcm.util.VoxelUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public class WRLStationCeilingPole extends PoleBlock {

    public static final IntProperty PART = BlockProperties.HORIZONTAL_PART;
    public WRLStationCeilingPole(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        switch(state.get(PART)) {
            case 0:
                return VoxelUtil.getDirectionalShape16(state.get(FACING), 5.5, 0, 7.5, 6.5, 16, 8.5);
            case 1:
                return VoxelUtil.getDirectionalShape16(state.get(FACING), 10.5, 0, 7.5, 11.5, 16, 8.5);
            default:
                return VoxelShapes.empty();
        }
    }


    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState blockBelow = ctx.getWorld().getBlockState(ctx.getBlockPos().down());
        if((blockBelow.getBlock() instanceof WRLStationCeilingPole) || blockIsAllowed(blockBelow.getBlock())) {
            return super.getPlacementState(ctx).with(PART, blockBelow.get(PART)).with(FACING, blockBelow.get(FACING));
        } else {
            return null;
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(PART);
    }

    @Override
    public boolean blockIsAllowed(Block block) {
        return block instanceof WRLStationCeilingBlock;
    }
}
