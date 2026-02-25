/**
 * Block renamed in JCM 2.0.0, unfortunately there's no easy way to migrate ids.
 * This class helps migrate these ids
 */

#if MC_VERSION < "11902"
package com.lx862.jcm.entrypoint;

import com.lx862.jcm.mod.util.JCMUtil;
import net.minecraft.world.level.block.Block;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.mtr.mapping.holder.Identifier;

public class MigrateMapping {
    @SubscribeEvent
    public void migrate(RegistryEvent.MissingMappings<Block> event) {
        event.getAllMappings().forEach(entry -> {
            ResourceLocation newId = JCMUtil.getMigrationId(new Identifier(entry.key)).data;
            if(!newId.equals(entry.key)) {
                entry.remap(ForgeRegistries.BLOCKS.getValue(newId));
            }
        });
    }
}
#else
package com.lx862.jcm.entrypoint;


import com.lx862.jcm.mod.util.JCMUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.MissingMappingsEvent;
import org.mtr.mapping.holder.Identifier;

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
#endif