package com.lx862.jcm.mixin.modded.mtr;

import com.lx862.mtrscripting.core.util.ante.StringMapSerializer;
import com.lx862.mtrscripting.mod.impl.mtr.eyecandy.config.JCMBlockEyecandyExtra;
import org.mtr.mapping.holder.CompoundTag;
import org.mtr.mod.block.BlockEyeCandy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Mixin(value = BlockEyeCandy.BlockEntity.class, remap = false)
public class BlockEntityEyecandyMixin implements JCMBlockEyecandyExtra {
    @Unique
    private final Map<String, String> jsblock$eyecandyCustomConfig = new HashMap<>();

    @Inject(method = "readCompoundTag", at = @At("HEAD"))
    private void jsblock$readCustomConfigTag(CompoundTag compoundTag, CallbackInfo ci) {
        jsblock$eyecandyCustomConfig.clear();

        try {
            if (compoundTag.contains("customConfigs")) {
                byte[] dataBytes = compoundTag.getByteArray("customConfigs");
                jsblock$eyecandyCustomConfig.putAll(StringMapSerializer.deserialize(dataBytes));
            }
        } catch (IOException e) {
        }
    }

    @Inject(method = "readCompoundTag", at = @At("HEAD"))
    private void jsblock$writeCustomConfigTag(CompoundTag compoundTag, CallbackInfo ci) {
        try {
            byte[] configBytes = StringMapSerializer.serializeToByteArray(jsblock$eyecandyCustomConfig);
            compoundTag.putByteArray("customConfigs", configBytes);
        } catch (IOException e) {
        }
    }

    @Override
    public Map<String, String> jsblock$getCustomConfig() {
        return jsblock$eyecandyCustomConfig;
    }

    @Override
    public void jsblock$updateCustomConfig(Map<String, String> newConfig) {
        jsblock$eyecandyCustomConfig.putAll(newConfig);
        ((BlockEyeCandy.BlockEntity)(Object)this).markDirty2();
    }
}
