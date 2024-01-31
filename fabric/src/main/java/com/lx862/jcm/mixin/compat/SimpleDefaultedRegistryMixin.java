package com.lx862.jcm.mixin.compat;

import com.lx862.jcm.mod.util.JCMUtil;
import net.minecraft.util.Identifier;
#if MC_VERSION < "11904"
import net.minecraft.util.registry.DefaultedRegistry;
#else
import net.minecraft.registry.SimpleDefaultedRegistry;
#endif
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Block renamed in JCM 2.0.0, unfortunately there's no easy way to migrate ids.
 * This class helps migrate these ids
 * See <a href="https://github.com/Noaaan/MythicMetals/blob/1.20/src/main/java/nourl/mythicmetals/mixin/DefaultedRegistryMixin.java">here</a>
 * And <a href="https://github.com/orgs/FabricMC/discussions/2361">https://github.com/orgs/FabricMC/discussions/2361</a>
 */
#if MC_VERSION < "11904"
    @Mixin(DefaultedRegistry.class)
#else
    @Mixin(SimpleDefaultedRegistry.class)
#endif
public class SimpleDefaultedRegistryMixin {
    @ModifyVariable(at = @At("HEAD"), method = "get(Lnet/minecraft/util/Identifier;)Ljava/lang/Object;", ordinal = 0, argsOnly = true)
    Identifier dataFixerRegistry(@Nullable Identifier id) {
        if(id == null) return id;
        return JCMUtil.getMigrationId(new org.mtr.mapping.holder.Identifier(id)).data;
    }
}