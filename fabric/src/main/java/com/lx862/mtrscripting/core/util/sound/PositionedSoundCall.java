package com.lx862.mtrscripting.core.util.sound;

import com.lx862.mtrscripting.core.util.ScriptVector3f;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.SoundHelper;

public class PositionedSoundCall extends SoundCall {
    protected final double x;
    protected final double y;
    protected final double z;

    public PositionedSoundCall(Identifier id, double x, double y, double z, float volume, float pitch) {
        super(SoundHelper.createSoundEvent(id), volume, pitch);
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void run(World world, ScriptVector3f basePos) {
        ScriptVector3f finalPos = basePos.copy();
        finalPos.add((float)x, (float)y, (float)z);
        world.playSound(finalPos.x(), finalPos.y(), finalPos.z(), soundEvent, SoundCategory.valueOf(soundCategory), volume, pitch, false);
    }
}
