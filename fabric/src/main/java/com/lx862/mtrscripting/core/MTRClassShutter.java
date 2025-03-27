package com.lx862.mtrscripting.core;

import com.lx862.mtrscripting.api.ScriptingAPI;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.ClassShutter;

/**
 * Limit the usage of arbitrary java class for security reason
 */
public class MTRClassShutter implements ClassShutter {
    @Override
    public boolean visibleToScripts(String fullClassName) {
        return fullClassName.startsWith("com.lx862.mtrscripting.util") || fullClassName.startsWith("com.lx862.mtrscripting.lib.org.mozilla") || ScriptingAPI.isClassAllowed(fullClassName);
    }
}
