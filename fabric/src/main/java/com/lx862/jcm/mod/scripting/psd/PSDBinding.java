package com.lx862.jcm.mod.scripting.psd;

import org.mtr.mapping.holder.BlockPos;

/**
 * One PSD script binding: the block position, the script it runs, and the
 * binding surface. {@code pidsPreset} selects the script source — a
 * {@code psdScripts} entry (false) or a scripted JCM PIDS preset (true).
 */
public final class PSDBinding {
    public final BlockPos pos;
    public final String scriptId;
    public final boolean pidsPreset;
    public final PSDWrapper.PSDType type;

    public PSDBinding(BlockPos pos, String scriptId, boolean pidsPreset, PSDWrapper.PSDType type) {
        this.pos = pos;
        this.scriptId = scriptId;
        this.pidsPreset = pidsPreset;
        this.type = type;
    }
}
