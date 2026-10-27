package com.lx862.mtrscripting.core.rhinojs;

import com.lx862.mtrscripting.core.api.MTRClassShutter;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.ClassShutter;

public class MTRRhinoClassShutter implements ClassShutter {
    private final MTRClassShutter classRestrictor;

    public MTRRhinoClassShutter(MTRClassShutter classShutter) {
        this.classRestrictor = classShutter;
    }

    @Override
    public boolean visibleToScripts(String fullClassName) {
        return classRestrictor.visibleToScripts(fullClassName);
    }
}
