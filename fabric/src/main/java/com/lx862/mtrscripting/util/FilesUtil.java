package com.lx862.mtrscripting.util;

import org.apache.commons.io.FileUtils;
import org.mtr.libraries.kotlin.text.Charsets;
import org.mtr.mapping.holder.MinecraftClient;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FilesUtil {
    private static final Path rootMinecraftPath = MinecraftClient.getInstance().getRunDirectoryMapped().toPath();
    private static final Path dataPath = rootMinecraftPath.resolve("data").resolve("mtrscripting");

    public static FileEntry read(String... paths) throws IOException {
        Path p = resolvePathSafe(rootMinecraftPath, paths);
        File f = p.toFile();
        if(!f.exists()) return null;
        return new FileEntry(f);
    }

    public static FileEntry readData(String... paths) throws IOException {
        Path p = resolvePathSafe(dataPath, paths);
        File f = p.toFile();
        if(!f.exists()) return null;
        return new FileEntry(f);
    }

    public static void saveData(String content, String... paths) throws IOException {
        Path p = resolvePathSafe(dataPath, paths);
        Files.createDirectories(p);
        FileUtils.writeStringToFile(p.toFile(), content, Charsets.UTF_8);
    }

    public static void saveData(BufferedImage image, String... paths) throws IOException {
        Path p = resolvePathSafe(dataPath, paths);
        Files.createDirectories(p);
        ImageIO.write(image, "PNG", p.toFile());
    }

    public static void deleteData(String... paths) throws IOException {
        Path p = resolvePathSafe(dataPath, paths);
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
        Path p = rootPath;
        for(String str : paths) {
            p = p.resolve(str);
        }
        ensurePathNotEscaped(p, rootPath);
        return p;
    }

    private static void ensurePathNotEscaped(Path targetPath, Path constrainedPath) throws IOException {
        Path pPath = targetPath.normalize();
        while(true) {
            if(pPath == null) {
                throw new IOException(String.format("Path must be within the \"%s\" directory!", targetPath.getFileName().toString()));
            }
            if(pPath.equals(constrainedPath)) break;
            pPath = pPath.getParent();
        }
    }

    public static class FileEntry {
        private final File fileReference;

        public FileEntry(File fileReference) {
            this.fileReference = fileReference;
        }

        public String asString() throws IOException {
            return FileUtils.readFileToString(fileReference, Charsets.UTF_8);
        }

        public BufferedImage asBufferedImage() throws IOException {
            return ImageIO.read(fileReference);
        }

        public byte[] asRawBytes() throws IOException {
            return FileUtils.readFileToByteArray(fileReference);
        }
    }
}
