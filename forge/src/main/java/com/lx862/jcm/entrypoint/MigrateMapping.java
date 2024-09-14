package com.lx862.jcm.entrypoint;


import com.lx862.jcm.mod.util.JCMUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.MissingMappingsEvent;
import org.mtr.mapping.holder.Identifier;

/**
 * Block renamed in JCM 2.0.0, this can be remapped on Forge via the MissingMappingsEvent.
 * This class helps migrate these ids
 */
public class MigrateMapping {
    @SubscribeEvent
    public void migrate(MissingMappingsEvent event) {
        event.getAllMappings(ForgeRegistries.BLOCKS.getRegistryKey()).forEach(entry -> {
            ResourceLocation newId = JCMUtil.getMigrationId(new Identifier(entry.getKey())).data;
            if(!newId.equals(entry.getKey())) {
                entry.remap(ForgeRegistries.BLOCKS.getValue(newId));
            }
        });
    }
}
