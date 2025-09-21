package com.lx862.jcm.mod.scripting.mtr.sound;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.SoundHelper;
import org.mtr.mod.render.StoredMatrixTransformations;

public class PositionedSoundCall extends SoundCall {
    protected final double x;
    protected final double y;
    protected final double z;

    public PositionedSoundCall(Identifier id, double x, double y, double z, float volume, float pitch) {
        super(SoundHelper.createSoundEvent(id), volume, pitch);
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void run(World world, GraphicsHolder graphicsHolder, StoredMatrixTransformations storedMatrixTransformations, Direction facing, int light) {
        world.playSound(x, y, z, soundEvent, SoundCategory.valueOf(soundCategory), volume, pitch, false);
    }
}
