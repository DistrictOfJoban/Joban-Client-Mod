package com.lx862.jcm.nativeapi;

import com.lx862.jcm.mod.config.JCMClientConfig;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.mapping.holder.MinecraftClient;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * NativeHotReload — the native script hot-reload watcher (ABI 16 era
 * developer feature, {@code scripting.nativeHotReload} config).
 *
 * <p>A low-frequency daemon thread (2 s) fingerprints the game's resource
 * pack directories — {@code resourcepacks/} plus a {@code resources/}
 * dev folder — as a (name, mtime, size) multiset. Any change (a rebuilt
 * {@code .so} copied into the pack, an edited {@code .js}, a pack
 * swapped out wholesale) schedules ONE client-thread script reload,
 * exactly the reload the F3+T path performs: NativeScriptManager.reload()
 * disposes every module instance, closes the modules (mtrCleanup), re-opens
 * them from the refreshed pack files and re-runs mtrInit on the next
 * frame — "改 C++ 不重启游戏". Debounced 1 s so a build script's
 * partial writes (copy → rename) collapse into one reload.</p>
 *
 * <p>Opt-in and off by default: an editor auto-saving an open pack would
 * otherwise trigger reload hitches the player never asked for. No reload
 * happens without loaded native modules OR parsed JS scripts, so a
 * vanilla session stays untouched.</p>
 */
public final class NativeHotReload {

    private static final long POLL_MILLIS = 2000;
    private static final long DEBOUNCE_MILLIS = 1000;

    private static volatile Thread worker;
    private static volatile String lastFingerprint;
    private static volatile long lastChangeAt;
    private static volatile long lastReloadAt;

    private NativeHotReload() {
    }

    /** Starts the watcher if configured (idempotent, client boot / config
     *  save). Cheap no-op when the feature is off. */
    public static synchronized void startIfEnabled() {
        if (!JCMClientConfig.INSTANCE.scripting.nativeHotReload.value()) {
            stop();
            return;
        }
        if (worker != null && worker.isAlive()) return;
        final Path gameDir = MinecraftClient.getInstance().getRunDirectoryMapped().toPath();
        final Thread thread = new Thread(() -> watch(gameDir), "jcm-native-hot-reload");
        thread.setDaemon(true);
        thread.start();
        worker = thread;
        JCMLogger.info("[C++] Hot-reload watcher armed ({}resourcepacks)", gameDir);
    }

    /** Stops the watcher (config turned off). */
    public static synchronized void stop() {
        final Thread thread = worker;
        worker = null;
        if (thread != null) thread.interrupt();
        lastFingerprint = null;
    }

    private static void watch(Path gameDir) {
        final List<Path> roots = new ArrayList<>();
        final Path packs = gameDir.resolve("resourcepacks");
        if (Files.isDirectory(packs)) roots.add(packs);
        /* Dev environment (loom run): the resource pack under test is the
           run/resources directory. Harmless when absent in a normal install. */
        final Path devResources = gameDir.resolve("resources");
        if (Files.isDirectory(devResources)) roots.add(devResources);

        while (worker == Thread.currentThread()) {
            try {
                Thread.sleep(POLL_MILLIS);
            } catch (InterruptedException e) {
                return;
            }
            try {
                final String fingerprint = fingerprint(roots);
                final String previous = lastFingerprint;
                lastFingerprint = fingerprint;
                if (previous == null) continue;             /* first scan: baseline */
                if (fingerprint.equals(previous)) {
                    /* Quiet — settle any in-flight burst, then reload once. */
                    if (lastChangeAt != 0 && System.currentTimeMillis() - lastChangeAt > DEBOUNCE_MILLIS) {
                        lastChangeAt = 0;
                        triggerReload();
                    }
                    continue;
                }
                lastChangeAt = System.currentTimeMillis();
            } catch (Throwable t) {
                /* A pack being swapped can transiently fail the walk — the
                   next poll re-baselines. */
            }
        }
    }

    private static void triggerReload() {
        /* Only reload when there is something to reload. */
        final boolean hasJs = !com.lx862.mtrscripting.mod.impl.mtr.MTRContentScripting.getScriptManager()
                .getInstanceManager().getInstances().isEmpty();
        if (NativeScriptManager.getModules().isEmpty() && !hasJs) {
            return;
        }
        final long now = System.currentTimeMillis();
        if (now - lastReloadAt < POLL_MILLIS) return;
        lastReloadAt = now;
        JCMLogger.info("[C++] Resource pack change detected - reloading native & JS scripts");
        MinecraftClient.getInstance().execute(() -> {
            try {
                com.lx862.jcm.mod.registry.Events.onClientReloadResource();
            } catch (Throwable t) {
                JCMLogger.error("Hot reload failed", t);
            }
        });
    }

    /** (name, mtime, size) multiset over every regular file under the
     *  roots — cheap enough at 0.5 Hz even for large pack folders. */
    private static String fingerprint(List<Path> roots) throws IOException {
        final StringBuilder sb = new StringBuilder();
        for (Path root : roots) {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    sb.append(root.relativize(file)).append('|')
                            .append(attrs.lastModifiedTime().toMillis()).append('|')
                            .append(attrs.size()).append('\n');
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        return sb.toString();
    }
}
