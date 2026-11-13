package com.lx862.mtrscripting.core.util;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;

public final class ScriptFilePathTest {
    private interface IOAction { void run() throws IOException; }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void rejected(IOAction action, String message) throws IOException {
        try {
            action.run();
        } catch (IOException expected) {
            return;
        }
        throw new AssertionError(message);
    }

    private static void checkLinks(Path base, Path root, Path outside) throws IOException {
        final Path link = root.resolve("outside-link");
        try {
            Files.createSymbolicLink(link, outside);
        } catch (UnsupportedOperationException | FileSystemException unavailable) {
            if (!System.getProperty("os.name").startsWith("Windows")) throw unavailable;
            System.out.println("Symlink cases skipped: this Windows runner does not permit symbolic links; Linux/macOS exercise them");
            return;
        }
        Files.writeString(outside.resolve("existing.txt"), "outside");
        rejected(() -> ScriptFilePath.resolve(root, true, "outside-link", "existing.txt"), "read followed an escaping symlink");
        rejected(() -> ScriptFilePath.resolve(root, true, "outside-link", "new.txt"), "write followed an escaping parent symlink");
        rejected(() -> ScriptFilePath.resolve(root, false, "outside-link", "existing.txt"), "delete followed an escaping parent symlink");

        final Path normalized = ScriptFilePath.resolve(root, true, "outside-link", "..", "local.txt");
        check(normalized.equals(root.resolve("local.txt")), "symlink/.. path was returned unnormalized");
        Files.writeString(normalized, "local");
        check(!Files.exists(base.resolve("local.txt")), "normalized write used filesystem symlink/.. traversal");

        // Deleting the final link must unlink it, never its external target.
        Files.delete(ScriptFilePath.resolve(root, false, "outside-link"));
        check(Files.exists(outside.resolve("existing.txt")), "delete removed a symlink target");
        final Path dangling = root.resolve("dangling");
        Files.createSymbolicLink(dangling, outside.resolve("absent"));
        rejected(() -> ScriptFilePath.resolve(root, true, "dangling"), "dangling escaping leaf was accepted");
        rejected(() -> ScriptFilePath.resolve(root, true, "dangling", "new.txt"), "dangling escaping parent was accepted");
        Files.delete(ScriptFilePath.resolve(root, false, "dangling"));
        check(!Files.exists(dangling, LinkOption.NOFOLLOW_LINKS), "safe deletion failed to remove a dangling link");

        final Path inside = Files.createDirectory(root.resolve("inside"));
        Files.createSymbolicLink(root.resolve("inside-link"), inside);
        final Path internalTarget = ScriptFilePath.resolve(root, true, "inside-link", "new.txt");
        Files.writeString(internalTarget, "inside");
        check(Files.readString(inside.resolve("new.txt")).equals("inside"), "internal links must remain usable");
        Files.createSymbolicLink(base.resolve("root-alias"), root);
        check(ScriptFilePath.resolve(base.resolve("root-alias"), true, "inside", "new.txt").equals(base.resolve("root-alias/inside/new.txt")), "configured root symlink must preserve lexical path");
    }

    public static void main(String[] args) throws IOException {
        final Path base = Files.createTempDirectory("jcm-script-path-").toAbsolutePath().normalize();
        try {
            final Path root = Files.createDirectory(base.resolve("game"));
            final Path outside = Files.createDirectory(base.resolve("outside"));
            check(ScriptFilePath.resolve(Path.of("."), true, "data", "mtrscripting", "new.txt")
                    .equals(Path.of(".").toAbsolutePath().normalize().resolve("data/mtrscripting/new.txt")), "relative Minecraft directory rejected a legal path");
            final Path missingRoot = root.resolve("data/mtrscripting");
            final Path missingTarget = ScriptFilePath.resolve(missingRoot, true, "nested", "new.txt");
            check(missingTarget.equals(missingRoot.resolve("nested/new.txt")), "uncreated data root cannot be resolved");
            Files.createDirectories(missingTarget.getParent());
            Files.writeString(missingTarget, "saved");
            rejected(() -> ScriptFilePath.resolve(root, true, "..", "outside", "file.txt"), "parent traversal was accepted");
            rejected(() -> ScriptFilePath.resolve(root, true, outside.resolve("file.txt").toString()), "absolute outside path was accepted");
            rejected(() -> ScriptFilePath.resolve(root, true, root.resolveSibling("game-other").resolve("file.txt").toString()), "path prefix confused sibling with child");
            checkLinks(base, root, outside);
            System.out.println("Script file paths: relative/missing roots, traversal, symlink confinement, normalized access and safe unlink passed");
        } finally {
            try (var paths = Files.walk(base)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }
}
