package com.lx862.jcm.mixin.mtrpatch;

import com.lx862.jcm.mod.extra.JCMPatchForMTR;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** For re-implementing shift-to-sit feature in MTR 3 */
@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Inject(method = "getActiveEyeHeight", at = @At("HEAD"), cancellable = true)
    private void jsblock$sitPlayerOnVehicle(EntityPose pose, EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
        /* Local player only: isSitting is the LOCAL player's state, but this
           inject applies to every PlayerEntity — remote players' eye heights
           (hitbox, name tag position) dropped whenever the local player sat. */
        if(JCMPatchForMTR.isSitting && (Object)this == MinecraftClient.getInstance().player) {
            cir.setReturnValue(1.27F);
        }
    }
}
