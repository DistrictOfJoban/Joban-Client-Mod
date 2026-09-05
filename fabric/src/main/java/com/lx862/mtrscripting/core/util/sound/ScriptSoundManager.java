package com.lx862.mtrscripting.core.util.sound;

import com.lx862.mtrscripting.core.util.ScriptVector3f;
import org.mtr.mapping.holder.*;

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
        queue(new NonPositionedSoundCall(id, SoundCategory.getMasterMapped(), volume, pitch));
    }

    public void playLocalSound(Identifier id, float volume, float pitch, String soundCategory) {
        queue(new NonPositionedSoundCall(id, SoundCategory.valueOf(soundCategory), volume, pitch));
    }

    public void playSound(Identifier id, ScriptVector3f pos, float volume, float pitch) {
        queue(new PositionedSoundCall(id, SoundCategory.getMasterMapped(), pos.x(), pos.y(), pos.z(), volume, pitch));
    }

    public void playSound(Identifier id, ScriptVector3f pos, float volume, float pitch, String soundCategory) {
        queue(new PositionedSoundCall(id, SoundCategory.valueOf(soundCategory), pos.x(), pos.y(), pos.z(), volume, pitch));
    }

    public void play(TickableSoundInstanceJS soundInstance) {
        if(soundInstance.isInstanceInUse()) return; // Don't allow playing twice

        soundInstance.setInstanceInUse(true);
        MinecraftClient.getInstance().submit(() -> {
            MinecraftClient.getInstance().getSoundManager().play(new SoundInstance(soundInstance));
        });
    }

    public void stop(TickableSoundInstanceJS soundInstance) {
        soundInstance.setInstanceInUse(false);
        soundInstance.setLoopable(false); // Prevent looping sound from repeating again
        MinecraftClient.getInstance().submit(() -> {
            MinecraftClient.getInstance().getSoundManager().stop(new SoundInstance(soundInstance));
        });
    }

    public void queue(SoundCall soundCall) {
        soundCall.validate();
        this.soundCalls.add(soundCall);
    }

    public void invoke(World world, ScriptVector3f basePos) {
        List<SoundCall> soundCalls = new ArrayList<>(this.soundCalls);

        for(SoundCall soundCall : soundCalls) {
            soundCall.run(world, basePos);
        }
        // Unlike render calls, sounds are one-shot, no need to keep them afterwords.
        reset();
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
