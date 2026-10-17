package com.lx862.jcm.mixin.mtrpatch;

import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongCollection;
import org.mtr.mapping.holder.Screen;
import org.mtr.mod.screen.DashboardListItem;
import org.mtr.mod.screen.DashboardListSelectorScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/* Hold shift to exclusively select one item, in a multi-select list. */
@Mixin(value = DashboardListSelectorScreen.class, remap = false)
public class DashboardListSelectorScreenMixin {
    @Shadow
    @Final
    protected LongCollection selectedIds;

    @Inject(method = "onAdd", at = @At("HEAD"))
    private void jsblock$handleExclusiveSelect(DashboardListItem dashboardListItem, int index, CallbackInfo ci) {
        if(Screen.hasShiftDown()) {
            selectedIds.clear();
        }
    }
}
