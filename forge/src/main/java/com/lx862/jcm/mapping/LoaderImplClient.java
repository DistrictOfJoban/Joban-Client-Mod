package com.lx862.jcm.mapping;

import com.lx862.jcm.mixin.minecraft.NativeImageAccessor;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.player.AbstractClientPlayer;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ScreenExtension;

import java.util.List;

/**
 * Forge implementation via Mojang mapping
 */
public class LoaderImplClient {
    public static void openURLScreen(ScreenExtension parentScreen, String url) {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.openScreen(
                new Screen(new net.minecraft.client.gui.screens.ConfirmLinkScreen((confirmed) -> {
                    if(confirmed) {
                        Util.getOperatingSystem().open(url);
                    }
                    mc.openScreen(new Screen(parentScreen));
                }, url, true)
            )
        );
    }

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
}
