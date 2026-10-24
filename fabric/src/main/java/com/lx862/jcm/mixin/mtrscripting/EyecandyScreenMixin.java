package com.lx862.jcm.mixin.mtrscripting;

import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import com.lx862.mtrscripting.mod.gui.screen.EyecandyCustomConfigScreen;
import com.lx862.mtrscripting.mod.impl.mtr.eyecandy.config.JCMBlockEyecandyExtra;
import com.lx862.mtrscripting.mod.resource.EyecandyResourceProvider;
import com.lx862.mtrscripting.mod.resource.MtrScriptingResourceManager;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongArrayList;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.block.BlockEyeCandy;
import org.mtr.mod.client.CustomResourceLoader;
import org.mtr.mod.client.IDrawing;
import org.mtr.mod.resource.ObjectResource;
import org.mtr.mod.screen.EyeCandyScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(value = EyeCandyScreen.class, remap = false)
public class EyecandyScreenMixin {
    @Shadow
    @Final
    private BlockPos blockPos;

    @Shadow
    @Final
    private LongArrayList selectedModelIndices;

    @Unique
    private ButtonWidgetExtension jsblock$customConfigButton;

    @Shadow
    @Final
    private int xStart;

    @Inject(method = "<init>", at = @At(value = "TAIL"))
    private void openSelectionScreen(CallbackInfo ci) {
        this.jsblock$customConfigButton = new ButtonWidgetExtension(0, 0, 0, 20, (btn) -> {
            ObjectResource objects = CustomResourceLoader.getObjects().get((int)selectedModelIndices.getLong(0));
            BlockEntity be = MinecraftClient.getInstance().getWorldMapped().getBlockEntity(blockPos);
            if(be != null && be.data instanceof BlockEyeCandy.BlockEntity) {
                String modelId = objects.getId();
                Map<String, String> customEntries = new HashMap<>(((JCMBlockEyecandyExtra)be.data).jsblock$getCustomConfig());
                EyecandyResourceProvider.EyecandyScriptConfiguration scriptConfig = MtrScriptingResourceManager.eyecandy.getScriptEntry(modelId);
                MinecraftClient.getInstance().openScreen(
                        new Screen(
                                new EyecandyCustomConfigScreen(modelId, modelId, blockPos, customEntries, scriptConfig.eyecandyCustomConfig())
                                .withPreviousScreen(new Screen((EyeCandyScreen)(Object)this))
                        )
                );
            }
        });
        this.jsblock$customConfigButton.setMessage2(Text.cast(TextUtil.translatable(TextCategory.GUI, "eyecandy.custom_config.edit")));
    }

    @Inject(method = "init2", at = @At("TAIL"))
    private void jsblock$addCustomConfigButton(CallbackInfo ci) {
        IDrawing.setPositionAndWidth(jsblock$customConfigButton,20 + xStart + 6, 140, 160);
        jsblock$customConfigButton.setVisibleMapped(false);

        if(!selectedModelIndices.isEmpty()) {
            ObjectResource objects = CustomResourceLoader.getObjects().get((int)selectedModelIndices.getLong(0));
            if(objects != null) {
                String modelId = objects.getId();
                EyecandyResourceProvider.EyecandyScriptConfiguration scriptConfig = MtrScriptingResourceManager.eyecandy.getScriptEntry(modelId);
                boolean visible = scriptConfig != null && scriptConfig.eyecandyCustomConfig() != null;
                if(visible) {
                    jsblock$customConfigButton.setVisibleMapped(true);
                    ((EyeCandyScreen)(Object)this).addChild(new ClickableWidget(jsblock$customConfigButton));
                }
            }
        }
    }
}
