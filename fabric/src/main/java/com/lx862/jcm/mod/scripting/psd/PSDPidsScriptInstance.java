package com.lx862.jcm.mod.scripting.psd;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.integration.MinecraftClientWrapper;
import com.lx862.mtrscripting.core.primitive.ParsedScript;
import com.lx862.mtrscripting.core.primitive.ScriptInstance;
import com.lx862.mtrscripting.core.util.ScriptVector3f;
import com.lx862.mtrscripting.core.util.render.ScriptRenderManager;
import com.lx862.mtrscripting.core.util.sound.ScriptSoundManager;
import com.lx862.mtrscripting.mod.gui.hud.SortableScriptInstance;
import com.lx862.jcm.mod.scripting.pids.PIDSWrapper;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.World;
import org.mtr.mod.block.BlockPSDAPGBase;
import org.mtr.mod.block.BlockPSDTop;

/**
 * One running "scripted PIDS preset bound to a PSD block" instance — the
 * wrapper the script sees is a synthesized {@link PIDSWrapper} (no
 * {@code PIDSBlockEntity} exists at a PSD), while the drawing context is the
 * PSD context, so the preset's Text/Texture/Rect calls replay onto the PSD
 * surface unchanged.
 */
@ApiInternal
public class PSDPidsScriptInstance extends ScriptInstance<PIDSWrapper> implements SortableScriptInstance {
    private final BlockPos blockPos;
    private ScriptSoundManager soundManager;
    private ScriptRenderManager renderManager;

    public PSDPidsScriptInstance(BlockPos blockPos, String scriptId, ParsedScript script) {
        super(new PSDScriptContext(scriptId), script);
        this.blockPos = blockPos;
        this.soundManager = new ScriptSoundManager();
        this.renderManager = new ScriptRenderManager();
    }

    public void saveRenderCalls(ScriptRenderManager renderManager) {
        this.renderManager.takeFrom(renderManager);
    }

    public void saveSoundCalls(ScriptSoundManager soundManager) {
        this.soundManager.takeFrom(soundManager);
    }

    public ScriptSoundManager getSoundManager() {
        return this.soundManager;
    }

    public ScriptRenderManager getRenderManager() {
        return this.renderManager;
    }

    @Override
    public boolean shouldInvalidate() {
        if (MinecraftClient.getInstance().getWorldMapped() == null) return true;
        final World world = MinecraftClient.getInstance().getWorldMapped();
        final BlockState state = world.getBlockState(blockPos);
        final Object block = state.getBlock().data;
        final boolean stillPsd = block instanceof BlockPSDAPGBase || block instanceof BlockPSDTop;
        return !stillPsd || !PSDClientBindings.isSameBinding(blockPos, getContextObject().getName());
    }

    @Override
    public int getSortScore() {
        return (int) (MinecraftClientWrapper.localPlayer().pos().distance(new ScriptVector3f(blockPos)) * 1000);
    }
}
