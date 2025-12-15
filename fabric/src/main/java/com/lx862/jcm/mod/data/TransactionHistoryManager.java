package com.lx862.jcm.mod.data;

import com.google.gson.*;
import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.mapping.holder.MinecraftServer;
import org.mtr.mapping.holder.PlayerEntity;
import org.mtr.mapping.holder.WorldSavePath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TransactionHistoryManager {
    public static final int MAX_ENTRY_LIMIT = 50;
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final JsonParser jsonParser = new JsonParser();

    public static void appendEntry(PlayerEntity player, TransactionEntry newEntry) {
        List<TransactionEntry> newEntries = getLogs(player);
        newEntries.add(newEntry);
        saveLogs(newEntries, player);
    }

    private static void saveLogs(List<TransactionEntry> entries, PlayerEntity player) {
        try {
            Path savePath = getSavePath(player.getServer(), player.getUuidAsString());
            savePath.getParent().toFile().mkdirs();

            Collections.sort(entries, (a, b) -> (int)(b.time() - a.time()));

            while(entries.size() > MAX_ENTRY_LIMIT) { // Clamp
                entries.remove(0);
            }

            // Write to json
            JsonObject jsonObject = new JsonObject();
            JsonArray entryArray = new JsonArray();
            for(TransactionEntry entry : entries) {
                JsonObject entryJson = toJson(entry);
                entryArray.add(entryJson);
            }
            jsonObject.add("entries", entryArray);

            Files.write(savePath, gson.toJson(jsonObject).getBytes());
        } catch (Exception e) {
            JCMLogger.error("Error saving transaction record to JSON file: {}", e.getMessage());
        }
    }

    public static List<TransactionEntry> getLogs(PlayerEntity player) {
        Path savePath = getSavePath(player.getServer(), player.getUuidAsString());
        savePath.getParent().toFile().mkdirs();

        List<TransactionEntry> entries = new ArrayList<>();
        try {
            if (Files.exists(savePath)) {
                JsonObject jsonObject = jsonParser.parse(String.join("", Files.readAllLines(savePath))).getAsJsonObject();
                JsonArray entryArray = jsonObject.getAsJsonArray("entries");
                for (JsonElement element : entryArray) {
                    entries.add(fromJson(element.getAsJsonObject()));
                }
            }
        } catch (Exception e) {
            JCMLogger.error("Failed to read transaction record from JSON file!", e);
            saveLogs(new ArrayList<>(), player); // Wipe the entry
        }
        Collections.sort(entries, (a, b) -> (int)(b.time() - a.time()));
        return entries;
    }

    public static Path getSavePath(MinecraftServer server, String playerUuid) {
        Path saveDirectory = server.getSavePath(WorldSavePath.getRootMapped()).resolve(Constants.MOD_ID).resolve("player_data");
        saveDirectory.toFile().mkdirs();
        return saveDirectory.resolve(playerUuid + ".json");
    }

    private static TransactionEntry fromJson(JsonObject jsonObject) {
        return new TransactionEntry(
                jsonObject.get("source").getAsString(),
                jsonObject.get("amount").getAsInt(),
                jsonObject.get("time").getAsLong()
        );
    }

    private static JsonObject toJson(TransactionEntry entry) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("source", entry.source());
        jsonObject.addProperty("amount", entry.amount());
        jsonObject.addProperty("time", entry.time());
        return jsonObject;
    }
}
