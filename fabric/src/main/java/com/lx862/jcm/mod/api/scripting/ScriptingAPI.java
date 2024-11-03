package com.lx862.jcm.mod.api.scripting;

import org.apache.logging.log4j.util.TriConsumer;
import vendor.com.lx862.jcm.org.mozilla.javascript.Context;
import vendor.com.lx862.jcm.org.mozilla.javascript.Scriptable;

import java.util.ArrayList;
import java.util.List;

/**
 * This class contains the event to hook into scripting, i.e. add new property and objects
 */
public class ScriptingAPI {
    private static final List<TriConsumer<String, Context, Scriptable>> onParseScriptCallback = new ArrayList<>();

    public static void onParseScript(TriConsumer<String, Context, Scriptable> callback) {
        onParseScriptCallback.add(callback);
    }

    public static void callOnParseScriptCallback(String contextName, Context context, Scriptable scriptable) {
        for(TriConsumer<String, Context, Scriptable> entry : onParseScriptCallback) {
            entry.accept(contextName, context, scriptable);
        }
    }
}
