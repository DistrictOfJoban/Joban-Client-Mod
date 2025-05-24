package com.lx862.jcm.mod.scripting.mtr.sound;

import com.lx862.mtrscripting.api.ScriptResultCall;
import org.mtr.mapping.holder.SoundEvent;

public abstract class SoundCall extends ScriptResultCall {
    protected final SoundEvent soundEvent;
    protected final float volume;
    protected final float pitch;

    public SoundCall(SoundEvent soundEvent, float volume, float pitch) {
        this.soundEvent = soundEvent;
        this.volume = volume;
        this.pitch = pitch;
    }
}
