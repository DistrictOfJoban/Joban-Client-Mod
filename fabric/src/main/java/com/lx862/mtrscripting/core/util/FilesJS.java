package com.lx862.mtrscripting.core.util;

import org.apache.commons.io.FileUtils;
import org.mtr.libraries.kotlin.text.Charsets;
import org.mtr.mapping.holder.MinecraftClient;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class FilesJS {
    private static final Path rootMinecraftPath = MinecraftClient.getInstance().getRunDirectoryMapped().toPath().toAbsolutePath().normalize();
    private static final Path dataPath = rootMinecraftPath.resolve("data").resolve("mtrscripting");

    private FilesJS() {
    }

    public static DataReaderJS read(String... paths) throws IOException {
        Path p = resolvePathSafe(rootMinecraftPath, paths);
        File f = p.toFile();
        if(!f.exists()) return null;
        return new DataReaderJS(() -> new FileInputStream(resolvePathSafe(rootMinecraftPath, p.toString()).toFile()));
    }

    public static DataReaderJS readData(String... paths) throws IOException {
        Path p = resolvePathSafe(dataPath, paths);
        File f = p.toFile();
        if(!f.exists()) return null;
        return new DataReaderJS(() -> new FileInputStream(resolvePathSafe(dataPath, p.toString()).toFile()));
    }

    public static void saveData(String content, String... paths) throws IOException {
        Path p = resolvePathSafe(dataPath, paths);
        Files.createDirectories(p.getParent());
        FileUtils.writeStringToFile(p.toFile(), content, Charsets.UTF_8);
    }

    public static void saveData(BufferedImage image, String... paths) throws IOException {
        Path p = resolvePathSafe(dataPath, paths);
        Files.createDirectories(p.getParent());
        ImageIO.write(image, "PNG", p.toFile());
    }

    public static void deleteData(String... paths) throws IOException {
        Path p = ScriptFilePath.resolve(dataPath, false, paths);
        Files.deleteIfExists(p);
    }

    public static boolean hasData(String... paths) throws IOException {
        Path p = resolvePathSafe(dataPath, paths);

        File f = p.toFile();
        return f.exists();
    }

    /**
     * Resolve a path while disallow escaping the root path.
     * @param rootPath The path to start resolving in
     * @param paths A series of relative path to resolve
     * @return The resolved path
     * @throws IOException If path is located outside of the specified rootPath
     */
    private static Path resolvePathSafe(Path rootPath, String... paths) throws IOException {
        return ScriptFilePath.resolve(rootPath, true, paths);
    }
}
