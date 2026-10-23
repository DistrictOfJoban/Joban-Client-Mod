package com.lx862.mtrscripting.mod.impl.mtr.eyecandy;

import com.lx862.mtrscripting.core.integration.MinecraftClientWrapper;
import com.lx862.mtrscripting.core.util.ScriptVector3f;
import com.lx862.mtrscripting.core.util.render.ScriptRenderManager;
import com.lx862.mtrscripting.core.util.sound.ScriptSoundManager;
import com.lx862.mtrscripting.core.primitive.ParsedScript;
import com.lx862.mtrscripting.core.primitive.ScriptInstance;
import com.lx862.mtrscripting.mod.gui.hud.SortableScriptInstance;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mod.block.BlockEyeCandy;

import java.util.Objects;

public class EyeCandyScriptInstance extends ScriptInstance<EyecandyBlockEntityWrapper> implements SortableScriptInstance {
    private ScriptRenderManager renderManager;
    private ScriptSoundManager soundManager;
    private final BlockEyeCandy.BlockEntity be;

    public EyeCandyScriptInstance(BlockEyeCandy.BlockEntity blockEntity, EyeCandyScriptContext context, ParsedScript script) {
        super(context, script);
        this.be = blockEntity;
        this.soundManager = new ScriptSoundManager();
        this.renderManager = new ScriptRenderManager();
    }

    public void updateRenderer(ScriptRenderManager renderManager) {
        this.renderManager = renderManager.copy();
    }

    public void updateSound(ScriptSoundManager soundManager) {
        this.soundManager = soundManager.copy();
    }

    public ScriptSoundManager getSoundManager() {
        return this.soundManager;
    }

    public ScriptRenderManager getRenderManager() {
        return this.renderManager;
    }

    public boolean shouldInvalidate() {
        boolean beRemoved = be.isRemoved2() || be.getWorld2().getBlockEntity(be.getPos2()) == null;
        boolean modelChanged = getWrapperObject() != null && getWrapperObject().modelChanged(getContextObject().getName());
        boolean notInGame = MinecraftClient.getInstance().getWorldMapped() == null;
        return notInGame || beRemoved || modelChanged;
    }

    @Override
    public int getSortScore() {
        return (int)(MinecraftClientWrapper.localPlayer().pos().distance(new ScriptVector3f(be.getPos2())) * 1000);
    }
}
