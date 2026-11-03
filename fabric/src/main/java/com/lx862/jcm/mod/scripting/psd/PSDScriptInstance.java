package com.lx862.jcm.mod.scripting.psd;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.integration.MinecraftClientWrapper;
import com.lx862.mtrscripting.core.primitive.ParsedScript;
import com.lx862.mtrscripting.core.primitive.ScriptInstance;
import com.lx862.mtrscripting.core.util.ScriptVector3f;
import com.lx862.mtrscripting.core.util.render.ScriptRenderManager;
import com.lx862.mtrscripting.core.util.sound.ScriptSoundManager;
import com.lx862.mtrscripting.mod.gui.hud.SortableScriptInstance;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.World;
import org.mtr.mod.block.BlockPSDAPGBase;
import org.mtr.mod.block.BlockPSDTop;

/**
 * One running PSD script instance — one per bound block position, keyed by
 * {@code UniqueKey("psd", scriptId, x, y, z)} in the JCM script manager.
 *
 * <p>Invalidation follows the binding: the instance dies when the block is no
 * longer a PSD glass/top block or when the player rebinds the position to
 * another script (the context name no longer matches).</p>
 */
@ApiInternal
public class PSDScriptInstance extends ScriptInstance<PSDWrapper> implements SortableScriptInstance {
    private final BlockPos blockPos;
    private ScriptSoundManager soundManager;
    private ScriptRenderManager renderManager;

    public PSDScriptInstance(BlockPos blockPos, String scriptId, ParsedScript script) {
        super(new PSDScriptContext(scriptId), script);
        this.blockPos = blockPos;
        this.soundManager = new ScriptSoundManager();
        this.renderManager = new ScriptRenderManager();
    }

    public void saveRenderCalls(ScriptRenderManager renderManager) {
        /* Swap, not copy — same steady-state allocation rule as the PIDS path. */
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
        final World world = World.cast(MinecraftClient.getInstance().getWorldMapped());
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
