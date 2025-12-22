package com.lx862.jcm.mod.scripting.mtr.sound;

import com.lx862.mtrscripting.api.ScriptResultCall;
import com.lx862.mtrscripting.util.ScriptVector3f;
import org.mtr.mapping.holder.SoundEvent;
import org.mtr.mapping.holder.World;

public abstract class SoundCall implements ScriptResultCall {
    protected final SoundEvent soundEvent;
    protected final String soundCategory = "MASTER";
    protected final float volume;
    protected final float pitch;

    public SoundCall(SoundEvent soundEvent, float volume, float pitch) {
        this.soundEvent = soundEvent;
        this.volume = volume;
        this.pitch = pitch;
    }

    public abstract void run(World world, ScriptVector3f basePos);

    @Override
    public void validate() {
        if(this.soundEvent == null) throw new IllegalStateException("SoundEvent must not be null!");
        if(this.volume < 0) throw new IllegalStateException("Volume must not be negative!");
        if(this.pitch < 0) throw new IllegalStateException("Pitch must not be negative!");
    }
}
