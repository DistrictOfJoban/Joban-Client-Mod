package com.lx862.jcm.mod.data;

import com.google.gson.*;
import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.mapping.holder.PlayerEntity;
import org.mtr.mapping.holder.WorldSavePath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TransactionLog {
    public static final int MAX_ENTRY_LIMIT = 50;

    public static void writeLog(PlayerEntity player, TransactionEntry entry) {
        Path savePath = getSavePath(player);
        savePath.getParent().toFile().mkdirs();

        JsonObject jsonObject = new JsonObject();
        JsonArray entryArray = new JsonArray();

        try {
            if (Files.exists(savePath)) {
                jsonObject = new JsonParser().parse(String.join("", Files.readAllLines(savePath))).getAsJsonObject();
                entryArray = jsonObject.getAsJsonArray("entries");
            }

            entryArray.add(entry.toJson());
            if(entryArray.size() > MAX_ENTRY_LIMIT) entryArray.remove(0);
            jsonObject.add("entries", entryArray);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.write(savePath, gson.toJson(jsonObject).getBytes());
        } catch (IOException e) {
            JCMLogger.error("Error saving data to JSON file: {}", e.getMessage());
        }
    }

    public static List<TransactionEntry> readLog(PlayerEntity player, String playerName) {
        Path savePath = getSavePath(player);
        savePath.getParent().toFile().mkdirs();

        List<TransactionEntry> entries = new ArrayList<>();
        try {
            if (Files.exists(savePath)) {
                JsonObject jsonObject = new JsonParser().parse(String.join("", Files.readAllLines(savePath))).getAsJsonObject();
                JsonArray playerDataArray = jsonObject.getAsJsonArray("entries");
                for (JsonElement element : playerDataArray) {
                    entries.add(TransactionEntry.fromJson(element.getAsJsonObject()));
                }
            }
        } catch (IOException e) {
            JCMLogger.error("Error reading data from JSON file: {}", e.getMessage());
        }
        return entries;
    }

    public static Path getSavePath(PlayerEntity player) {
        Path saveDirectory = player.getServer().getSavePath(WorldSavePath.getRootMapped()).resolve(Constants.MOD_ID).resolve("player_data");
        saveDirectory.toFile().mkdirs();
        return saveDirectory.resolve(player.getUuidAsString() + ".json");
    }
}
