package com.lx862.jcm.mapping;

import com.lx862.jcm.mixin.mtrscripting.NativeImageAccessor;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.mtr.mapping.holder.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Fabric implementation via Yarn mapping
 */
public class LoaderImplClient {
    public static List<PlayerEntity> getWorldPlayers(ClientWorld world) {
        List<PlayerEntity> players = new ArrayList<>();
        for(AbstractClientPlayerEntity entity : world.data.getPlayers()) {
            players.add(new PlayerEntity(entity));
        }
        return players;
    }

    public static Vector3d getEntityVelocity(Entity entity) {
        return new Vector3d(entity.data.getVelocity());
    }

    public static long getNativeImagePointer(NativeImage nativeImage) {
        return ((NativeImageAccessor)(Object)nativeImage.data).getPointer();
    }

    public static void addPackOverride(Identifier id, String modid) {
        ResourceManagerHelper.registerBuiltinResourcePack(id.data, FabricLoader.getInstance().getModContainer(modid).orElseThrow(), ResourcePackActivationType.ALWAYS_ENABLED);
    }
}
