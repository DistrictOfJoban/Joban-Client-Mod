package com.lx862.jcm.mixin;

import com.lx862.mtrscripting.mod.impl.mtr.server.DoorDirectionManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.mtr.mod.block.BlockPSDAPGDoorBase;
import org.mtr.mod.block.PlatformHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Forge twin of the fabric {@code DoorHeightmapMixin}: the catch-all layer
 * of the server-side door-direction memory for bulk editors that write
 * chunk sections directly (Axiom's set-block-buffer operations update the
 * chunk's heightmaps per changed block, on the live section, on the server
 * thread — a bulk editor that skipped {@code Heightmap#update} would leave
 * vanilla heightmaps stale). {@code Heightmap#update} therefore converges
 * all three write styles — the world API, the chunk API, and section
 * writers — on every supported MC version.
 *
 * <p>See the fabric variant for the filter order (server-thread guard,
 * {@code instanceof LevelChunk}, raw-level identity map), the new-state-only
 * coarsening (note any update in a route-served platform's chunk — the
 * section-writer's removal case has no old state to test), and the
 * chunk-coalesced batching that turns batch edits into a trickle of
 * re-scans. Mojang-mapped names: {@code Heightmap#update} takes chunk-local
 * x/z and a world-absolute y, exactly the convention the fabric twin
 * converts with.
 */
@Mixin(Heightmap.class)
public class DoorHeightmapMixin {

    @Shadow
    @Final
    private ChunkAccess chunk;

    @Inject(method = "update(IIILnet/minecraft/world/level/block/state/BlockState;)Z", at = @At("HEAD"))
    private void jsblock$trackDoorRelevantHeightmapUpdate(int x, int y, int z, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if(!DoorDirectionManager.isServerThread()) return;
        if(!(this.chunk instanceof LevelChunk)) return;
        final Level level = ((DoorLevelChunkAccessor) this.chunk).jcm$getLevel();
        if(level == null) return;
        final ChunkPos chunkPos = ((LevelChunk) this.chunk).getPos();
        final Block newBlock = state.getBlock();
        if(newBlock instanceof PlatformHelper || newBlock instanceof BlockPSDAPGDoorBase) {
            DoorDirectionManager.onDoorBlockChange(level, chunkPos.x * 16 + x, y, chunkPos.z * 16 + z);
            return;
        }
        if(!DoorDirectionManager.chunkMayContainPlatforms(level, chunkPos.x, chunkPos.z)) return;
        DoorDirectionManager.onDoorBlockChange(level, chunkPos.x * 16 + x, y, chunkPos.z * 16 + z);
    }
}
