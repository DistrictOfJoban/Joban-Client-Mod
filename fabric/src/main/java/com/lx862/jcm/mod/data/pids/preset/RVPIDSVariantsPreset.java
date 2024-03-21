package com.lx862.jcm.mod.data.pids.preset;

import org.mtr.mapping.holder.Identifier;

import javax.annotation.Nonnull;

/**
 * Variations of RV PIDS Preset (Such as Door Closing PSD/APG)
 */
public class RVPIDSVariantsPreset extends RVPIDSPreset {
    private final Identifier background;
    public RVPIDSVariantsPreset(String id, String name, Identifier background) {
        super(id, name);
        this.background = background;
    }

    @Override
    public @Nonnull Identifier getBackground() {
        return background;
    }

    @Override
    public boolean isRowHidden(int row) {
        return row < 3;
    }
}
