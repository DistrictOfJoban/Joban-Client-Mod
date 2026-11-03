package com.lx862.mtrscripting.mod.impl.mtr.util.ante;

import com.lx862.mtrscripting.core.util.sound.ScriptSoundManager;
import org.mtr.mapping.holder.SoundCategory;

@Deprecated
public class ANTESoundHelperJS {
    private static final ScriptSoundManager dummySoundManager = new ScriptSoundManager();

    public static void play(ANTETickableSoundJS tickableSoundInstance) {
        dummySoundManager.play(tickableSoundInstance.tickableSoundInstanceJS);
    }

    public static void play(ANTETickableSoundJS tickableSoundInstance, int delay) {
        dummySoundManager.play(tickableSoundInstance.tickableSoundInstanceJS, delay);
    }

    public static void stop(ANTETickableSoundJS tickableSoundInstance) {
        dummySoundManager.stop(tickableSoundInstance.tickableSoundInstanceJS);
    }

    public static SoundCategory getSoundSource(String str) {
        return SoundCategory.valueOf(str);
    }
}
