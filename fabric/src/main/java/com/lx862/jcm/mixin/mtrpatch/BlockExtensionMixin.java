package com.lx862.jcm.mixin.mtrpatch;

import com.lx862.jcm.mod.block.base.SlabExtendableBlock;
import com.lx862.jcm.mod.data.BlockProperties;
import org.mtr.mapping.holder.*;
import org.mtr.mod.block.BlockRailwaySignPole;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockAbstractMapping.class, remap = false)
public class BlockExtensionMixin {
    @Inject(method = "getStateForNeighborUpdate2", at = @At("HEAD"), remap = false, cancellable = true)
    private void jsblock$updatePoles(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos, CallbackInfoReturnable<BlockState> cir) {
        if((Object)this instanceof BlockRailwaySignPole) {
            if(direction == Direction.UP) {
                if(SlabExtendableBlock.shouldExtendForSlab(world, neighborPos, SlabType.TOP)) {
                    cir.setReturnValue(
                            state.with(new Property<>(BlockProperties.IS_SLAB_MIGRATE.data), BlockProperties.InvertBoolean.TRUE)
                    );
                } else {
                    cir.setReturnValue(
                        state.with(new Property<>(BlockProperties.IS_SLAB_MIGRATE.data), BlockProperties.InvertBoolean.FALSE)
                    );
                }
            }
        }
    }
}
