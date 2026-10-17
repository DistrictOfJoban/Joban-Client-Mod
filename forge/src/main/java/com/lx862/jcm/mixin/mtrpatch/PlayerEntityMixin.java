package com.lx862.jcm.mixin.mtrpatch;

import com.lx862.jcm.mod.extra.JCMPatchForMTR;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** For re-implementing shift-to-sit feature in MTR 3 */
@Mixin(Player.class)
public class PlayerEntityMixin {
    @Inject(method = "getStandingEyeHeight", at = @At("HEAD"), cancellable = true)
    private void jsblock$sitPlayerOnVehicle(Pose p_36259_, net.minecraft.world.entity.EntityDimensions p_36260_, CallbackInfoReturnable<Float> cir) {
        if(JCMPatchForMTR.isSitting) {
            cir.setReturnValue(1.27F);
        }
    }
}
