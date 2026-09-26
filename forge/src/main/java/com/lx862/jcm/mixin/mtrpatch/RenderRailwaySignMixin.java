package com.lx862.jcm.mixin.mtrpatch;

import com.lx862.jcm.mod.extra.SignExtraAccessor;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.render.RenderRailwaySign;
import org.mtr.mod.render.StoredMatrixTransformations;
import org.mtr.mod.resource.SignResource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = RenderRailwaySign.class, remap = false)
public class RenderRailwaySignMixin {
    @Unique private static int jsblock$curSignTextColor = -1;

    @Inject(method = "drawSign", at = @At(value = "INVOKE", target = "Lorg/mtr/mod/resource/SignResource;getSmall()Z"), locals = LocalCapture.CAPTURE_FAILHARD)
    private static void jsblock$captureTextColor(GraphicsHolder graphicsHolder, StoredMatrixTransformations storedMatrixTransformations, BlockPos pos, String signId, float x, float y, float size, float maxWidthLeft, float maxWidthRight, LongAVLTreeSet selectedIds, Direction facing, int backgroundColor, RenderRailwaySign.DrawTexture drawTexture, CallbackInfo ci, SignResource sign) {
        jsblock$curSignTextColor = ((SignExtraAccessor)(Object)sign).jsblock$getSignTextColor();
    }

    @ModifyArg(method = "renderCustomText", at = @At(value = "INVOKE", target = "Lorg/mtr/mod/client/DynamicTextureCache;getSignText(Ljava/lang/String;Lorg/mtr/mod/data/IGui$HorizontalAlignment;FII)Lorg/mtr/mod/client/DynamicTextureCache$DynamicResource;"), index = 4)
    private static int jsblock$transformTextColor(int backgroundColor) {
        return jsblock$curSignTextColor;
    }

}
