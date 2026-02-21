package com.lx862.jcm.mod.scripting.mtr.sound;

import org.mtr.mapping.holder.SoundCategory;
import org.mtr.mapping.holder.SoundEvent;
import org.mtr.mapping.mapper.AbstractSoundInstanceExtension;

public class NonPositionedSoundInstance extends AbstractSoundInstanceExtension {
    protected NonPositionedSoundInstance(SoundEvent sound, SoundCategory category, float volume, float pitch) {
        super(sound, category);
        setVolumeMapped(volume);
        setPitchMapped(pitch);
        setIsRepeatableMapped(false);
        setIsRepeatableMapped(false);
        setIsRelativeMapped(true);
    }
}
