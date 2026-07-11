package com.lx862.mtrscripting.core.util.sound;

import com.lx862.mtrscripting.core.util.ScriptVector3f;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.SoundHelper;

public class NonPositionedSoundCall extends SoundCall {
    public NonPositionedSoundCall(Identifier id, SoundCategory soundCategory, float volume, float pitch) {
        super(SoundHelper.createSoundEvent(id), soundCategory, volume, pitch);
    }

    @Override
    public void run(World world, ScriptVector3f basePos) {
        MinecraftClient.getInstance().getSoundManager().play(new SoundInstance(new NonPositionedSoundInstance(soundEvent, soundCategory, volume, pitch)));
    }
}
