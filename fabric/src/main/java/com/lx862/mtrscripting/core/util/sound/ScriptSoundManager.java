package com.lx862.mtrscripting.core.util.sound;

import com.lx862.mtrscripting.core.util.ScriptVector3f;
import org.mtr.mapping.holder.*;

import java.util.ArrayList;
import java.util.List;

public class ScriptSoundManager {
    private List<SoundCall> soundCalls;

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

    public synchronized void invoke(World world, ScriptVector3f basePos) {
        /* synchronized: same cross-thread pair as ScriptRenderManager — the
           JS capture path (executor threads) swaps/appends this manager's
           list while the render thread replays it. */
        /* Snapshot-count loop: soundCall.run never queues new sounds into
         * THIS manager, so the old defensive ArrayList copy per invoke (per
         * scripted block/lift/car per frame, even when the list was empty)
         * bought nothing — removed. */
        final int count = this.soundCalls.size();
        for(int i = 0; i < count; i++) {
            this.soundCalls.get(i).run(world, basePos);
        }
        // Unlike render calls, sounds are one-shot, no need to keep them afterwords.
        reset();
    }

    public ScriptSoundManager copy() {
        return new ScriptSoundManager(new ArrayList<>(this.soundCalls));
    }

    /** Zero-allocation variant of {@link #copy()} for the render-thread
     *  capture step: swaps the backing lists instead of copying them, so the
     *  receiver keeps the queued calls and the source walks away with the
     *  drained list (the caller resets it right after, as before). */
    public synchronized void takeFrom(ScriptSoundManager other) {
        final List<SoundCall> mine = this.soundCalls;
        mine.clear();
        this.soundCalls = other.soundCalls;
        other.soundCalls = mine;
    }

    public synchronized void addCallsFrom(ScriptSoundManager other) {
        this.soundCalls.addAll(other.soundCalls);
    }

    public synchronized void reset() {
        this.soundCalls.clear();
    }

    /** True when no sound call is queued — lets the vehicle capture path
     *  skip the (previously per-frame) empty sound-manager copy entirely. */
    public boolean isEmpty() {
        return this.soundCalls.isEmpty();
    }
}
