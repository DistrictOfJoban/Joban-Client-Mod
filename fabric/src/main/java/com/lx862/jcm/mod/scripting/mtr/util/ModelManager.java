package com.lx862.jcm.mod.scripting.mtr.util;

import com.lx862.mtrscripting.ScriptManager;
import org.mtr.mapping.holder.Identifier;

public class ModelManager {
    public static ScriptedModel loadModel(Identifier id, boolean flipV) {
        try {
            return new ScriptedModel(id, flipV);
        } catch (Exception e) {
            ScriptManager.LOGGER.error("", e);
            return null;
        }
    }
}
