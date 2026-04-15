package com.lx862.mtrscripting.util.sound;

import com.lx862.mtrscripting.util.ScriptVector3f;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.SoundHelper;

public class NonPositionedSoundCall extends SoundCall {
    public NonPositionedSoundCall(Identifier id, float volume, float pitch) {
        super(SoundHelper.createSoundEvent(id), volume, pitch);
    }

    @Override
    public void run(World world, ScriptVector3f basePos) {
        MinecraftClient.getInstance().getSoundManager().play(new SoundInstance(new NonPositionedSoundInstance(soundEvent, SoundCategory.valueOf(soundCategory), volume, pitch)));
    }
}
