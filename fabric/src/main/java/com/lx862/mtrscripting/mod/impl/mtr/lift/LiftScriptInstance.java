package com.lx862.mtrscripting.mod.impl.mtr.lift;

import com.lx862.mtrscripting.core.integration.MinecraftClientWrapper;
import com.lx862.mtrscripting.core.primitive.ParsedScript;
import com.lx862.mtrscripting.core.primitive.ScriptInstance;
import com.lx862.mtrscripting.core.util.render.ScriptRenderManager;
import com.lx862.mtrscripting.core.util.sound.ScriptSoundManager;
import com.lx862.mtrscripting.mod.gui.hud.SortableScriptInstance;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mod.client.VehicleRidingMovement;

public class LiftScriptInstance extends ScriptInstance<LiftWrapper> implements SortableScriptInstance {
    private ScriptRenderManager renderManager;
    private ScriptSoundManager soundManager;

    public LiftScriptInstance(LiftScriptContext context, LiftWrapper lift, ParsedScript script) {
        super(context, script);
        this.soundManager = new ScriptSoundManager();
        this.renderManager = new ScriptRenderManager();
        setWrapperObject(lift);
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
        boolean beRemoved = getWrapperObject().removed();
        boolean modelChanged = getWrapperObject().styleChanged(getContextObject().getName());
        boolean notInGame = MinecraftClient.getInstance().getWorldMapped() == null;
        return notInGame || beRemoved || modelChanged;
    }

    @Override
    public int getSortScore() {
        if(VehicleRidingMovement.isRiding(getWrapperObject().getId())) return Integer.MIN_VALUE;
        return (int)(MinecraftClientWrapper.localPlayer().pos().distance(getWrapperObject().getPos()) * 1000);
    }
}
