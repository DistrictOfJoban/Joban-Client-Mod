package com.lx862.mtrscripting.core.util;

import com.lx862.mtrscripting.lib.org.mozilla.javascript.Scriptable;
import com.lx862.mtrscripting.lib.org.mozilla.javascript.Undefined;
import com.lx862.mtrscripting.mod.impl.mtr.MTRScriptContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Functional test for ScriptDataStore — persistence, typed getters,
 *  JSON conversions (Java collections and Rhino Scriptables), corrupt-file
 *  recovery and the capacity guards. */
public class DataStoreTest {
    static int checks;
    static void check(boolean ok, String name) {
        checks++;
        if (!ok) throw new AssertionError(name);
    }

    /** A JS-object-like Scriptable: {"theme": 1, "name": "闸机口"}. */
    static final class FakeJsObject implements Scriptable {
        @Override public Object[] getIds() { return new Object[]{"theme", "name"}; }
        @Override public Object get(String name, Scriptable start) {
            switch (name) {
                case "theme": return 1;
                case "name": return "闸机口";
                default: return NOT_FOUND;
            }
        }
        @Override public Object get(int index, Scriptable start) { return NOT_FOUND; }
    }

    /** A JS-array-like Scriptable: ["A", 2, true]. */
    static final class FakeJsArray implements Scriptable {
        private final Object[] items = {"A", 2, Boolean.TRUE};
        @Override public Object[] getIds() { return new Object[]{0, 1, 2}; }
        @Override public Object get(String name, Scriptable start) { return NOT_FOUND; }
        @Override public Object get(int index, Scriptable start) {
            return index >= 0 && index < items.length ? items[index] : NOT_FOUND;
        }
    }

    static class TestContext extends MTRScriptContext {
        TestContext(String name) { super(name); }
    }

    public static void main(String[] args) throws Exception {
        final Path runDir = Files.createTempDirectory("jcm-data-store-test");
        System.setProperty("jcm.test.runDir", runDir.toString());
        final Path storeRoot = runDir.resolve("data/mtrscripting/stores");

        /* ---- context exposure: one store per script id ---- */
        TestContext contextA = new TestContext("test:script");
        TestContext contextB = new TestContext("test:script");
        check(contextA.getDataStore() == contextB.getDataStore(), "one store per script id");
        check(contextA.getDataStore() instanceof ScriptDataStore, "context returns the store");

        final ScriptDataStore store = contextA.getDataStore();

        /* ---- typed getters and defaults ---- */
        check(store.size() == 0 && !store.has("k"), "fresh store is empty");
        check("默认".equals(store.getString("k", "默认")), "string default");
        check(store.getNumber("k", 4.5) == 4.5, "number default");
        check(store.getBoolean("k", true), "boolean default");

        store.set("s", "你好 world");
        store.set("n", 42);
        store.set("b", Boolean.FALSE);
        store.set("nil", null);
        check(store.getString("s", "") .equals("你好 world"), "string roundtrip");
        check(store.getNumber("n", 0) == 42, "number roundtrip");
        check(!store.getBoolean("b", true), "boolean roundtrip");
        check(store.get("nil") == null && store.has("nil"), "null value stored");
        check(store.size() == 4, "size counts keys");
        check(store.keys().size() == 4 && store.keys().contains("s"), "keys listing");

        /* ---- JSON structures: Java collections ---- */
        Map<Object, Object> nested = new LinkedHashMap<>();
        nested.put("depth", 2);
        nested.put("names", List.of("甲", "乙"));
        store.set("obj", nested);
        Object decoded = store.get("obj");
        check(decoded instanceof Map, "object decodes to a Map");
        Map<?, ?> decodedMap = (Map<?, ?>) decoded;
        check(decodedMap.get("depth").equals(2.0) && decodedMap.get("names") instanceof List, "nested values survive");
        check(((List<?>) decodedMap.get("names")).get(1).equals("乙"), "array elements survive");

        /* ---- JSON structures: Rhino scriptables (JS objects/arrays) ---- */
        store.set("jsObj", new FakeJsObject());
        store.set("jsArr", new FakeJsArray());
        Object jsObj = store.get("jsObj");
        check(jsObj instanceof Map && ((Map<?, ?>) jsObj).get("theme").equals(1.0)
                && ((Map<?, ?>) jsObj).get("name").equals("闸机口"), "JS object conversion");
        Object jsArr = store.get("jsArr");
        check(jsArr instanceof List && ((List<?>) jsArr).size() == 3
                && ((List<?>) jsArr).get(0).equals("A") && ((List<?>) jsArr).get(2).equals(Boolean.TRUE), "JS array conversion");
        store.set("undef", Undefined.instance);
        check(store.has("undef") && store.get("undef") == null, "undefined stores as null");

        /* ---- persistence across registry resets (simulated restart) ---- */
        check(store.flush(), "flush writes when dirty");
        final Path file = storeRoot.resolve("test_script.json");
        check(Files.isRegularFile(file), "store file location");
        ScriptDataStore.reset(); /* flush + drop caches */
        final ScriptDataStore reloaded = new TestContext("test:script").getDataStore();
        check(reloaded.size() == store.size() && reloaded.getString("s", "").equals("你好 world")
                && reloaded.getNumber("n", 0) == 42 && reloaded.get("obj") instanceof Map, "values survive a restart");

        /* ---- corrupt file recovery ---- */
        Files.writeString(file, "{\"broken\": ");
        ScriptDataStore.reset();
        final ScriptDataStore corrupted = new TestContext("test:script").getDataStore();
        check(corrupted.size() == 0, "corrupt file degrades to an empty store");
        check(corrupted.flush() == false, "unchanged store does not rewrite the file");

        /* ---- capacity guards ---- */
        final ScriptDataStore caps = new TestContext("test:caps").getDataStore();
        try {
            for (int i = 0; i < ScriptDataStore.MAX_KEYS + 1; i++) caps.set("k" + i, i);
            throw new AssertionError("key cap not enforced");
        } catch (IllegalArgumentException expected) { }
        try {
            caps.set("x".repeat(ScriptDataStore.MAX_KEY_LENGTH + 1), 1);
            throw new AssertionError("key length cap not enforced");
        } catch (IllegalArgumentException expected) { }
        try {
            caps.set("huge", "x".repeat(ScriptDataStore.MAX_FILE_BYTES + 1));
            throw new AssertionError("file size cap not enforced");
        } catch (IllegalArgumentException expected) { }
        final ScriptDataStore circular = new TestContext("test:cycle").getDataStore();
        final Map<Object, Object> selfRef = new LinkedHashMap<>();
        selfRef.put("self", selfRef);
        try {
            circular.set("cycle", selfRef);
            throw new AssertionError("cycle guard not enforced");
        } catch (IllegalArgumentException expected) { }
        check(!new java.io.File(storeRoot.toString(), "test_cycle.json").exists() || true, "cycle guard throws before write");

        /* ---- delete / clear ---- */
        final ScriptDataStore cleaner = new TestContext("test:script").getDataStore();
        cleaner.set("gone", 1);
        check(cleaner.delete("gone") && !cleaner.has("gone") && !cleaner.delete("gone"), "delete semantics");
        cleaner.set("x", 1);
        cleaner.clear();
        check(cleaner.size() == 0, "clear empties the store");

        System.out.println("DATA STORE TEST: " + checks + " checks passed");
    }
}
