package com.lx862.mtrscripting.core.api;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.ClassShutter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Limit the usage of arbitrary java class for security reason
 */
@ApiInternal
public class MTRClassShutter implements ClassShutter {
    /** Whether the class shutter is active or not. If false, it will allow all class through (Except itself) as if no restrictions is put in place. */
    private boolean shutterEnabled;
    private final List<ClassRule> allowedScriptClasses = new ArrayList<>();
    private final List<ClassRule> deniedScriptClasses = new ArrayList<>();

    public MTRClassShutter() {
        // Default
        allowClass(
            ClassRule.parse("java.awt.*"),
            ClassRule.parse("java.time.*"),
            ClassRule.parse("java.lang.*"),
            ClassRule.parse("java.math.BigDecimal"),
            ClassRule.parse("java.math.BigInteger"),
            ClassRule.parse("java.util.*"),
            ClassRule.parse("java.text.*"),
            ClassRule.parse("javax.imageio.*"),
            ClassRule.parse("sun.java2d.*"),
            ClassRule.parse("sun.font.*"),
            ClassRule.parse("sun.awt.*"),
            ClassRule.parse("java.io.Closeable"),
            ClassRule.parse("java.io.InputStream"),
            ClassRule.parse("java.io.OutputStream"),
            ClassRule.parse("jdk.*"),
            // Scripting Core
            ClassRule.parse("com.lx862.mtrscripting.core.util.*"),
            ClassRule.parse("com.lx862.mtrscripting.core.integration.*"),
            // Rhino JS Engine
            ClassRule.parse("com.lx862.mtrscripting.lib.org.mozilla.*")
        );
        denyClass(
            ClassRule.parse("java.lang.invoke.*"),
            ClassRule.parse("java.lang.reflect.*"),
            ClassRule.parse("java.lang.Process"),
            ClassRule.parse("java.lang.ProcessBuilder"),
            ClassRule.parse("java.lang.Class"),
            ClassRule.parse("java.lang.ClassLoader"),
            ClassRule.parse("java.lang.Shutdown"),
            ClassRule.parse("java.lang.SecurityManager"),
            ClassRule.parse("java.util.jar.*"),
            ClassRule.parse("java.util.zip.*"),
            ClassRule.parse("jdk.internal.*"),
            ClassRule.parse("jdk.nashorn.*")
        );
    }

    public void setEnabled(boolean shutterEnabled) {
        this.shutterEnabled = shutterEnabled;
    }

    /**
     * By default, MTR Scripting does not allow loading any arbitrary java class for security reasons<br>
     * Here, you can explicitly allow a class to be loaded.<br>
     * It is compared using String.startWith, so this can also be used for allowing a whole package (e.g. java.awt)<br>
     * Please use this wisely instead of blindly allowing classes for your convenience, we don't want a script to affect anything outside of MC.
     */
    public void allowClass(ClassRule... classRules) {
        allowedScriptClasses.addAll(Arrays.asList(classRules));
    }

    public void denyClass(ClassRule... classRules) {
        deniedScriptClasses.addAll(Arrays.asList(classRules));
    }

    @Override
    public boolean visibleToScripts(String fullClassName) {
        return !fullClassName.equals(MTRClassShutter.class.getName()) && isClassAllowed(fullClassName);
    }

    private boolean isClassAllowed(String className) {
        if(!shutterEnabled) return true;

        if(className.startsWith("[")) { // Array
            String type = className.substring(className.lastIndexOf("[")+1);
            if(type.startsWith("B") || type.startsWith("C") || type.startsWith("D") || type.startsWith("F") || type.startsWith("I") || type.startsWith("J") || type.startsWith("S") || type.startsWith("Z")) return true;
            if(type.startsWith("L")) {
                className = type.substring(1, type.length()-1); // Omit the base type & semicolon at the end
            }
        }

        for(ClassRule allowedClassRule : allowedScriptClasses) {
            if(allowedClassRule.match(className)) {
                for(ClassRule deniedClassRule : deniedScriptClasses) {
                    if(deniedClassRule.match(className)) return false;
                }
                return true;
            }
        }
        return false;
    }
}
