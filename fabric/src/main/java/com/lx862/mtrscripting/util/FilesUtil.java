package com.lx862.mtrscripting.util;

import org.apache.commons.io.FileUtils;
import org.mtr.libraries.kotlin.text.Charsets;
import org.mtr.mapping.holder.MinecraftClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FilesUtil {
    private static final Path rootMinecraftPath = MinecraftClient.getInstance().getRunDirectoryMapped().toPath();
    private static final Path dataPath = rootMinecraftPath.resolve("data").resolve("mtrscripting");

    public static String readData(String prefix, String filename) throws IOException {
        Path p = dataPath.resolve(prefix).resolve(filename);
        ensurePathNotEscaped(p);

        return FileUtils.readFileToString(p.toFile(), Charsets.UTF_8);
    }

    public static void saveData(String prefix, String filename, String content) throws IOException {
        Path p = dataPath.resolve(prefix).resolve(filename);
        ensurePathNotEscaped(p);

        FileUtils.writeStringToFile(p.toFile(), content, Charsets.UTF_8);
    }

    public static void deleteData(String prefix, String filename) throws IOException {
        Path p = dataPath.resolve(prefix).resolve(filename);
        ensurePathNotEscaped(p);

        Files.deleteIfExists(p);
    }

    private static boolean hasFile(String prefix, String filename) throws IOException {
        Path p = dataPath.resolve(prefix).resolve(filename);
        ensurePathNotEscaped(p);

        File f = p.toFile();
        if(!f.exists()) return false;

        return f.isFile();
    }

    private static void ensurePathNotEscaped(Path relPath) throws IOException {
        Path pPath = relPath.normalize();
        while(true) {
            if(pPath == null) {
                throw new IOException("Path must be within the Minecraft root directory!");
            }
            if(pPath.equals(dataPath)) break;
            pPath = pPath.getParent();
        }
    }
}
