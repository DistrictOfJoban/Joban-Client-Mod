package com.lx862.jcm.mapping;

import net.fabricmc.loader.api.FabricLoader;
#if MC_VERSION >= "11903"
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
#else
import net.minecraft.network.packet.s2c.play.PlaySoundIdS2CPacket;
#endif

import net.minecraft.state.property.Property;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.SoundHelper;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Fabric implementation via Yarn mapping
 */
public class LoaderImpl {
    public static boolean isRainingAt(World world, BlockPos pos) {
        return world.data.hasRain(pos.data);
    }

    public static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir();
    }

    /** Get a block settings forcing it to be solid, as we don't want water to break our block. */
    public static BlockSettings getSolidBlockSettings(BlockSettings settings) {
        #if MC_VERSION >= "12001"
            return new BlockSettings(settings.data.solid());
        #else
        return settings;
        #endif
    }

    public static Item getItemFromId(Identifier id) {
        final Optional<net.minecraft.item.Item> itm;
        #if MC_VERSION < "11903"
            itm = net.minecraft.util.registry.Registry.ITEM.getOrEmpty(id.data);
        #else
            itm = net.minecraft.registry.Registries.ITEM.getOrEmpty(id.data);
        #endif
        return itm.map(Item::new).orElse(null);
    }

    public static Identifier getIdFromItem(Item itm) {
        #if MC_VERSION < "11903"
            return new Identifier(net.minecraft.util.registry.Registry.ITEM.getId(itm.data));
        #else
            return new Identifier(net.minecraft.registry.Registries.ITEM.getId(itm.data));
        #endif
    }

    public static int getRedstoneLevel(World world, BlockPos blockPos) {
        return world.data.getReceivedRedstonePower(blockPos.data);
    }

    public static Style withClipboardContentText(Style style, String content) {
        return new Style(style.data.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, content)));
    }

    public static Style withURLContentText(Style style, String urlContent) {
        return new Style(style.data.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, urlContent)));
    }

    public static Style withHoverContentText(Style style, MutableText content) {
        return new Style(style.data.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, content.data)));
    }

    public static void playSound(World world, ServerPlayerEntity playerEntity, Identifier soundId, SoundCategory soundCategory, BlockPos blockPos, float volume, float pitch) {
        #if MC_VERSION >= "11903"
        SoundEvent event = SoundHelper.createSoundEvent(soundId);
        playerEntity.data.networkHandler.sendPacket(
                new PlaySoundS2CPacket(RegistryEntry.of(event.data), soundCategory.data, blockPos.getX(), blockPos.getY(), blockPos.getZ(), volume, pitch, world.getRandom().data.nextLong())
        );
        #elif MC_VERSION >= "11900"
        playerEntity.data.networkHandler.sendPacket(new PlaySoundIdS2CPacket(soundId.data, soundCategory.data, new net.minecraft.util.math.Vec3d(blockPos.getX(), blockPos.getY(), blockPos.getZ()), volume, 1, world.getRandom().data.nextLong()));
        #else
        playerEntity.data.networkHandler.sendPacket(new PlaySoundIdS2CPacket(soundId.data, soundCategory.data, new net.minecraft.util.math.Vec3d(blockPos.getX(), blockPos.getY(), blockPos.getZ()), volume, 1));
        #endif
    }

    public static String getBlockStateValue(BlockState blockState, String stateName) {
        Property property = blockState.getBlock().data.getStateManager().getProperty(stateName);
        if(property == null) return null;
        return property.name(blockState.data.get(property));
    }
}
