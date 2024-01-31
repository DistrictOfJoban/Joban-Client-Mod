package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.VerticallyAttachedDirectionalBlock;
import com.lx862.jcm.mod.block.entity.KCRStationNameSignBlockEntity;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.util.*;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.BlockEntityExtension;
import org.mtr.mapping.mapper.BlockWithEntity;
import org.mtr.mapping.tool.HolderBase;

import java.util.List;

public class KCRStationNameSignBlock extends VerticallyAttachedDirectionalBlock implements BlockWithEntity {
    public static final BooleanProperty EXIT_ON_LEFT = BlockProperties.EXIT_ON_LEFT;
    private final boolean stationColored;
    public KCRStationNameSignBlock(BlockSettings settings, boolean stationColored) {
        super(settings, true, false);
        this.stationColored = stationColored;
    }

    @Override
    public VoxelShape getOutlineShape2(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return VoxelUtil.getDirectionalShape16(BlockUtil.getProperty(state, FACING), -7, 2, 5.5, 23, 16, 10.5);
    }

    @Override
    public BlockEntityExtension createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new KCRStationNameSignBlockEntity(blockPos, blockState, stationColored);
    }

    @Override
    public ActionResult onUse2(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        super.onUse2(state, world, pos, player, hand, hit);
        return getBrushActionResult(player);
    }

    @Override
    public void onServerUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if(Utils.playerHoldingBrush(player)) {
            world.setBlockState(pos, state.cycle(new Property<>(EXIT_ON_LEFT.data)));
            player.sendMessage(Text.cast(TextUtil.translatable(TextCategory.HUD, "kcr_name_sign.success")), true);
        }
    }

    @Override
    public void addBlockProperties(List<HolderBase<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(EXIT_ON_LEFT);
    }
}
