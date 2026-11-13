"""Run complete production script lifecycle classes against narrow Rhino stubs.

No lifecycle implementation is copied or extracted. RhinoParsedScript,
ParsedScript, ScriptInstance, ScriptInstanceManager, AbstractScriptContext and
TimingJS are compiled directly from fabric/src/main/java. The stubs replace only
Minecraft, logging, ScriptManager's executor plumbing and the Rhino interpreter
surface. Tests inject Function callbacks into the real parsed-script lists and
use a real single-thread executor plus latches to exercise ordering. This tests
host lifecycle semantics, not JavaScript parsing or Rhino language behavior.

Intended for GitHub Actions/CTest; do not run locally during the audit task.
"""
import argparse
import os
from pathlib import Path
import subprocess


STUBS = {
    'org/mtr/mapping/holder/Identifier.java': '''package org.mtr.mapping.holder;
public record Identifier(String namespace, String path) {
    public String getNamespace() { return namespace; }
    public String getPath() { return path; }
}''',
    'org/mtr/mapping/holder/MinecraftClient.java': '''package org.mtr.mapping.holder;
public class MinecraftClient {
    private static final MinecraftClient INSTANCE = new MinecraftClient();
    public static MinecraftClient getInstance() { return INSTANCE; }
    public boolean isPaused() { return false; }
}''',
    'com/lx862/mtrscripting/mod/MTRScriptingMod.java': '''package com.lx862.mtrscripting.mod;
public class MTRScriptingMod { public static final String VERSION = "lifecycle-test"; }''',
    'com/lx862/mtrscripting/core/api/MTRClassShutter.java': '''package com.lx862.mtrscripting.core.api;
public class MTRClassShutter { public boolean visibleToScripts(String name) { return true; } }''',
    'com/lx862/mtrscripting/core/ScriptManager.java': '''package com.lx862.mtrscripting.core;
import com.lx862.mtrscripting.core.api.MTRClassShutter;
import com.lx862.mtrscripting.core.primitive.ParsedScript;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
public class ScriptManager {
    private final ExecutorService executor;
    private final Log logger = new Log();
    public final Event parseScriptEvent = new Event();
    public final AtomicInteger submissions = new AtomicInteger();
    public ScriptManager(ExecutorService executor) { this.executor = executor; }
    public ExecutorService getDesignatedScriptExecutor() { return executor; }
    public Future<?> submitScriptTask(ExecutorService target, Runnable task) {
        submissions.incrementAndGet();
        return target.submit(task);
    }
    public MTRClassShutter getClassShutter() { return new MTRClassShutter(); }
    public Log getLogger() { return logger; }
    public static class Event {
        public void invoke(Consumer<BiConsumer<String, ParsedScript>> action) { }
    }
    public static class Log {
        public final AtomicInteger errors = new AtomicInteger();
        public final AtomicInteger fatals = new AtomicInteger();
        public void info(String message, Object... args) { }
        public void warn(String message, Object... args) { }
        public void error(String message, Object... args) { errors.incrementAndGet(); }
        public void fatal(String message, Object... args) { fatals.incrementAndGet(); }
    }
}''',
    'com/lx862/mtrscripting/core/util/DataReaderJS.java': '''package com.lx862.mtrscripting.core.util;
public class DataReaderJS { public static final int CACHE_TIMEOUT_TICK = 20; }''',
    'com/lx862/mtrscripting/core/util/ScriptResourceUtil.java': '''package com.lx862.mtrscripting.core.util;
import com.lx862.mtrscripting.core.primitive.ParsedScript;
import org.mtr.mapping.holder.Identifier;
public class ScriptResourceUtil {
    public static void beginParseScript(ParsedScript script) { }
    public static void finishParseScript() { }
    public static void executeScript(ParsedScript script, Identifier id, String source) { }
    public static void resetCache() { }
}''',
    'com/lx862/mtrscripting/lib/org/mozilla/javascript/ClassShutter.java': '''package com.lx862.mtrscripting.lib.org.mozilla.javascript;
public interface ClassShutter { boolean visibleToScripts(String name); }''',
    'com/lx862/mtrscripting/lib/org/mozilla/javascript/Scriptable.java': '''package com.lx862.mtrscripting.lib.org.mozilla.javascript;
public interface Scriptable {
    Object NOT_FOUND = new Object();
    Object get(String name, Scriptable start);
    void put(String name, Scriptable start, Object value);
    void delete(String name);
}''',
    'com/lx862/mtrscripting/lib/org/mozilla/javascript/ImporterTopLevel.java': '''package com.lx862.mtrscripting.lib.org.mozilla.javascript;
import java.util.HashMap;
public class ImporterTopLevel implements Scriptable {
    private final HashMap<String, Object> values = new HashMap<>();
    public ImporterTopLevel(Context context) { }
    public Object get(String name, Scriptable start) { return values.getOrDefault(name, NOT_FOUND); }
    public void put(String name, Scriptable start, Object value) { values.put(name, value); }
    public void delete(String name) { values.remove(name); }
}''',
    'com/lx862/mtrscripting/lib/org/mozilla/javascript/Context.java': '''package com.lx862.mtrscripting.lib.org.mozilla.javascript;
import java.util.ArrayDeque;
public class Context {
    public static final int VERSION_ES6 = 200;
    private static final ThreadLocal<ArrayDeque<Context>> STACK = ThreadLocal.withInitial(ArrayDeque::new);
    public static Context enter() { Context value = new Context(); STACK.get().push(value); return value; }
    public static void exit() {
        if (STACK.get().isEmpty()) throw new IllegalStateException("unbalanced Context.exit");
        STACK.get().pop();
    }
    public static Context getCurrentContext() { return STACK.get().peek(); }
    public void setLanguageVersion(int value) { }
    public void setClassShutter(ClassShutter shutter) { }
    public Script compileString(String source, String name, int line, Object securityDomain) {
        return (context, scope) -> null;
    }
    public Scriptable newObject(Scriptable scope) { return new ImporterTopLevel(this); }
}''',
    'com/lx862/mtrscripting/lib/org/mozilla/javascript/Function.java': '''package com.lx862.mtrscripting.lib.org.mozilla.javascript;
@FunctionalInterface public interface Function {
    Object call(Context context, Scriptable scope, Scriptable self, Object[] arguments);
}''',
    'com/lx862/mtrscripting/lib/org/mozilla/javascript/Script.java': '''package com.lx862.mtrscripting.lib.org.mozilla.javascript;
@FunctionalInterface public interface Script { Object exec(Context context, Scriptable scope); }''',
    'com/lx862/mtrscripting/lib/org/mozilla/javascript/RhinoException.java': '''package com.lx862.mtrscripting.lib.org.mozilla.javascript;
public class RhinoException extends RuntimeException {
    public RhinoException(String message) { super(message); }
    public String getScriptStackTrace() { return "fixture.js:1"; }
}''',
    'com/lx862/mtrscripting/lib/org/mozilla/javascript/NativeJavaMethod.java': '''package com.lx862.mtrscripting.lib.org.mozilla.javascript;
public class NativeJavaMethod { public NativeJavaMethod(java.lang.reflect.Method method, String name) { } }''',
    'com/lx862/mtrscripting/lib/org/mozilla/javascript/NativeJavaClass.java': '''package com.lx862.mtrscripting.lib.org.mozilla.javascript;
public class NativeJavaClass { public NativeJavaClass(Scriptable scope, Class<?> type) { } }''',
    'com/lx862/mtrscripting/lib/org/mozilla/javascript/NativeJavaObject.java': '''package com.lx862.mtrscripting.lib.org.mozilla.javascript;
public class NativeJavaObject { public NativeJavaObject(Scriptable scope, Object value, Object type) { } }''',
    'com/lx862/mtrscripting/lib/org/mozilla/javascript/lc/type/TypeInfo.java': '''package com.lx862.mtrscripting.lib.org.mozilla.javascript.lc.type;
public class TypeInfo { public static final Object OBJECT = new Object(); }''',
}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--build', required=True, type=Path)
    parser.add_argument('--java-home', type=Path)
    args = parser.parse_args()
    repo = Path(__file__).resolve().parents[2]
    work = args.build.resolve() / 'script-lifecycle-test'
    work.mkdir(parents=True, exist_ok=True)
    sources = []
    for name, content in STUBS.items():
        target = work / 'src' / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(content, encoding='utf-8')
        sources.append(target)
    base = repo / 'fabric/src/main/java'
    sources += [base / path for path in [
        'com/lx862/mtrscripting/core/annotation/ApiInternal.java',
        'com/lx862/mtrscripting/core/annotation/ValueNullable.java',
        'com/lx862/mtrscripting/core/api/AbstractScriptContext.java',
        'com/lx862/mtrscripting/core/primitive/UniqueKey.java',
        'com/lx862/mtrscripting/core/primitive/ScriptContent.java',
        'com/lx862/mtrscripting/core/primitive/ScriptInstance.java',
        'com/lx862/mtrscripting/core/primitive/ParsedScript.java',
        'com/lx862/mtrscripting/core/ScriptInstanceManager.java',
        'com/lx862/mtrscripting/core/util/TimingJS.java',
        'com/lx862/mtrscripting/core/rhinojs/MTRRhinoClassShutter.java',
        'com/lx862/mtrscripting/core/rhinojs/RhinoParsedScript.java',
    ]]
    sources.append(repo / 'native/bench/java/ScriptLifecycleTest.java')
    classes = work / 'classes'
    classes.mkdir(exist_ok=True)
    argfile = work / 'sources.txt'
    argfile.write_text('\n'.join('"' + path.as_posix() + '"' for path in sources), encoding='utf-8')
    java_home = args.java_home or os.environ.get('JAVA_HOME')

    def tool(name):
        return str(Path(java_home) / 'bin' / (name + ('.exe' if os.name == 'nt' else ''))) if java_home else name

    subprocess.run([tool('javac'), '-J-Dfile.encoding=UTF-8', '--release', '17', '-encoding', 'UTF-8',
                    '-d', str(classes), '@' + str(argfile)], check=True)
    subprocess.run([tool('java'), '-cp', str(classes), 'ScriptLifecycleTest'], check=True, timeout=60)


if __name__ == '__main__':
    main()
