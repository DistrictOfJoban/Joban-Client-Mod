/*
MIT License

Copyright (c) 2026 Aphrodite281

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package com.lx862.mtrscripting.mod.impl.mtr.util.ante;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Collection;

public class ANTEGlobalRegisterJS {

    private static final Map<String, Object> register = new HashMap<>();

    public static void put(String key, Object value) {
        synchronized (register) {
            register.put(key, value);
        }
    }

    public static Object get(String key) {
        synchronized (register) {
            return register.get(key);
        }
    }

    public static boolean containsKey(String key) {
        synchronized (register) {
            return register.containsKey(key);
        }
    }

    public static boolean containsValue(Object value) {
        synchronized (register) {
            return register.containsValue(value);
        }
    }

    public static Set<String> keySet() {
        synchronized (register) {
            return register.keySet();
        }
    }

    public static Collection<Object> values() {
        synchronized (register) {
            return register.values();
        }
    }

    public static void clear() {
        synchronized (register) {
            register.clear();
        }
    }

    public static int size() {
        synchronized (register) {
            return register.size();
        }
    }

    public static void remove(String key) {
        synchronized (register) {
            register.remove(key);
        }
    }

    public static Set<Map.Entry<String, Object>> entrySet() {
        synchronized (register) {
            return register.entrySet();
        }
    }

}