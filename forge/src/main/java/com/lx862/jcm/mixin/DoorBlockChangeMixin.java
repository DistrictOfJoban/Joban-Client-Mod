package com.lx862.jcm.mixin;

import com.lx862.mtrscripting.mod.impl.mtr.server.DoorDirectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.mtr.mod.block.BlockPSDAPGDoorBase;
import org.mtr.mod.block.PlatformHelper;
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
 *
 * <p>Like the fabric twin, the hook targets the FOUR-argument
 * {@code setBlock(pos, state, flags, maxUpdateDepth)}: it is {@code Level}'s
 * real implementation of {@code LevelWriter}, the 2- and 3-argument
 * overloads delegate into it (3-arg passes {@code 512}), and it is not
 * overridden by {@code ServerLevel} — every world-level write dispatches
 * here exactly once.
 *
 * <p>The platform/PSD-APG test is deliberately inlined rather than factored
 * into a helper class: {@code com.lx862.jcm.mixin} is a mixin package
 * declared by the mixin configs, and Mixin refuses to load any plain class
 * living in such a package at runtime ({@code IllegalClassLoadError}), so
 * nothing but mixins may sit next to this file.
 */
@Mixin(Level.class)
public class DoorBlockChangeMixin {

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"))
    private void jsblock$trackDoorBlockChange(BlockPos pos, BlockState state, int flags, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir) {
        if(!((Object) this instanceof ServerLevel)) return;
        final Block newBlock = state.getBlock();
        if(newBlock instanceof PlatformHelper || newBlock instanceof BlockPSDAPGDoorBase) {
            DoorDirectionManager.onDoorBlockChange(this, pos.getX(), pos.getY(), pos.getZ());
            return;
        }
        if(!DoorDirectionManager.chunkMayContainPlatforms(this, pos.getX() >> 4, pos.getZ() >> 4)) return;
        final Block replacedBlock = ((Level) (Object) this).getBlockState(pos).getBlock();
        if(replacedBlock instanceof PlatformHelper || replacedBlock instanceof BlockPSDAPGDoorBase) {
            DoorDirectionManager.onDoorBlockChange(this, pos.getX(), pos.getY(), pos.getZ());
        }
    }
}
