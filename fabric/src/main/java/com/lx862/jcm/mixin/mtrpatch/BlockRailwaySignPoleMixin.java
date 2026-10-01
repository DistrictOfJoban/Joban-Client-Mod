package com.lx862.jcm.mixin.mtrpatch;

import com.lx862.jcm.mod.data.BlockProperties;
import org.mtr.mapping.tool.HolderBase;
import org.mtr.mod.block.BlockRailwaySignPole;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = BlockRailwaySignPole.class, remap = false)
public class BlockRailwaySignPoleMixin {
    @Inject(method = "addBlockProperties", at = @At("TAIL"))
    private void jsblock$addIsSlabProperties(List<HolderBase<?>> properties, CallbackInfo ci) {
        properties.add(BlockProperties.IS_SLAB_MIGRATE);
    }
}
