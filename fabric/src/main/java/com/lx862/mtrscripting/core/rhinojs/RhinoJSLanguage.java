package com.lx862.mtrscripting.core.rhinojs;

import com.lx862.mtrscripting.core.ScriptManager;
import com.lx862.mtrscripting.core.api.ScriptingLanguage;
import com.lx862.mtrscripting.core.primitive.ParsedScript;

public class RhinoJSLanguage extends ScriptingLanguage {
    @Override
    public ParsedScript createParsedScript(ScriptManager scriptManager, String displayName, String contextName, ParsedScript.Configuration configuration) {
        return new RhinoParsedScript(scriptManager, displayName, contextName, configuration);
    }
}
