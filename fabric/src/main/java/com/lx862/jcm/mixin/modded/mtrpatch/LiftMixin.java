package com.lx862.jcm.mixin.modded.mtrpatch;

import com.lx862.jcm.mod.extra.JCMPatchForMTR;
import org.mtr.core.data.Data;
import org.mtr.core.data.Lift;
import org.mtr.core.data.LiftFloor;
import org.mtr.core.data.TransportMode;
import org.mtr.core.generated.data.LiftSchema;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.SoundHelper;
import org.mtr.mod.Init;
import org.mtr.mod.block.BlockLiftTrackFloor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Lift.class, remap = false)
public abstract class LiftMixin extends LiftSchema {
    @Shadow
    @Final
    private boolean isClientside;

    @Shadow
    public abstract LiftFloor getCurrentFloor();

    protected LiftMixin(TransportMode transportMode, Data data) {
        super(transportMode, data);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void jsblock$playArrivalDingSound(long millisElapsed, CallbackInfo ci) {
        if(isClientside && JCMPatchForMTR.liftJustArrived(id, instructions.size())) {
            LiftFloor currentFloor = getCurrentFloor();
            BlockPos pos = Init.positionToBlockPos(currentFloor.getPosition());
            ClientWorld mcWorld = MinecraftClient.getInstance().getWorldMapped();
            ClientPlayerEntity mcPlayer = MinecraftClient.getInstance().getPlayerMapped();

            if(mcWorld != null && mcPlayer != null) {
                BlockEntity be = mcWorld.getBlockEntity(pos);
                if(be != null && be.data instanceof BlockLiftTrackFloor.BlockEntity) {
                    BlockLiftTrackFloor.BlockEntity floorBE = (BlockLiftTrackFloor.BlockEntity)be.data;
                    if(floorBE.getShouldDing()) {
                        mcWorld.playSound(PlayerEntity.cast(mcPlayer), pos, SoundHelper.createSoundEvent(JCMPatchForMTR.LIFT_DING_SOUND), SoundCategory.BLOCKS, 1, 2);
                    }
                }
            }
        }
    }
}
