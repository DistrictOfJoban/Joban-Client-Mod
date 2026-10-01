package com.lx862.jcm.mixin.mtrpatch;

import com.lx862.jcm.mod.block.base.SlabExtendableBlock;
import com.lx862.jcm.mod.data.BlockProperties;
import org.mtr.mapping.holder.*;
import org.mtr.mod.block.BlockPoleCheckBase;
import org.mtr.mod.block.BlockRailwaySignPole;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockPoleCheckBase.class, remap = false)
public class BlockPoleCheckBaseMixin {

    @Inject(method = "getPlacementState2", at = @At("RETURN"), cancellable = true)
    public void getPlacementState2(ItemPlacementContext ctx, CallbackInfoReturnable<BlockState> cir) {

        if((Object)this instanceof BlockRailwaySignPole) {
            BlockState state = cir.getReturnValue();
            if(state != null) {
                BlockPos slabCheckPos = ctx.getBlockPos().up();
                if(SlabExtendableBlock.shouldExtendForSlab(WorldAccess.cast(ctx.getWorld()), slabCheckPos, SlabType.TOP)) {
                    cir.setReturnValue(
                            state
                            .with(new Property<>(BlockProperties.IS_SLAB_MIGRATE.data), BlockProperties.InvertBoolean.TRUE)
                    );
                }
            }
        }
    }
}
