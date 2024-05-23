package com.lx862.jcm.mod.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lx862.jcm.mod.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.gson.JsonElement;
import org.mtr.mapping.holder.PlayerEntity;
import org.mtr.mapping.holder.WorldSavePath;

public class EnquiryLog {
    private static final Logger LOGGER = LogManager.getLogger();
    private static Path PLAYER_DATA_DIR;
    private static String playerName;
    private static String stationName;
    private static long finalFare;
    private static String formattedDateTime;
    private static int balance;

    public EnquiryLog(PlayerEntity playerEntity, String playerName, String stationName, long finalFare, String formattedDateTime, int balance) {
        EnquiryLog.playerName = playerName;
        EnquiryLog.stationName = stationName;
        EnquiryLog.finalFare = finalFare;
        EnquiryLog.formattedDateTime = formattedDateTime;
        EnquiryLog.balance = balance;

        initializePlayerDataDir(playerEntity);
        saveDataToJson(playerEntity.getUuidAsString());
    }

    private static void initializePlayerDataDir(PlayerEntity playerEntity) {
        PLAYER_DATA_DIR = Objects.requireNonNull(playerEntity.getServer()).getSavePath(WorldSavePath.getRootMapped())
                .resolve(Constants.MOD_ID).resolve("players_data");
    }

    private static void saveDataToJson(String playerUuid) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject jsonObject = new JsonObject();
        Path playerFilePath = PLAYER_DATA_DIR.resolve(playerUuid + ".json");

        try {
            List<String> lines;
            if (!Files.exists(PLAYER_DATA_DIR)) {
                Files.createDirectories(PLAYER_DATA_DIR);
            }
            if (!Files.exists(playerFilePath)) {
                Files.createFile(playerFilePath);
            } else {
                lines = Files.readAllLines(playerFilePath);
                String jsonString = String.join("\n", lines);
                jsonObject = gson.fromJson(jsonString, JsonObject.class);
            }

            JsonArray playerDataArray = jsonObject.getAsJsonArray("entries");
            if (playerDataArray == null) {
                playerDataArray = new JsonArray();
                jsonObject.add("entries", playerDataArray);
            }

            JsonObject dataObject = new JsonObject();
            dataObject.addProperty("station", stationName);
            dataObject.addProperty("fare", finalFare);
            dataObject.addProperty("date", formattedDateTime);
            dataObject.addProperty("balance", balance);

            playerDataArray.add(dataObject);

            Files.write(playerFilePath, gson.toJson(jsonObject).getBytes());
        } catch (IOException e) {
            LOGGER.error("Error saving data to JSON file: {}", e.getMessage());
        }
    }

    public static List<Entry> getEntries(PlayerEntity player, String playerName) {
        initializePlayerDataDir(player);
        Path playerFilePath = PLAYER_DATA_DIR.resolve(player.getUuidAsString() + ".json");

        List<Entry> entries = new ArrayList<>();
        try {
            if (Files.exists(playerFilePath)) {
                Gson gson = new Gson();
                List<String> lines = Files.readAllLines(playerFilePath);
                String jsonString = String.join("\n", lines);
                JsonObject jsonObject = gson.fromJson(jsonString, JsonObject.class);
                JsonArray playerDataArray = jsonObject.getAsJsonArray("entries");
                if (playerDataArray != null) {
                    for (JsonElement element : playerDataArray) {
                        JsonObject dataObject = element.getAsJsonObject();
                        String station = dataObject.get("station").getAsString();
                        long fare = dataObject.get("fare").getAsLong();
                        String date = dataObject.get("date").getAsString();
                        int balance = dataObject.get("balance").getAsInt();
                        entries.add(new Entry(station, fare, date, balance));
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error reading data from JSON file: {}", e.getMessage());
        }
        return entries;
    }
}
