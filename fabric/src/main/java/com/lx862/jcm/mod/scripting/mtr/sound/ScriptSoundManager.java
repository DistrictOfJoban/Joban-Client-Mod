package com.lx862.jcm.mod.scripting.mtr.sound;

import com.lx862.mtrscripting.util.ScriptVector3f;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.World;

import java.util.ArrayList;
import java.util.List;

public class ScriptSoundManager {
    public final List<SoundCall> soundCalls;

    public ScriptSoundManager() {
        this.soundCalls = new ArrayList<>();
    }

    private ScriptSoundManager(List<SoundCall> soundCalls) {
        this.soundCalls = new ArrayList<>();
        this.soundCalls.addAll(soundCalls);
    }

    public void playLocalSound(Identifier id, float volume, float pitch) {
        queue(new NonPositionedSoundCall(id, volume, pitch));
    }

    public void playSound(Identifier id, ScriptVector3f pos, float volume, float pitch) {
        queue(new PositionedSoundCall(id, pos.x(), pos.y(), pos.z(), volume, pitch));
    }

    public void queue(SoundCall soundCall) {
        soundCall.validate();
        this.soundCalls.add(soundCall);
    }

    public void invoke(World world, ScriptVector3f basePos) {
        for(SoundCall soundCall : new ArrayList<>(soundCalls)) {
            soundCall.run(world, basePos);
        }
    }

    public ScriptSoundManager copy() {
        return new ScriptSoundManager(this.soundCalls);
    }

    public void reset() {
        this.soundCalls.clear();
    }
}
