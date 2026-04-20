package com.lx862.jcm.mixin.modded.mtr;

import com.lx862.mtrscripting.mod.resource.MTRContentResourceManager;
import com.lx862.mtrscripting.mod.impl.mtr.MTRContentScripting;
import com.lx862.mtrscripting.mod.impl.mtr.eyecandy.EyeCandyScriptContext;
import com.lx862.mtrscripting.mod.impl.mtr.eyecandy.EyeCandyScriptInstance;
import com.lx862.mtrscripting.mod.impl.mtr.eyecandy.EyecandyBlockEntityWrapper;
import com.lx862.mtrscripting.core.primitive.ParsedScript;
import com.lx862.mtrscripting.core.primitive.UniqueKey;
import com.lx862.mtrscripting.core.primitive.ScriptInstance;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.block.BlockEyeCandy;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.render.RenderEyeCandy;
import org.mtr.mod.render.StoredMatrixTransformations;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderEyeCandy.class, remap = false)
public class RenderEyeCandyMixin {
    @Inject(method = "render(Lorg/mtr/mod/block/BlockEyeCandy$BlockEntity;FLorg/mtr/mapping/mapper/GraphicsHolder;II)V", at = @At(value = "INVOKE", target = "Lorg/mtr/mod/block/BlockEyeCandy$BlockEntity;getModelId()Ljava/lang/String;"))
    public void renderScript(BlockEyeCandy.BlockEntity blockEntity, float tickDelta, GraphicsHolder graphicsHolder, int light, int overlay, CallbackInfo ci) {
        World world = blockEntity.getWorld2();
        if(world == null) return;

        ParsedScript script = MTRContentResourceManager.getEyecandyScript(blockEntity.getModelId());
        if(script == null) return;
        EyecandyBlockEntityWrapper beWrapper = new EyecandyBlockEntityWrapper(blockEntity);

        ScriptInstance<EyecandyBlockEntityWrapper> scriptInstance = MTRContentScripting.getScriptManager().getInstanceManager().getInstance(new UniqueKey("eyecandy", blockEntity.getModelId(), blockEntity.getPos2().getX(), blockEntity.getPos2().getY(), blockEntity.getPos2().getZ()), () -> new EyeCandyScriptInstance(new EyeCandyScriptContext(beWrapper), beWrapper, script));
        if(!(scriptInstance instanceof EyeCandyScriptInstance)) return;

        EyeCandyScriptInstance eyeCandyScriptInstance = (EyeCandyScriptInstance) scriptInstance;
        EyeCandyScriptContext eyeCandyScriptContext = (EyeCandyScriptContext)scriptInstance.getContextObject();
        scriptInstance.setWrapperObject(beWrapper);

        scriptInstance.getScript().invokeRenderFunctions(scriptInstance, () -> {
            eyeCandyScriptInstance.updateRenderer(eyeCandyScriptContext.renderManager());
            eyeCandyScriptInstance.updateSound(eyeCandyScriptContext.soundManager());
            eyeCandyScriptContext.resetForNextRun();
        });

        BlockPos blockPos = blockEntity.getPos2();
        Direction facing = IBlock.getStatePropertySafe(blockEntity.getWorld2(), blockPos, BlockEyeCandy.FACING);

        StoredMatrixTransformations blockStoredMatrixTransformations = new StoredMatrixTransformations(0.5 + (double)blockEntity.getPos2().getX(), (double)blockEntity.getPos2().getY(), 0.5 + (double)blockEntity.getPos2().getZ());
        StoredMatrixTransformations storedMatrixTransformations = blockStoredMatrixTransformations.copy();

        storedMatrixTransformations.add((graphicsHolderNew) -> {
            graphicsHolderNew.translate(blockEntity.getTranslateX(), blockEntity.getTranslateY(), blockEntity.getTranslateZ());
            graphicsHolderNew.rotateYRadians(-(float)Math.toRadians(facing.asRotation()) + (float)Math.PI);
            graphicsHolderNew.rotateXRadians(blockEntity.getRotateX() + (float)Math.PI);
            graphicsHolderNew.rotateYRadians(blockEntity.getRotateY());
            graphicsHolderNew.rotateZRadians(blockEntity.getRotateZ());
        });

        eyeCandyScriptInstance.getRenderManager().invoke(world, storedMatrixTransformations, facing, light);
        eyeCandyScriptInstance.getSoundManager().invoke(world, beWrapper.pos());
    }
}
