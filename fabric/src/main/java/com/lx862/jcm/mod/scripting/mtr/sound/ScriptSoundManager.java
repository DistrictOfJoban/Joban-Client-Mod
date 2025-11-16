package com.lx862.jcm.mod.scripting.mtr.sound;

import com.lx862.mtrscripting.api.ScriptResultCall;
import com.lx862.mtrscripting.util.ScriptVector3f;
import org.mtr.mapping.holder.Direction;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mod.render.StoredMatrixTransformations;

import java.util.ArrayList;
import java.util.List;

public class ScriptSoundManager {
    public final List<ScriptResultCall> soundCalls;

    public ScriptSoundManager() {
        this.soundCalls = new ArrayList<>();
    }

    private ScriptSoundManager(List<ScriptResultCall> soundCalls) {
        this.soundCalls = new ArrayList<>();
        this.soundCalls.addAll(soundCalls);
    }

    public void playLocalSound(Identifier id, float volume, float pitch) {
        this.soundCalls.add(new NonPositionedSoundCall(id, volume, pitch));
    }

    public void playSound(Identifier id, ScriptVector3f pos, float volume, float pitch) {
        this.soundCalls.add(new PositionedSoundCall(id, pos.x(), pos.y(), pos.z(), volume, pitch));
    }

    public void invoke(World world, ScriptVector3f basePos, GraphicsHolder graphicsHolder, StoredMatrixTransformations storedMatrixTransformations, Direction facing, int light) {
        for(ScriptResultCall soundCall : new ArrayList<>(soundCalls)) {
            soundCall.run(world, basePos, graphicsHolder, storedMatrixTransformations, facing, light);
        }
    }

    public ScriptSoundManager copy() {
        return new ScriptSoundManager(this.soundCalls);
    }

    public void reset() {
        this.soundCalls.clear();
    }
}
