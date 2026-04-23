package com.lx862.mtrscripting.core.util.sound;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.util.ScriptVector3f;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.MovingSoundInstanceExtension;
import org.mtr.mapping.mapper.SoundHelper;

public class TickableSoundInstanceJS extends MovingSoundInstanceExtension {

    protected TickableSoundInstanceJS(Identifier soundId, String category) {
        this(SoundHelper.createSoundEvent(soundId), SoundCategory.valueOf(category));
    }

    protected TickableSoundInstanceJS(SoundEvent sound, SoundCategory category) {
        super(sound, category);
    }

    public static TickableSoundInstanceJS create(Identifier soundId, String category) {
        return new TickableSoundInstanceJS(soundId, category);
    }

    public TickableSoundInstanceJS setSoundVolume(float f) {
        setVolume(f);
        return this;
    }

    public TickableSoundInstanceJS setSoundPitch(float f) {
        setPitch(f);
        return this;
    }

    public TickableSoundInstanceJS setLoopable(boolean canLoop) {
        setIsRepeatableMapped(canLoop);
        return this;
    }

    public TickableSoundInstanceJS setLoopDelay(int tick) {
        setRepeatDelay(tick);
        return this;
    }

    public TickableSoundInstanceJS setRelative(boolean isRelative) {
        setIsRelativeMapped(isRelative);
        return this;
    }

    public TickableSoundInstanceJS setPos(ScriptVector3f pos) {
        setX(pos.x());
        setY(pos.y());
        setZ(pos.z());
        return this;
    }

    public boolean isPlaying() {
        return MinecraftClient.getInstance().getSoundManager().isPlaying(new SoundInstance(this));
    }

    @ApiInternal
    @Override
    public boolean shouldAlwaysPlay2() {
        return true;
    }

    @ApiInternal
    @Override
    public void tick2() {
    }
}
