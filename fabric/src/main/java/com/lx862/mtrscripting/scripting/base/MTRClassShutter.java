package com.lx862.mtrscripting.scripting.base;

import com.lx862.mtrscripting.api.ScriptingAPI;
import vendor.com.lx862.jcm.org.mozilla.javascript.ClassShutter;

/**
 * Limit the usage of arbitrary java class for security reason
 */
public class MTRClassShutter implements ClassShutter {
    @Override
    public boolean visibleToScripts(String fullClassName) {
        return fullClassName.startsWith("com.lx862.mtrscripting.scripting.util") || fullClassName.startsWith("vendor.com.lx862.jcm.org.mozilla") || ScriptingAPI.isClassAllowed(fullClassName);
    }
}
