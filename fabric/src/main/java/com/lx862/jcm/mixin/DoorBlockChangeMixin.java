package com.lx862.jcm.mixin;

import com.lx862.mtrscripting.mod.impl.mtr.server.DoorDirectionManager;
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
 * WorldEdit and Axiom edit path funnels through it on the server, and the
 * pre-filters below make the cost for unrelated edits negligible — one
 * instanceof per placement, and for the removal direction a chunk-index
 * lookup that only ever runs for chunks that actually contain a route-served
 * platform. The manager coalesces positions by chunk and then by platform
 * and re-scans a bounded number of platforms per tick, so batch tools of
 * any size turn into a short trickle of re-scans instead of a lag spike.
 */
@Mixin(World.class)
public class DoorBlockChangeMixin {

    @Inject(method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z", at = @At("HEAD"))
    private void jsblock$trackDoorBlockChange(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<Boolean> cir) {
        if(!((Object) this instanceof ServerWorld)) return;
        if(DoorBlockChangeHelper.isDoorBlock(state.getBlock())) {
            DoorDirectionManager.onDoorBlockChange(this, pos.getX(), pos.getY(), pos.getZ());
            return;
        }
        if(!DoorDirectionManager.chunkMayContainPlatforms(this, pos.getX() >> 4, pos.getZ() >> 4)) return;
        if(DoorBlockChangeHelper.isDoorBlock(((World) (Object) this).getBlockState(pos).getBlock())) {
            DoorDirectionManager.onDoorBlockChange(this, pos.getX(), pos.getY(), pos.getZ());
        }
    }
}
