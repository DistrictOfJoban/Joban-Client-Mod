package com.lx862.jcm.mod.scripting.mtr.sound;

import com.lx862.mtrscripting.util.ScriptVector3f;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.SoundHelper;
import org.mtr.mod.render.StoredMatrixTransformations;

public class NonPositionedSoundCall extends SoundCall {
    public NonPositionedSoundCall(Identifier id, float volume, float pitch) {
        super(SoundHelper.createSoundEvent(id), volume, pitch);
    }

    @Override
    public void run(World world, ScriptVector3f basePos, GraphicsHolder graphicsHolder, StoredMatrixTransformations storedMatrixTransformations, Direction facing, int light) {
        MinecraftClient.getInstance().getPlayerMapped().playSound(soundEvent, SoundCategory.valueOf(soundCategory), volume, pitch);
    }
}
