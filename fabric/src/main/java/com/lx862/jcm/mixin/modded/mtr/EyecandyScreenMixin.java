package com.lx862.jcm.mixin.modded.mtr;

import com.lx862.mtrscripting.mod.gui.screen.EyecandySelectionScreen;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.ButtonWidget;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.Screen;
import org.mtr.mod.screen.EyeCandyScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EyeCandyScreen.class, remap = false)
public abstract class EyecandyScreenMixin {
    @Shadow
    @Final
    private BlockPos blockPos;

    @Shadow
    @Final
    private LongArrayList selectedModelIndices;

    @Shadow
    protected abstract void sendUpdate();

    @Inject(method = "lambda$new$0", at = @At(value = "HEAD"), cancellable = true)
    private void openSelectionScreen(ObjectArrayList<?> objectsForList, ButtonWidget button, CallbackInfo ci) {
        MinecraftClient.getInstance().openScreen(new Screen(
                new EyecandySelectionScreen(blockPos, selectedModelIndices, this::sendUpdate)
                        .withPreviousScreen(new Screen((EyeCandyScreen)(Object)this))
        ));
        ci.cancel();
    }
}
