package com.lx862.jcm.mod.scripting;

import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.SoundCategory;
import org.mtr.mapping.holder.SoundEvent;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.SoundHelper;

public class SoundCall {
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

    public void draw(World world) {
        world.playSound(x, y, z, soundEvent, SoundCategory.MASTER, volume, pitch, false);
    }
}
