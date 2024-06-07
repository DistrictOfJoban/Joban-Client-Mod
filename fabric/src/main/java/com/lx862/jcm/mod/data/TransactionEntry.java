package com.lx862.jcm.mod.data;

import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;

public class TransactionEntry {
    public static final SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    public final String source;
    public final long time;
    public final long amount;

    public TransactionEntry(String source, long amount, long time) {
        this.source = source;
        this.amount = amount;
        this.time = time;
    }

    public String getFormattedDate() {
        return formatter.format(time);
    }

    public static TransactionEntry fromJson(JsonObject jsonObject) {
        return new TransactionEntry(
                jsonObject.get("source").getAsString(),
                jsonObject.get("amount").getAsInt(),
                jsonObject.get("time").getAsLong()
        );
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("source", source);
        jsonObject.addProperty("amount", amount);
        jsonObject.addProperty("time", time);
        return jsonObject;
    }
}