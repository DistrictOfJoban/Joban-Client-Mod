package com.lx862.mtrscripting.mod.impl.mtr.eyecandy;

import com.lx862.mtrscripting.core.util.render.ScriptRenderManager;
import com.lx862.mtrscripting.core.util.sound.ScriptSoundManager;
import com.lx862.mtrscripting.core.primitive.ParsedScript;
import com.lx862.mtrscripting.core.primitive.ScriptInstance;
import org.mtr.mapping.holder.MinecraftClient;

import java.util.Objects;

public class EyeCandyScriptInstance extends ScriptInstance<EyecandyBlockEntityWrapper> {
    private final EyecandyBlockEntityWrapper be;
    private ScriptRenderManager renderManager;
    private ScriptSoundManager soundManager;

    public EyeCandyScriptInstance(EyeCandyScriptContext context, EyecandyBlockEntityWrapper be, ParsedScript script) {
        super(context, script);
        this.be = be;
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
        boolean beRemoved = be.removed();
        boolean modelChanged = !Objects.equals(be.getModelId(), getContextObject().getName());
        boolean notInGame = MinecraftClient.getInstance().getWorldMapped() == null;
        return notInGame || beRemoved || modelChanged;
    }
}
