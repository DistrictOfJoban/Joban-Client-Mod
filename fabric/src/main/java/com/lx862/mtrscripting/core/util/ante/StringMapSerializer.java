/*
MIT License

Copyright (c) 2026 Aphrodite281

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package com.lx862.mtrscripting.core.util.ante;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class StringMapSerializer {

    private static ByteArrayOutputStream serializeIn(Map<String, String> map) throws IOException{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(map.size());

        for (Map.Entry<String, String> entry : map.entrySet()) {
            dos.writeUTF(entry.getKey());
            dos.writeUTF(entry.getValue());
        }

        dos.flush();
        return baos;
    }

    private static Map<String, String> deserializeIn(Map<String, String> map, byte[] bytes) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        DataInputStream dis = new DataInputStream(bais);

        int size = dis.readInt();

        for (int i = 0; i < size; i++) {
            String key = dis.readUTF();
            String value = dis.readUTF();
            map.put(key, value);
        }

        dis.close();
        return map;
    }

    public static byte[] serializeToByteArray(Map<String, String> map) throws IOException {
        return serializeIn(map).toByteArray();
    }

    public static Map<String, String> deserialize(Map<String, String> map, byte[] bytes) throws IOException {
        return deserializeIn(map, bytes);
    }

    public static String serializeToString(Map<String, String> map) throws IOException {
        return serializeIn(map).toString("UTF-8");
    }

    public static Map<String, String> deserialize(String str) throws IOException {
        return deserializeIn(new HashMap<>(), str.getBytes("UTF-8"));
    }
}