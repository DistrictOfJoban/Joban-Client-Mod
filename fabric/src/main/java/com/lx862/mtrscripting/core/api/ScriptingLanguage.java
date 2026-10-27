package com.lx862.mtrscripting.core.api;

import com.lx862.mtrscripting.core.ScriptManager;
import com.lx862.mtrscripting.core.primitive.ParsedScript;

public abstract class ScriptingLanguage {
    public static final String RHINO_JS = "js";

    public abstract ParsedScript createParsedScript(ScriptManager scriptManager, String displayName, String contextName, ParsedScript.Configuration configuration);
}
