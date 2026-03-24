package com.lx862.jcm.mixin.modded.mtrpatch;

import com.lx862.jcm.mod.extra.JCMPatchForMTR;
import org.mtr.core.serializer.ReaderBase;
import org.mtr.mod.generated.resource.ModelPropertiesSchema;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ModelPropertiesSchema.class, remap = false)
public class ModelPropertiesSchemaMixin {

    @Mutable
    @Shadow
    @Final
    protected String gangwayInnerSideResource;

    @Mutable
    @Shadow
    @Final
    protected String gangwayInnerTopResource;

    @Mutable
    @Shadow
    @Final
    protected String gangwayInnerBottomResource;

    @Mutable
    @Shadow
    @Final
    protected String gangwayOuterSideResource;

    @Mutable
    @Shadow
    @Final
    protected String gangwayOuterTopResource;

    @Mutable
    @Shadow
    @Final
    protected String gangwayOuterBottomResource;

    @Mutable
    @Shadow
    @Final
    protected String barrierInnerSideResource;

    @Mutable
    @Shadow
    @Final
    protected String barrierOuterSideResource;

    @Inject(method = "updateData", at = @At(value = "TAIL"))
    private void jsblock$fixLegacyTextureReference(ReaderBase readerBase, CallbackInfo ci) {
        if(gangwayInnerSideResource.startsWith("mtr:textures/entity")) {
            for(String s : JCMPatchForMTR.mtr3GangwayTextures) {
                if(this.gangwayInnerSideResource.startsWith("mtr:textures/entity/" + s)) {
                    gangwayInnerSideResource = gangwayInnerSideResource.replace("/entity", "/gangway").replace("connector_", "");
                    gangwayInnerTopResource = gangwayInnerTopResource.replace("/entity", "/gangway").replace("connector_", "");
                    gangwayInnerBottomResource = gangwayInnerBottomResource.replace("/entity", "/gangway").replace("connector_", "");
                    gangwayOuterSideResource = gangwayOuterSideResource.replace("/entity", "/gangway").replace("connector_", "");
                    gangwayOuterTopResource = gangwayOuterTopResource.replace("/entity", "/gangway").replace("connector_", "");
                    gangwayOuterBottomResource = gangwayOuterBottomResource.replace("/entity", "/gangway").replace("connector_", "");
                    break;
                }
            }
        }

        if(barrierInnerSideResource.startsWith("mtr:textures/entity")) {
            for(String s : JCMPatchForMTR.mtr3BarrierTextures) {
                if(this.barrierInnerSideResource.startsWith("mtr:textures/entity/" + s)) {
                    barrierInnerSideResource = barrierInnerSideResource.replace("/entity", "/barrier").replace("barrier_", "");
                    barrierOuterSideResource = barrierOuterSideResource.replace("/entity", "/barrier").replace("barrier_", "");
                    break;
                }
            }
        }
    }
}
