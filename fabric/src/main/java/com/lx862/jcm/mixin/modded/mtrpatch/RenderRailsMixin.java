package com.lx862.jcm.mixin.modded.mtrpatch;

import com.lx862.jcm.mod.config.JCMClientConfig;
import com.lx862.jcm.mod.extra.JCMPatchForMTR;
import org.mtr.core.data.Rail;
import org.mtr.core.data.RailMath;
import org.mtr.libraries.com.logisticscraft.occlusionculling.OcclusionCullingInstance;
import org.mtr.libraries.com.logisticscraft.occlusionculling.util.Vec3d;
import org.mtr.libraries.it.unimi.dsi.fastutil.doubles.DoubleDoubleImmutablePair;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArraySet;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.MinecraftClientHelper;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.render.RenderRails;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = RenderRails.class, remap = false)
public class RenderRailsMixin {
    @Unique
    private static RailMath jsblock$capturedRailMath;

    @Shadow
    private static int renderRailStat(GraphicsHolder graphicsHolder, String title, String data, int line) {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    @Inject(method = "lambda$render$5", at = @At("HEAD"), cancellable = true)
    private static void jsblock$cancelRailRendering(boolean holdingRailRelated, ObjectArraySet hoverRails, ClientWorld clientWorld, Rail rail, CallbackInfo ci) {
        if(JCMClientConfig.INSTANCE.mtrPatch.disableRailRendering.value()) ci.cancel();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lorg/mtr/mod/render/RenderRails;renderRailStats(Lorg/mtr/mapping/holder/BlockPos;Lorg/mtr/mapping/holder/BlockPos;DDD)V", ordinal = 0), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    private static void jsblock$captureRail1(CallbackInfo ci, MinecraftClient minecraftClient, ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, ObjectArrayList cullingTasks, Vector3d cameraPosition, Vec3d camera, boolean holdingRailRelated, ObjectArrayList railsToRender, ObjectArraySet hoverRails, ObjectObjectImmutablePair railAndBlockPos, Rail rail, BlockPos blockPos, Rail newRail, DoubleDoubleImmutablePair railRadii) {
        jsblock$capturedRailMath = newRail.railMath;
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lorg/mtr/mod/render/RenderRails;renderRailStats(Lorg/mtr/mapping/holder/BlockPos;Lorg/mtr/mapping/holder/BlockPos;DDD)V", ordinal = 1), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    private static void jsblock$captureRail2(CallbackInfo ci, MinecraftClient minecraftClient, ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, ObjectArrayList cullingTasks, Vector3d cameraPosition, Vec3d camera, boolean holdingRailRelated, ObjectArrayList railsToRender, ObjectArraySet hoverRails, ObjectObjectImmutablePair railAndBlockPos, Rail rail, BlockPos blockPos, DoubleDoubleImmutablePair railRadii) {
        jsblock$capturedRailMath = rail.railMath;
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lorg/mtr/mod/render/RenderRails;renderRailStats(Lorg/mtr/mapping/holder/BlockPos;Lorg/mtr/mapping/holder/BlockPos;DDD)V", ordinal = 2), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    private static void jsblock$captureRail3(CallbackInfo ci, MinecraftClient minecraftClient, ClientWorld clientWorld, ClientPlayerEntity clientPlayerEntity, ObjectArrayList cullingTasks, Vector3d cameraPosition, Vec3d camera, boolean holdingRailRelated, ObjectArrayList railsToRender, ObjectArraySet hoverRails, ItemStack itemStack, Item item, HitResult hitResult, Vector3d hitPos, BlockPos posStart, CompoundTag compoundTag, BlockPos posEnd, BlockState blockStateEnd, BlockState blockStateStart, float angleEnd, ObjectObjectImmutablePair angles, Rail rail, Rail newRail, double railLength, DoubleDoubleImmutablePair railRadii) {
        jsblock$capturedRailMath = rail.railMath;
    }

    @Inject(method="lambda$renderRailStats$24", at = @At(value = "INVOKE", target = "Lorg/mtr/mapping/mapper/GraphicsHolder;pop()V"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private static void jsblock$showMidGradient(BlockPos renderPos, double textOffset, BlockPos otherPos, String textXYZOffsetLabel, String textXYZOffset, String textXZRadius, String textXZRadiusLabel, String textLengthLabel, String textLength, GraphicsHolder graphicsHolder, Vector3d offset, CallbackInfo ci, int line) {
        if(jsblock$capturedRailMath != null) {
            double railLength = jsblock$capturedRailMath.getLength();
            double p1 = ((RailMathAccessor)jsblock$capturedRailMath).invokeGetPositionY((railLength / 2) - 0.5);
            double p2 = ((RailMathAccessor)jsblock$capturedRailMath).invokeGetPositionY((railLength / 2) + 0.5);
            double midGradient =  Math.abs(p2 - p1) * 1000;
            graphicsHolder.translate(0, -0.5 / 0.03125F, 0);
            renderRailStat(graphicsHolder, "Gradient", String.format("%.1f‰", midGradient), line);
        }
    }

    @Inject(method = "lambda$render$1", at = @At("HEAD"), cancellable = true)
    private static void jsblock$improveRailCulling(MinecraftClientData.RailWrapper railWrapper, Vec3d camera, OcclusionCullingInstance occlusionCullingInstance, CallbackInfoReturnable<Runnable> cir) {
        boolean overrideDefaultBehaviour = JCMClientConfig.INSTANCE.mtrPatch.railCullingImprovement.value();
        if(!overrideDefaultBehaviour) return;

        Box railCullingBoundary = JCMPatchForMTR.clampBoundingBoxToRenderDistance(
                new Vector3d(camera.getX(), camera.getY(), camera.getZ()),
                MinecraftClientHelper.getRenderDistance(),
                railWrapper.startVector.x, railWrapper.startVector.y, railWrapper.startVector.z,
                railWrapper.endVector.x, railWrapper.endVector.y, railWrapper.endVector.z
        );

        boolean shouldRender = JCMPatchForMTR.shouldSkipCullingTask(
                railCullingBoundary.getMinXMapped(), railCullingBoundary.getMinYMapped(), railCullingBoundary.getMinZMapped(),
                railCullingBoundary.getMaxXMapped(), railCullingBoundary.getMaxYMapped(), railCullingBoundary.getMaxZMapped()
        ) || occlusionCullingInstance.isAABBVisible(
                new Vec3d(railCullingBoundary.getMinXMapped(), railCullingBoundary.getMinYMapped(), railCullingBoundary.getMinZMapped()),
                new Vec3d(railCullingBoundary.getMaxXMapped(), railCullingBoundary.getMaxYMapped(), railCullingBoundary.getMaxZMapped()),
                camera
        );

        cir.setReturnValue(() -> railWrapper.shouldRender = shouldRender);
    }
}
