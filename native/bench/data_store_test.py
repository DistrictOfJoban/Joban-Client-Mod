"""Compile and run the persistent script data store against real sources.

Compiles the REAL ScriptDataStore / MTRScriptContext / AbstractScriptContext
(and the MinecraftClientWrapper weather helper path is covered by the jvm
content harness) against small stubs for the mapping holder and Rhino
runtime, then exercises the store's full contract: typed getters, JSON
objects/arrays (both Java collections and Rhino Scriptables), persistence
across registry resets, corrupt-file recovery and the capacity guards.
"""
import argparse
import hashlib
import os
import subprocess
import sys
import time
import urllib.request
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from jvm_test import manifold_preprocess  # noqa: E402 — same #if semantics as the gradle build

STUBS = {
    'com/lx862/jcm/mod/util/JCMLogger.java': 'package com.lx862.jcm.mod.util; public class JCMLogger { public static void warn(String s,Object... a) {} }',
    'com/lx862/jcm/mod/config/JCMClientConfig.java': 'package com.lx862.jcm.mod.config; public class JCMClientConfig { public static final JCMClientConfig INSTANCE=new JCMClientConfig(); public final Scripting scripting=new Scripting(); public static class Scripting { public final ConfigValue scriptDebugMode=new ConfigValue(); } public static class ConfigValue { public boolean value() { return false; } } }',
    # ScriptDataStore reads the run directory through the mapping holder.
    'org/mtr/mapping/holder/MinecraftClient.java': 'package org.mtr.mapping.holder; public class MinecraftClient { public static MinecraftClient getInstance(){return new MinecraftClient();} public java.io.File getRunDirectoryMapped(){return new java.io.File(System.getProperty("jcm.test.runDir","run-test"));} }',
    # The relocated Rhino runtime surface ScriptDataStore actually touches.
    'com/lx862/mtrscripting/lib/org/mozilla/javascript/Scriptable.java': 'package com.lx862.mtrscripting.lib.org.mozilla.javascript; public interface Scriptable { Object[] getIds(); Object get(String name, Scriptable start); Object get(int index, Scriptable start); Object NOT_FOUND_MARKER = null; }',
    'com/lx862/mtrscripting/lib/org/mozilla/javascript/Undefined.java': 'package com.lx862.mtrscripting.lib.org.mozilla.javascript; public class Undefined { public static final Undefined instance = new Undefined(); }',
}

# Scriptable.NOT_FOUND is a static field on the interface — declare it on the
# stub via a second pass since Java interfaces allow static fields.
STUBS['com/lx862/mtrscripting/lib/org/mozilla/javascript/Scriptable.java'] = (
    'package com.lx862.mtrscripting.lib.org.mozilla.javascript;\n'
    'public interface Scriptable {\n'
    '  Object NOT_FOUND = new Object();\n'
    '  Object[] getIds();\n'
    '  Object get(String name, Scriptable start);\n'
    '  Object get(int index, Scriptable start);\n'
    '}\n'
)


def fetch_gson(work):
    gson = work / 'gson.jar'
    if gson.exists():
        return gson
    urls = [
        'https://repo.maven.apache.org/maven2/com/google/code/gson/gson/2.11.0/gson-2.11.0.jar',
        'https://repo1.maven.org/maven2/com/google/code/gson/gson/2.11.0/gson-2.11.0.jar',
        'https://maven.aliyun.com/repository/public/com/google/code/gson/gson/2.11.0/gson-2.11.0.jar',
    ]
    for attempt in range(6):
        url = urls[attempt % len(urls)]
        try:
            urllib.request.urlretrieve(url, gson)
            break
        except Exception as e:
            if attempt == 5:
                raise
            print('gson download from %s failed (%s), retrying...' % (url, e))
            time.sleep(2)
    assert hashlib.sha256(gson.read_bytes()).hexdigest() == '57928d6e5a6edeb2abd3770a8f95ba44dce45f3b23b7a9dc2b309c581552a78b'
    return gson


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--build', required=True, type=Path)
    parser.add_argument('--java-home', type=Path)
    args = parser.parse_args()
    repo = Path(__file__).resolve().parents[2]
    work = args.build.resolve() / 'data-store-test'
    work.mkdir(parents=True, exist_ok=True)
    gson = fetch_gson(work)

    sources = []
    for rel, content in STUBS.items():
        f = work / 'src' / rel
        f.parent.mkdir(parents=True, exist_ok=True)
        f.write_text(content, encoding='utf-8')
        sources.append(f)
    base = repo / 'fabric/src/main/java'
    for rel in [
        'com/lx862/mtrscripting/core/annotation/ApiInternal.java',
        'com/lx862/mtrscripting/core/annotation/ValueNullable.java',
        'com/lx862/mtrscripting/core/api/AbstractScriptContext.java',
        'com/lx862/mtrscripting/core/util/ScriptDataStore.java',
        'com/lx862/mtrscripting/mod/impl/mtr/MTRScriptContext.java',
    ]:
        f = work / 'src' / rel
        f.parent.mkdir(parents=True, exist_ok=True)
        f.write_text(manifold_preprocess((base / rel).read_text(encoding='utf-8')), encoding='utf-8')
        sources.append(f)
    test = work / 'src/com/lx862/mtrscripting/core/util/DataStoreTest.java'
    test.parent.mkdir(parents=True, exist_ok=True)
    test.write_text((Path(__file__).with_name('java') / 'DataStoreTest.java').read_text(encoding='utf-8'), encoding='utf-8')
    sources.append(test)

    classes = work / 'classes'
    classes.mkdir(exist_ok=True)
    argfile = work / 'sources.txt'
    argfile.write_text('\n'.join('"' + str(s).replace('\\', '/') + '"' for s in sources), encoding='utf-8')
    java_home = args.java_home or os.environ.get('JAVA_HOME')

    def tool(name):
        return str(Path(java_home) / 'bin' / (name + ('.exe' if os.name == 'nt' else ''))) if java_home else name

    subprocess.run([tool('javac'), '-encoding', 'UTF-8', '-cp', str(gson), '-d', str(classes), '@' + str(argfile)], check=True)
    subprocess.run([tool('java'), '-cp', str(classes) + os.pathsep + str(gson), 'com.lx862.mtrscripting.core.util.DataStoreTest'], check=True)
    print('DATA STORE TEST OK')


if __name__ == '__main__':
    main()
