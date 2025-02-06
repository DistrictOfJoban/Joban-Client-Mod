package com.lx862.jcm.mod.scripting;

import com.lx862.mtrscripting.api.ScriptResultCall;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.SoundHelper;
import org.mtr.mod.render.StoredMatrixTransformations;

public class SoundCall extends ScriptResultCall {
    private final SoundEvent soundEvent;
    private final double x;
    private final double y;
    private final double z;
    private final float volume;
    private final float pitch;

    public SoundCall(Identifier id, double x, double y, double z, float volume, float pitch) {
        this.soundEvent = SoundHelper.createSoundEvent(id);
        this.x = x;
        this.y = y;
        this.z = z;
        this.volume = volume;
        this.pitch = pitch;
    }

    @Override
    public void run(World world, GraphicsHolder graphicsHolder, StoredMatrixTransformations storedMatrixTransformations, Direction facing, int light) {
        world.playSound(x, y, z, soundEvent, SoundCategory.MASTER, volume, pitch, false);
    }
}
