package com.lx862.jcm.mixin;

import com.lx862.mtrscripting.mod.impl.mtr.server.DoorDirectionManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.mtr.mod.block.BlockPSDAPGDoorBase;
import org.mtr.mod.block.PlatformHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Feeds the server-side door-direction memory whenever a block change could
 * alter it: any {@code World#setBlockState} on a server world whose new —
 * or, after the chunk-index pre-filter, old — block is a platform block or
 * a PSD/APG door.
 *
 * <p>Hooked at the world level rather than the chunk: every vanilla,
 * WorldEdit and Axiom per-block edit path funnels through it on the
 * server, and the pre-filters below make the cost for unrelated edits
 * negligible — one instanceof per placement, and for the removal
 * direction a chunk-index lookup that only ever runs for chunks that
 * actually contain a route-served platform. The manager coalesces
 * positions by chunk and then by platform and re-scans a bounded number
 * of platforms per tick, so batch tools of any size turn into a short
 * trickle of re-scans instead of a lag spike.
 *
 * <p>The hook targets the FOUR-argument {@code setBlockState(pos, state,
 * flags, maxUpdateDepth)}: it is {@code World}'s real implementation of
 * {@code ModifiableWorld} (method_30092, present with the same shape on
 * every supported MC version), the 2- and 3-argument overloads delegate
 * into it (3-arg passes {@code 512}), and it is NOT overridden by
 * {@code ServerWorld} — so every world-level write dispatches here exactly
 * once, with no double-fire for the shorter overloads and no gap for direct
 * 4-argument callers (structure placement, some mods).
 *
 * <p>The world API is not the only write style on a server: bulk editors
 * that write chunk sections directly — Axiom's set-block-buffer
 * operations being the flagship example — bypass this hook entirely.
 * {@code DoorHeightmapMixin} closes that gap at the heightmap convergence
 * point that all three write styles (world API, chunk API, section
 * writers) must pass.
 *
 * <p>The platform/PSD-APG test is deliberately inlined rather than factored
 * into a helper class: {@code com.lx862.jcm.mixin} is a mixin package
 * declared by the mixin configs, and Mixin refuses to load any plain class
 * living in such a package at runtime ({@code IllegalClassLoadError}), so
 * nothing but mixins may sit next to this file.
 */
@Mixin(World.class)
public class DoorBlockChangeMixin {

    @Inject(method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;II)Z", at = @At("HEAD"))
    private void jsblock$trackDoorBlockChange(BlockPos pos, BlockState state, int flags, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir) {
        if(!((Object) this instanceof ServerWorld)) return;
        final Block newBlock = state.getBlock();
        if(newBlock instanceof PlatformHelper || newBlock instanceof BlockPSDAPGDoorBase) {
            DoorDirectionManager.onDoorBlockChange(this, pos.getX(), pos.getY(), pos.getZ());
            return;
        }
        if(!DoorDirectionManager.chunkMayContainPlatforms(this, pos.getX() >> 4, pos.getZ() >> 4)) return;
        final Block replacedBlock = ((World) (Object) this).getBlockState(pos).getBlock();
        if(replacedBlock instanceof PlatformHelper || replacedBlock instanceof BlockPSDAPGDoorBase) {
            DoorDirectionManager.onDoorBlockChange(this, pos.getX(), pos.getY(), pos.getZ());
        }
    }
}
