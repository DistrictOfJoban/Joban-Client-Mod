package com.lx862.jcm.mod.data.scripting.base;

import com.lx862.jcm.mod.util.JCMLogger;

public class ScriptContext {
    // TODO: Temp
    public void print(Object... objs) {
        StringBuilder sb = new StringBuilder();
        for(Object obj : objs) {
            sb.append(obj.toString());
        }
        JCMLogger.info("[Scripting] {}", sb.toString().trim());
    }
}
