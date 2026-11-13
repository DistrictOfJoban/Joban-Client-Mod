package com.lx862.mtrscripting.core.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/** Path confinement for script files; does not provide atomic TOCTOU protection. */
final class ScriptFilePath {
    private ScriptFilePath() {
    }

    /**
     * Resolve relative to an absolute, normalized root, following existing
     * ancestors to ensure symbolic links stay within its canonical directory.
     * For deletion the final link is not followed: deleting a link removes the
     * directory entry, not its target, while linked parent directories must
     * still remain inside the root.
     */
    static Path resolve(Path rootPath, boolean followFinalLink, String... paths) throws IOException {
        final Path root = rootPath.toAbsolutePath().normalize();
        Path target = root;
        try {
            for (String path : paths) target = target.resolve(path);
            target = target.normalize();
        } catch (InvalidPathException invalid) {
            throw new IOException("Invalid script file path", invalid);
        }
        if (!target.startsWith(root)) throw outsideRoot(root);

        final Path canonicalRoot = realPathWithMissingTail(root);
        final Path checkedPath = followFinalLink || target.equals(root) ? target : target.getParent();
        if (!realPathWithMissingTail(checkedPath).startsWith(canonicalRoot)) throw outsideRoot(root);
        // Return the normalized lexical path, not toRealPath(): delete must
        // retain its unlink semantics, and symlink/.. must not be reinterpreted.
        return target;
    }

    /** Existing roots/ancestors are canonicalized; not-yet-created tails are retained. */
    private static Path realPathWithMissingTail(Path path) throws IOException {
        Path existing = path;
        while (!Files.exists(existing, LinkOption.NOFOLLOW_LINKS)) {
            existing = existing.getParent();
            if (existing == null) throw new IOException("No accessible ancestor for script file path: " + path);
        }
        // NOFOLLOW_LINKS above deliberately detects dangling links. toRealPath
        // then fails closed instead of treating such links as new directories.
        return existing.toRealPath().resolve(existing.relativize(path)).normalize();
    }

    private static IOException outsideRoot(Path root) {
        return new IOException("Path must stay within the \"" + root + "\" directory");
    }
}
