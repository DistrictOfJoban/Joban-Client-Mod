package com.lx862.mtrscripting.core.util.sound;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.api.AbstractScriptContext;
import com.lx862.mtrscripting.core.util.ScriptVector3f;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.MovingSoundInstanceExtension;
import org.mtr.mapping.mapper.SoundHelper;

public class TickableSoundInstanceJS extends MovingSoundInstanceExtension implements AutoCloseable {
    private boolean instanceInUse = false;
    private boolean isRepeatable = false;

    protected TickableSoundInstanceJS(Identifier soundId, String category) {
        this(SoundHelper.createSoundEvent(soundId), SoundCategory.valueOf(category));
    }

    protected TickableSoundInstanceJS(SoundEvent sound, SoundCategory category) {
        super(sound, category);
    }

    @ApiInternal
    public static TickableSoundInstanceJS create(Identifier soundId, SoundCategory category) {
        return new TickableSoundInstanceJS(SoundHelper.createSoundEvent(soundId), category);
    }

    public static TickableSoundInstanceJS create(Identifier soundId, String category) {
        return new TickableSoundInstanceJS(soundId, category);
    }

    // TODO: Getters

    public TickableSoundInstanceJS setSoundVolume(float f) {
        setVolume(f);
        return this;
    }

    public TickableSoundInstanceJS setSoundPitch(float f) {
        setPitch(f);
        return this;
    }

    public TickableSoundInstanceJS setRepeatable(boolean canRepeat) {
        this.isRepeatable = canRepeat;
        return this;
    }

    /** Legacy method that directly sets the sound instance. Not recommended due to manually managed */
    @Deprecated
    public TickableSoundInstanceJS setLoopable(boolean canLoop) {
        super.setIsRepeatableMapped(canLoop);
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

    public void play(AbstractScriptContext abstractScriptContext) {
        play();
        abstractScriptContext.addDisposable(this);
    }

    public void play(AbstractScriptContext abstractScriptContext, int delay) {
        play(delay);
        abstractScriptContext.addDisposable(this);
    }

    public void play() {
        if(isInstanceInUse()) return; // Don't allow playing twice

        setIsRepeatableMapped(this.isRepeatable);
        setInstanceInUse(true);
        MinecraftClient.getInstance().submit(() -> {
            MinecraftClient.getInstance().getSoundManager().play(new SoundInstance(this), 0);
        });
    }

    public void play(int delay) {
        if(isInstanceInUse()) return; // Don't allow playing twice

        setInstanceInUse(true);
        setIsRepeatableMapped(this.isRepeatable);
        MinecraftClient.getInstance().submit(() -> {
            MinecraftClient.getInstance().getSoundManager().play(new SoundInstance(this), delay);
        });
    }

    public void stop() {
        if(!isInstanceInUse()) return;

        setInstanceInUse(false);
        super.setIsRepeatableMapped(false);
        MinecraftClient.getInstance().submit(() -> {
            MinecraftClient.getInstance().getSoundManager().stop(new SoundInstance(this));
        });
    }

    @ApiInternal
    public void setInstanceInUse(boolean isPlaying) {
        this.instanceInUse = isPlaying;
    }

    @ApiInternal
    public boolean isInstanceInUse() {
        return this.instanceInUse;
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

    @Override
    public void close() throws Exception {
        stop();
    }
}
