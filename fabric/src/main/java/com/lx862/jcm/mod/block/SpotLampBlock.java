package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.SlabExtendableBlock;
import com.lx862.jcm.mod.block.base.VerticallyAttachedBlock;
import com.lx862.jcm.mod.data.BlockProperties;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.block.IBlock;

import java.util.List;

public class SpotLampBlock extends VerticallyAttachedBlock {
    public static final BooleanProperty LIT = BlockProperties.POWERED;
    public static final EnumProperty<BlockProperties.InvertBoolean> IS_SLAB = BlockProperties.IS_SLAB_MIGRATE;

    public SpotLampBlock(BlockSettings settings) {
        super(settings);
    }

    @Override
    public BlockState getPlacementState2(ItemPlacementContext ctx) {
        BlockState superState = super.getPlacementState2(ctx);
        if(superState == null) return null;
        boolean isTop = IBlock.getStatePropertySafe(superState, BlockProperties.TOP);
        BlockPos targetBlockPos = isTop ? ctx.getBlockPos().up() : ctx.getBlockPos().down();
        boolean hasSlab = SlabExtendableBlock.shouldExtendForSlab(WorldAccess.cast(ctx.getWorld()), targetBlockPos, isTop ? SlabType.TOP : SlabType.BOTTOM);

        return superState.with(new Property<>(IS_SLAB.data), hasSlab ? BlockProperties.InvertBoolean.TRUE : BlockProperties.InvertBoolean.FALSE);
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        if (IBlock.getStatePropertySafe(state, TOP)) {
            return IBlock.getVoxelShapeByDirection(4, 15.75, 4, 12, 16, 12, Direction.NORTH);
        } else {
            return IBlock.getVoxelShapeByDirection(4, 0, 4, 12, 0.25, 12, Direction.NORTH);
        }
    }

    @Override
    public VoxelShape getCollisionShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        return IBlock.checkHoldingBrush(world, player, () -> {
            BlockState newState = state.cycle(new Property<>(LIT.data));
            world.setBlockState(pos, newState);
        });
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(LIT);
        properties.add(IS_SLAB);
    }
}
