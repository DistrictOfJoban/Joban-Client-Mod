package com.lx862.mtrscripting.core.util.ante;

import com.lx862.mtrscripting.core.util.ScriptVector3f;
import com.lx862.mtrscripting.core.util.sound.TickableSoundInstanceJS;
import net.minecraft.client.sound.Sound;
import net.minecraft.client.sound.SoundInstance;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.SoundCategory;

@Deprecated
public class ANTETickableSoundJS {

    public final TickableSoundInstanceJS tickableSoundInstanceJS;

    public ANTETickableSoundJS(Identifier sound) {
        this.tickableSoundInstanceJS = TickableSoundInstanceJS.create(sound, SoundCategory.BLOCKS);
    }

    public ANTETickableSoundJS(Identifier sound, SoundCategory category) {
        this.tickableSoundInstanceJS = TickableSoundInstanceJS.create(sound, category);
    }

    public void setData(float volume, float pitch, ScriptVector3f pos) {
        this.tickableSoundInstanceJS.setSoundVolume(volume);
        this.tickableSoundInstanceJS.setSoundPitch(pitch);
        this.tickableSoundInstanceJS.setPos(pos);
    }

    public void setLooping(boolean bl) {
        this.tickableSoundInstanceJS.setLoopable(bl);
    }

    public void setDelay(int tick) {
        this.tickableSoundInstanceJS.setLoopDelay(tick);
    }

    public void setAttenuation(SoundInstance.AttenuationType type) {
    }

    public void play() {
        ANTESoundHelperJS.play(this);
    }

    public void quit() {
        ANTESoundHelperJS.stop(this);
    }

    public void pause() {
        ANTESoundHelperJS.stop(this);
    }

    public void setRelative(boolean relative) {
        this.tickableSoundInstanceJS.setRelative(relative);
    }

    public Sound getSound() {
        return this.tickableSoundInstanceJS.getSound();
    }

    public Identifier getLocation() {
        return this.tickableSoundInstanceJS.getId2();
    }

    public SoundCategory getSource() {
        return this.tickableSoundInstanceJS.getCategory2();
    }

    public boolean isLooping() {
        return this.tickableSoundInstanceJS.isPlaying();
    }

    public float getVolume() {
        return this.tickableSoundInstanceJS.getVolume2();
    }

    public float getPitch() {
        return this.tickableSoundInstanceJS.getPitch2();
    }

    public SoundInstance.AttenuationType getAttenuation() {
        return this.tickableSoundInstanceJS.getAttenuationType();
    }

    public int getDelay() {
        return this.tickableSoundInstanceJS.getRepeatDelay2();
    }

    public boolean isRelative() {
        return this.tickableSoundInstanceJS.isRelative();
    }
}
