package com.lx862.jcm.mixin.modded.mtr;

import com.lx862.mtrscripting.core.util.ante.StringMapSerializer;
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

        // Legacy ANTE custom config parsing
        try {
            if (compoundTag.contains("customConfigs")) {
                byte[] dataBytes = compoundTag.getByteArray("customConfigs");
                StringMapSerializer.deserialize(jsblock$eyecandyCustomConfig, dataBytes);
            }
        } catch (IOException e) {
        }
    }

    @Inject(method = "readCompoundTag", at = @At("HEAD"))
    private void jsblock$writeCustomConfigTag(CompoundTag compoundTag, CallbackInfo ci) {
        // Legacy ANTE custom config writing
        try {
            byte[] configBytes = StringMapSerializer.serializeToByteArray(jsblock$eyecandyCustomConfig);
            compoundTag.putByteArray("customConfigs", configBytes);
        } catch (IOException e) {
        }
    }

    @Override
    public Map<String, String> jsblock$getCustomConfigs() {
        return jsblock$eyecandyCustomConfig;
    }
}
