package com.lx862.jcm.mixin.mtrscripting;

import com.lx862.mtrscripting.mod.resource.EyecandyResourceProvider;
import com.lx862.mtrscripting.mod.resource.MtrScriptingResourceManager;
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
    @Inject(method = "render(Lorg/mtr/mod/block/BlockEyeCandy$BlockEntity;FLorg/mtr/mapping/mapper/GraphicsHolder;II)V", at = @At(value = "INVOKE", target = "Lorg/mtr/mod/block/BlockEyeCandy$BlockEntity;getModelId()Ljava/lang/String;"), cancellable = true)
    public void renderScript(BlockEyeCandy.BlockEntity blockEntity, float tickDelta, GraphicsHolder graphicsHolder, int light, int overlay, CallbackInfo ci) {
        World world = blockEntity.getWorld2();
        if(world == null) return;

        String eyecandyId = blockEntity.getModelId();
        EyecandyResourceProvider.EyecandyScriptConfiguration eyecandyScriptEntry = MtrScriptingResourceManager.eyecandy.getScriptEntry(eyecandyId);
        if(eyecandyScriptEntry == null) return;
        String nativeId=MtrScriptingResourceManager.eyecandy.getScriptEntryId(eyecandyId);
        if(com.lx862.jcm.nativeapi.NativeScriptManager.isLoaded(nativeId)) {
            BlockPos pos=blockEntity.getPos2();
            Direction facing=IBlock.getStatePropertySafe(world,pos,BlockEyeCandy.FACING);
            StoredMatrixTransformations transforms=new StoredMatrixTransformations(0.5+pos.getX(),pos.getY(),0.5+pos.getZ());
            transforms.add(gh->{
                gh.translate(blockEntity.getTranslateX(),blockEntity.getTranslateY(),blockEntity.getTranslateZ());
                gh.rotateYRadians(-(float)Math.toRadians(facing.asRotation())+(float)Math.PI);
                gh.rotateXRadians(blockEntity.getRotateX()+(float)Math.PI);
                gh.rotateYRadians(blockEntity.getRotateY()); gh.rotateZRadians(blockEntity.getRotateZ());
            });
            com.lx862.jcm.nativeapi.NativeContentDriver.eyecandy(nativeId,blockEntity,world,transforms,facing,light);
            if(MtrScriptingResourceManager.eyecandy.isScriptControlledRendering(eyecandyId)) ci.cancel();
            return;
        }
        ParsedScript parsedScript = eyecandyScriptEntry.parsedScript();
        EyecandyBlockEntityWrapper eyecandyWrapper = new EyecandyBlockEntityWrapper(blockEntity);

        ScriptInstance<EyecandyBlockEntityWrapper> scriptInstance = MTRContentScripting.getScriptManager().getInstanceManager().getInstance(new UniqueKey("eyecandy", eyecandyId, blockEntity.getPos2().getX(), blockEntity.getPos2().getY(), blockEntity.getPos2().getZ()), () -> new EyeCandyScriptInstance(new EyeCandyScriptContext(eyecandyWrapper), eyecandyWrapper, parsedScript));
        if(!(scriptInstance instanceof EyeCandyScriptInstance)) return;

        if(MtrScriptingResourceManager.eyecandy.isScriptControlledRendering(eyecandyId)) {
            ci.cancel();
        }

        EyeCandyScriptInstance eyeCandyScriptInstance = (EyeCandyScriptInstance) scriptInstance;
        EyeCandyScriptContext eyeCandyScriptContext = (EyeCandyScriptContext)scriptInstance.getContextObject();
        scriptInstance.setWrapperObject(eyecandyWrapper);

        scriptInstance.getScript().invokeRenderFunctions(scriptInstance, () -> {
            eyeCandyScriptInstance.updateRenderer(eyeCandyScriptContext.getRenderManager());
            eyeCandyScriptInstance.updateSound(eyeCandyScriptContext.getSoundManager());
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
        eyeCandyScriptInstance.getSoundManager().invoke(world, eyecandyWrapper.pos());
    }
}
