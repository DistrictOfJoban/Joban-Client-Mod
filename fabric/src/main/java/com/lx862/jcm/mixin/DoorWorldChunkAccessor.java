package com.lx862.jcm.mixin;

import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The {@code WorldChunk#world} backing field, for the door-direction
 * heightmap hook: a {@code Heightmap} only carries its owning chunk, and
 * the chunk's world reference (the raw per-dimension singleton the
 * manager identity-keys) is what routes a bulk section write to the right
 * dimension's state. Field, not method, so no accessor visibility can vary
 * across the six supported MC versions.
 */
@Mixin(WorldChunk.class)
public interface DoorWorldChunkAccessor {

    @Accessor("world")
    World jcm$getWorld();
}
