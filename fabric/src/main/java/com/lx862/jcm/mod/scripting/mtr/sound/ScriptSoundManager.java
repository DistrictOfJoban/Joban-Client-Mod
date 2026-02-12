package com.lx862.jcm.mod.scripting.mtr.sound;

import com.lx862.mtrscripting.util.ScriptVector3f;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.World;

import java.util.ArrayList;
import java.util.List;

public class ScriptSoundManager {
    private final List<SoundCall> soundCalls;

    public ScriptSoundManager() {
        this(new ArrayList<>());
    }

    private ScriptSoundManager(List<SoundCall> soundCalls) {
        this.soundCalls = soundCalls;
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
        for(SoundCall soundCall : soundCalls) {
            soundCall.run(world, basePos);
        }
    }

    public ScriptSoundManager copy() {
        return new ScriptSoundManager(new ArrayList<>(this.soundCalls));
    }

    public void addCallsFrom(ScriptSoundManager other) {
        this.soundCalls.addAll(other.soundCalls);
    }

    public void reset() {
        this.soundCalls.clear();
    }
}
