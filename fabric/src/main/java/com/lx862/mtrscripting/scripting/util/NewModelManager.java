package com.lx862.mtrscripting.scripting.util;

import org.mtr.mapping.holder.Identifier;

public class NewModelManager {
    public static NewModel loadModel(Identifier id, boolean flipV) {
        return new NewModel(id, flipV);
    }
}
