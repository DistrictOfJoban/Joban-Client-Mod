package com.lx862.jcm.mapping;

#if MC_VERSION >= "11903"
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
#else
import net.minecraft.network.protocol.game.ClientboundCustomSoundPacket;
#endif

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.fml.loading.FMLPaths;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.SoundHelper;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Forge implementation via Mojang mapping
 */
public class LoaderImpl {
    public static boolean isRainingAt(World world, BlockPos pos) {
        return world.data.isRainingAt(pos.data);
    }

    public static Path getConfigPath() {
        return FMLPaths.CONFIGDIR.get();
    }

    /** Get a block settings forcing it to be solid, as we don't want water to break our block. */
    public static BlockSettings getSolidBlockSettings(BlockSettings settings) {
        #if MC_VERSION >= "12001"
            return new BlockSettings(settings.data.forceSolidOn());
        #else
            return settings;
        #endif
    }

    public static Item getItemFromId(Identifier id) {
        #if MC_VERSION < "11903"
            final Optional<net.minecraft.world.item.Item> itm;
            itm = net.minecraft.core.Registry.ITEM.getOptional(id.data);
        #else
            final Optional<net.minecraft.world.item.Item> itm;
            itm = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(id.data);
        #endif
        return itm.map(Item::new).orElse(null);
    }

    public static Identifier getIdFromItem(Item itm) {
        #if MC_VERSION < "11903"
            return new Identifier(net.minecraft.core.Registry.ITEM.getKey(itm.data));
        #else
            return new Identifier(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(itm.data));
        #endif
    }

    public static int getRedstoneLevel(World world, BlockPos blockPos) {
        return world.data.getBestNeighborSignal(blockPos.data);
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
        playerEntity.data.connection.send(
                new ClientboundSoundPacket(Holder.direct(event.data), soundCategory.data, blockPos.getX(), blockPos.getY(), blockPos.getZ(), volume, pitch, world.getRandom().data.nextLong())
        );
        #elif MC_VERSION >= "11900"
        playerEntity.data.connection.send(new ClientboundCustomSoundPacket(soundId.data, soundCategory.data, new net.minecraft.world.phys.Vec3(blockPos.getX(), blockPos.getY(), blockPos.getZ()), volume, 1, world.getRandom().data.nextLong()));
        #else
        playerEntity.data.connection.send(new ClientboundCustomSoundPacket(soundId.data, soundCategory.data, new net.minecraft.world.phys.Vec3(blockPos.getX(), blockPos.getY(), blockPos.getZ()), volume, 1));
        #endif
    }

    public static String getBlockStateValue(BlockState blockState, String stateName) {
        Property property = blockState.getBlock().data.getStateDefinition().getProperty(stateName);
        if(property == null) return null;
        return property.getName(blockState.data.getValue(property));
    }
}
