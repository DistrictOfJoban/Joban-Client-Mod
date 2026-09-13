package com.lx862.mtrscripting.mod.impl.mtr.lift;

import com.lx862.mtrscripting.core.primitive.ParsedScript;
import com.lx862.mtrscripting.core.primitive.ScriptInstance;
import com.lx862.mtrscripting.core.util.render.ScriptRenderManager;
import com.lx862.mtrscripting.core.util.sound.ScriptSoundManager;
import org.mtr.mapping.holder.MinecraftClient;

public class LiftScriptInstance extends ScriptInstance<LiftWrapper> {
    private final LiftWrapper lift;
    private ScriptRenderManager renderManager;
    private ScriptSoundManager soundManager;

    public LiftScriptInstance(LiftScriptContext context, LiftWrapper lift, ParsedScript script) {
        super(context, script);
        this.lift = lift;
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
        boolean beRemoved = lift.removed();
        boolean modelChanged = lift.styleChanged(getContextObject().getName());
        boolean notInGame = MinecraftClient.getInstance().getWorldMapped() == null;
        return notInGame || beRemoved || modelChanged;
    }
}
