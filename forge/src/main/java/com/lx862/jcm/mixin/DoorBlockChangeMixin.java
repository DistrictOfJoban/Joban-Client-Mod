package com.lx862.jcm.mixin;

import com.lx862.mtrscripting.mod.impl.mtr.server.DoorDirectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Forge twin of the fabric {@code DoorBlockChangeMixin}: feeds the
 * server-side door-direction memory whenever a {@code Level#setBlock} on a
 * server world changes a platform block or a PSD/APG door (directly, or by
 * replacing/removing one — that direction only runs the second filter for
 * chunks the manager's index says contain a route-served platform).
 *
 * <p>See the fabric variant for the batching rationale: the manager
 * coalesces positions by chunk and platform and re-scans a bounded number
 * of platforms per tick, so WorldEdit/Axiom batch edits never lag the
 * server.
 */
@Mixin(Level.class)
public class DoorBlockChangeMixin {

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z", at = @At("HEAD"))
    private void jsblock$trackDoorBlockChange(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<Boolean> cir) {
        if(!((Object) this instanceof ServerLevel)) return;
        if(DoorBlockChangeHelper.isDoorBlock(state.getBlock())) {
            DoorDirectionManager.onDoorBlockChange(this, pos.getX(), pos.getY(), pos.getZ());
            return;
        }
        if(!DoorDirectionManager.chunkMayContainPlatforms(this, pos.getX() >> 4, pos.getZ() >> 4)) return;
        if(DoorBlockChangeHelper.isDoorBlock(((Level) (Object) this).getBlockState(pos).getBlock())) {
            DoorDirectionManager.onDoorBlockChange(this, pos.getX(), pos.getY(), pos.getZ());
        }
    }
}
