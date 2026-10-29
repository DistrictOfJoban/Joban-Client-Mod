package com.lx862.jcm.mixin;

import com.lx862.mtrscripting.mod.impl.mtr.server.DoorDirectionManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import org.mtr.mod.block.BlockPSDAPGDoorBase;
import org.mtr.mod.block.PlatformHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The catch-all layer of the server-side door-direction memory, complementing
 * {@link DoorBlockChangeMixin}.
 *
 * <p>{@code World#setBlockState} (the world mixin) covers every vanilla,
 * WorldEdit and Axiom per-block edit, plus hand placement, pistons,
 * explosions and structure placement. But bulk editors that write chunk
 * sections directly — Axiom's set-block-buffer operations being the
 * flagship example — bypass the world API entirely, and after the write
 * they update the chunk's heightmaps, because a bulk editor that skipped
 * {@code Heightmap#trackUpdate} would leave vanilla heightmaps stale and
 * visibly broken. {@code Heightmap#trackUpdate} is therefore the single
 * convergence point of all three write styles:
 *
 * <ul>
 *   <li>world API ({@code World#setBlockState} updates heightmaps after the
 *       section write),</li>
 *   <li>chunk API ({@code WorldChunk#setBlockState} does the same),</li>
 *   <li>section writers (Axiom's buffer ops call it explicitly per changed
 *       block, on the live section, on the server thread).</li>
 * </ul>
 *
 * <p>Only the NEW state reaches this hook (the write already happened), so
 * the fine-grained old-state test of the world mixin is unavailable; in
 * exchange the position is chunk-local and the owning chunk is right there
 * on the heightmap. Positions are noted at full world coordinates through
 * the same {@link DoorDirectionManager#onDoorBlockChange} the world mixin
 * uses — chunk-coalesced either way:
 * <ul>
 *   <li>the new block is a platform block or a PSD/APG door: note,
 *       regardless of the chunk index (a platform being built where none
 *       was known yet must still reach the changed-chunk set);</li>
 *   <li>otherwise: note only when the chunk's index says a route-served
 *       platform sits there — the section-writer's removal case (a
 *       platform block replaced with air has no old-state to test) plus a
 *       bounded, idempotent over-trigger for unrelated edits inside
 *       station chunks, which merely re-derives the same sides.</li>
 * </ul>
 *
 * <p>Filters, in order of cost: the server-thread guard (the heightmap is
 * the one hook level where worldgen can reach us — via wrapped proto
 * chunks during below-zero retrogen — and those worker-thread notes would
 * race the server thread's tick pipeline for zero value), the
 * {@code instanceof WorldChunk} check (drops proto-chunk worldgen outright,
 * before any map lookup) and the manager's raw-world identity map (drops
 * client worlds). Called up to four times per block write (once per
 * heightmap type), each pass is one failed check deep for the common case.
 *
 * <p>Like the world mixin: the platform/PSD-APG test stays inlined —
 * {@code com.lx862.jcm.mixin} is a mixin package declared by the mixin
 * configs, and Mixin refuses to load any plain class living in such a
 * package at runtime.
 */
@Mixin(Heightmap.class)
public class DoorHeightmapMixin {

    @Shadow
    @Final
    private Chunk chunk;

    @Inject(method = "trackUpdate(IIILnet/minecraft/block/BlockState;)Z", at = @At("HEAD"))
    private void jsblock$trackDoorRelevantHeightmapUpdate(int x, int y, int z, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if(!DoorDirectionManager.isServerThread()) return;
        if(!(this.chunk instanceof WorldChunk)) return;
        final World world = ((DoorWorldChunkAccessor) this.chunk).jcm$getWorld();
        if(world == null) return;
        final ChunkPos chunkPos = ((WorldChunk) this.chunk).getPos();
        final Block newBlock = state.getBlock();
        if(newBlock instanceof PlatformHelper || newBlock instanceof BlockPSDAPGDoorBase) {
            DoorDirectionManager.onDoorBlockChange(world, chunkPos.x * 16 + x, y, chunkPos.z * 16 + z);
            return;
        }
        if(!DoorDirectionManager.chunkMayContainPlatforms(world, chunkPos.x, chunkPos.z)) return;
        DoorDirectionManager.onDoorBlockChange(world, chunkPos.x * 16 + x, y, chunkPos.z * 16 + z);
    }
}
