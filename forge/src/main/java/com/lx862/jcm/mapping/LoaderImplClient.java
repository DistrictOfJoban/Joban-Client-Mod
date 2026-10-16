package com.lx862.jcm.mapping;

import com.lx862.jcm.mixin.mtrpatch.WorldRendererAccessor;
import com.lx862.jcm.mixin.mtrscripting.NativeImageAccessor;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.CameraType;
import net.minecraft.client.player.AbstractClientPlayer;
import org.mtr.mapping.holder.*;

import java.util.List;

/**
 * Forge implementation via Mojang mapping
 */
public class LoaderImplClient {
    public static long getNativeImagePointer(NativeImage nativeImage) {
        return ((NativeImageAccessor)(Object)nativeImage.data).getPixels();
    }

    public static List<PlayerEntity> getWorldPlayers(ClientWorld world) {
        List<PlayerEntity> players = new ObjectArrayList<>();
        for(AbstractClientPlayer entity : world.data.players()) {
            players.add(new PlayerEntity(entity));
        }
        return players;
    }

    public static Vector3d getEntityVelocity(Entity entity) {
        return new Vector3d(entity.data.getDeltaMovement());
    }

    public static void addPackOverride(Identifier id, String modid) {
    }

    public static int getCameraMode() {
        CameraType perspective = MinecraftClient.getInstance().getOptionsMapped().data.getCameraType();
        return perspective == CameraType.FIRST_PERSON ? 1 : perspective == CameraType.THIRD_PERSON_BACK ? 2 : 3;
    }

    public static boolean isFrustumVisible(Box box) {
        return ((WorldRendererAccessor)MinecraftClient.getInstance().getWorldRendererMapped().data).jsblock$getFrustum().isVisible(box.data);
    }
}
