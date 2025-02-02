package com.lx862.mtrscripting.scripting.util;

import com.lx862.mtrscripting.scripting.ScriptManager;
import org.mtr.mapping.holder.Identifier;

public class NewModelManager {
    public static NewModel loadModel(Identifier id, boolean flipV) {
        try {
            return new NewModel(id, flipV);
        } catch (Exception e) {
            ScriptManager.LOGGER.error("", e);
            return null;
        }
    }
}
