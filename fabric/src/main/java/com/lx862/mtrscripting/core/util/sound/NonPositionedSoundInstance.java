package com.lx862.mtrscripting.core.util.sound;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import org.mtr.mapping.holder.SoundCategory;
import org.mtr.mapping.holder.SoundEvent;
import org.mtr.mapping.mapper.AbstractSoundInstanceExtension;

@ApiInternal
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
