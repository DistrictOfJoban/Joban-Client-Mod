package com.lx862.mtrscripting.core;

import com.lx862.mtrscripting.api.ClassRule;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.ClassShutter;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;

import java.util.Arrays;

/**
 * Limit the usage of arbitrary java class for security reason
 */
public class MTRClassShutter implements ClassShutter {
    private final ObjectList<ClassRule> allowedScriptClasses = new ObjectArrayList<>();
    private final ObjectList<ClassRule> deniedScriptClasses = new ObjectArrayList<>();

    public MTRClassShutter() {
        // Default
        allowClass(
            ClassRule.parse("java.awt.*"),
            ClassRule.parse("java.time.*"),
            ClassRule.parse("java.lang.*"),
            ClassRule.parse("java.math.*"),
            ClassRule.parse("java.util.*"),
            ClassRule.parse("javax.imageio.*"),
            ClassRule.parse("sun.java2d.*"),
            ClassRule.parse("sun.font.*"),
            ClassRule.parse("java.io.Closeable"),
            ClassRule.parse("java.io.InputStream"),
            ClassRule.parse("java.io.OutputStream"),
            ClassRule.parse("jdk.*")
        );
        denyClass(
            ClassRule.parse("java.lang.reflect.*"),
            ClassRule.parse("jdk.internal.*")
        );
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
        return fullClassName.startsWith("com.lx862.mtrscripting.util") || fullClassName.startsWith("com.lx862.mtrscripting.lib.org.mozilla") || isClassAllowed(fullClassName);
    }

    private boolean isClassAllowed(String str) {
        for(ClassRule cs : deniedScriptClasses) {
            if(cs.match(str)) return false;
        }
        for(ClassRule cs : allowedScriptClasses) {
            if(cs.match(str)) return true;
        }
        return false;
    }
}
