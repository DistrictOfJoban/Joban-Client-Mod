package com.lx862.jcm.mod.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mtr.mapping.holder.MinecraftClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.JsonElement;

public class EnquiryLog {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Path LOG_PATH = MinecraftClient.getInstance().getRunDirectoryMapped().toPath().resolve("config").resolve("enquiry_data.json");

    private static String playerName;
    private static String stationName;
    private static long finalFare;
    private static String formattedDateTime;
    private static int balance;

    public EnquiryLog(String playerName, String stationName, long finalFare, String formattedDateTime, int balance) {
        EnquiryLog.playerName = playerName;
        EnquiryLog.stationName = stationName;
        EnquiryLog.finalFare = finalFare;
        EnquiryLog.formattedDateTime = formattedDateTime;
        EnquiryLog.balance = balance;

        saveDataToJson();
    }

    private static void saveDataToJson() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject jsonObject = new JsonObject();

        try {
            if (!Files.exists(LOG_PATH)) {
                Files.createDirectories(LOG_PATH.getParent());
                Files.createFile(LOG_PATH);
            } else {
                String jsonString = Files.readString(LOG_PATH);
                jsonObject = gson.fromJson(jsonString, JsonObject.class);
            }

            JsonArray playerDataArray = jsonObject.getAsJsonArray(playerName);
            if (playerDataArray == null) {
                playerDataArray = new JsonArray();
                jsonObject.add(playerName, playerDataArray);
            }

            JsonObject dataObject = new JsonObject();
            dataObject.addProperty("station", stationName);
            dataObject.addProperty("fare", finalFare);
            dataObject.addProperty("date", formattedDateTime);
            dataObject.addProperty("balance", balance);

            playerDataArray.add(dataObject);

            Files.writeString(LOG_PATH, gson.toJson(jsonObject));
        } catch (IOException e) {
            LOGGER.error("Error saving data to JSON file: {}", e.getMessage());
        }
    }

    public static List<Entry> getEntries(String playerName) {
        List<Entry> entries = new ArrayList<>();
        try {
            if (Files.exists(LOG_PATH)) {
                Gson gson = new Gson();
                String jsonString = Files.readString(LOG_PATH);
                JsonObject jsonObject = gson.fromJson(jsonString, JsonObject.class);
                JsonArray playerDataArray = jsonObject.getAsJsonArray(playerName);
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

    public static int getEntryCount(String playerName) {
        int count = 0;
        try {
            if (Files.exists(LOG_PATH)) {
                Gson gson = new Gson();
                String jsonString = Files.readString(LOG_PATH);
                JsonObject jsonObject = gson.fromJson(jsonString, JsonObject.class);
                JsonArray playerDataArray = jsonObject.getAsJsonArray(playerName);
                if (playerDataArray != null) {
                    count = playerDataArray.size();
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error reading data from JSON file: {}", e.getMessage());
        }
        return count;
    }
}