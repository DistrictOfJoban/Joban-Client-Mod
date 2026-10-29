package com.lx862.jcm.mixin;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Forge twin of the fabric {@code DoorWorldChunkAccessor}: the
 * {@code LevelChunk#level} backing field, for the door-direction heightmap
 * hook — a {@code Heightmap} only carries its owning chunk, and the chunk's
 * level reference (the raw per-dimension singleton the manager
 * identity-keys) is what routes a bulk section write to the right
 * dimension's state. Field, not method, so no accessor visibility can vary
 * across the six supported MC versions.
 */
@Mixin(LevelChunk.class)
public interface DoorLevelChunkAccessor {

    @Accessor("level")
    Level jcm$getLevel();
}
