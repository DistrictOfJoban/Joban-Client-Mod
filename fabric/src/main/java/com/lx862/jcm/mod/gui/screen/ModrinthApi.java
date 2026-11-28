package com.lx862.jcm.mod.gui.screen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lx862.jcm.mod.util.JCMLogger;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.MinecraftClient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * ModrinthApi — minimal read-only client for the Modrinth v2 API backing
 * the in-game script browser (search community script resource packs,
 * one-click download into {@code resourcepacks/}).
 *
 * <p>HTTP runs on its own threads (CompletableFuture + the JDK HttpClient);
 * callbacks are marshalled onto the client thread by the caller. The
 * User-Agent follows Modrinth's guidelines (project name + version).</p>
 */
public final class ModrinthApi {

    public static final String USER_AGENT = "Joban-Client-Mod/2.3 (script browser; github.com/BugQuantumCore/Joban-Client-Mod-Yanyang)";
    private static final String API = "https://api.modrinth.com/v2";
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public record SearchHit(String slug, String title, String author, String description,
                            long downloads, String iconUrl, String projectId) {
    }

    private ModrinthApi() {
    }

    /** Search resource packs matching the query, newest-relevance order. */
    public static CompletableFuture<List<SearchHit>> search(String query, int limit) {
        final String facets = "[[\"project_type:resourcepack\"]]";
        final String url = API + "/search?query=" + encode(query)
                + "&facets=" + encode(facets) + "&limit=" + limit;
        return getJson(url).thenApply(root -> {
            final List<SearchHit> hits = new ArrayList<>();
            final JsonArray array = root.getAsJsonArray("hits");
            for (JsonElement element : array) {
                try {
                    final JsonObject hit = element.getAsJsonObject();
                    hits.add(new SearchHit(
                            string(hit, "slug"),
                            string(hit, "title"),
                            string(hit, "author"),
                            string(hit, "description"),
                            hit.has("downloads") ? hit.get("downloads").getAsLong() : 0,
                            string(hit, "icon_url"),
                            string(hit, "project_id")));
                } catch (RuntimeException ignored) {
                }
            }
            return hits;
        });
    }

    public record VersionFile(String url, String filename, long size) {
    }

    /** The latest version's primary (or first) file of a project. */
    public static CompletableFuture<VersionFile> primaryFile(String projectId) {
        return getJson(API + "/project/" + encode(projectId) + "/version").thenApply(root -> {
            if (!root.isJsonArray() || root.getAsJsonArray().size() == 0) return null;
            final JsonObject version = root.getAsJsonArray().get(0).getAsJsonObject();
            final JsonArray files = version.getAsJsonArray("files");
            JsonObject chosen = null;
            for (JsonElement element : files) {
                final JsonObject file = element.getAsJsonObject();
                if (file.has("primary") && file.get("primary").getAsBoolean()) {
                    chosen = file;
                    break;
                }
            }
            if (chosen == null && files.size() > 0) chosen = files.get(0).getAsJsonObject();
            if (chosen == null) return null;
            return new VersionFile(string(chosen, "url"), string(chosen, "filename"),
                    chosen.has("size") ? chosen.get("size").getAsLong() : 0);
        });
    }

    /** Downloads a file into the game's resourcepacks directory, returning
     *  the saved path. */
    public static CompletableFuture<Path> downloadToResourcePacks(VersionFile file) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final Path packs = MinecraftClient.getInstance().getRunDirectoryMapped().toPath()
                        .resolve("resourcepacks");
                Files.createDirectories(packs);
                final Path target = packs.resolve(file.filename());
                final HttpRequest request = HttpRequest.newBuilder(URI.create(file.url()))
                        .header("User-Agent", USER_AGENT)
                        .GET()
                        .build();
                final HttpResponse<Path> response = HTTP.send(request,
                        HttpResponse.BodyHandlers.ofFile(target));
                if (response.statusCode() / 100 != 2) {
                    throw new IOException("HTTP " + response.statusCode());
                }
                return target;
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage() == null ? e.toString() : e.getMessage(), e);
            }
        });
    }

    /* ------------------------------------------------------------------ */

    private static CompletableFuture<JsonObject> getJson(String url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(15))
                        .GET()
                        .build();
                final HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() / 100 != 2) {
                    throw new IOException("HTTP " + response.statusCode());
                }
                return JsonParser.parseString(response.body()).getAsJsonObject();
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage() == null ? e.toString() : e.getMessage(), e);
            }
        });
    }

    private static String string(JsonObject object, String key) {
        final JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? "" : element.getAsString();
    }

    private static String encode(String raw) {
        return java.net.URLEncoder.encode(raw, java.nio.charset.StandardCharsets.UTF_8);
    }

    /** Marshals a completion onto the client thread (screens update their
     *  widgets from here). */
    public static <T> void thenOnClient(CompletableFuture<T> future, Consumer<T> success, Consumer<String> failure) {
        future.whenComplete((result, error) -> MinecraftClient.getInstance().execute(() -> {
            if (error != null) {
                failure.accept(error.getMessage() == null ? error.toString() : error.getMessage());
            } else {
                success.accept(result);
            }
        }));
    }

    static String formatDownloads(long downloads) {
        if (downloads >= 1_000_000) return String.format("%.1fM", downloads / 1e6);
        if (downloads >= 1_000) return String.format("%.1fk", downloads / 1e3);
        return Long.toString(downloads);
    }

    static String tr(String key, Object... args) {
        return TextUtil.translatable(TextCategory.GUI, key, args).getString();
    }
}
