package com.lx862.jcm.mod.scripting;

import org.mtr.core.data.Data;
import org.mtr.core.data.Platform;
import org.mtr.core.data.Siding;
import org.mtr.core.data.SimplifiedRoute;
import org.mtr.core.data.Station;
import org.mtr.core.serializer.JsonReader;
import org.mtr.core.serializer.JsonWriter;
import org.mtr.core.serializer.SerializedDataBase;
import org.mtr.core.tool.Utilities;
import org.mtr.libraries.com.google.gson.JsonArray;
import org.mtr.libraries.com.google.gson.JsonObject;
import org.mtr.mod.client.MinecraftClientData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MTRDatasetHolder {
    public final List<Station> stations = new ArrayList<>();
    public final List<Platform> platforms = new ArrayList<>();
    public final List<SimplifiedRoute> routes = new ArrayList<>();
    public final List<Siding> sidings = new ArrayList<>();

    public final Map<Long, Station> stationIdMap = new HashMap<>();
    public final Map<Long, Platform> platformIdMap = new HashMap<>();
    public final Map<Long, SimplifiedRoute> routeIdMap = new HashMap<>();
    public final Map<Long, Siding> sidingIdMap = new HashMap<>();

    public MTRDatasetHolder() {
    }

    public MTRDatasetHolder(String json) {
        Data instance = MinecraftClientData.getInstance();

        JsonReader jsonReader = new JsonReader(Utilities.parseJson(json));
        jsonReader.iterateReaderArray("stations", () -> {}, reader -> stations.add(new Station(reader, instance)));
        jsonReader.iterateReaderArray("platforms", () -> {}, reader -> platforms.add(new Platform(reader, instance)));
        jsonReader.iterateReaderArray("routes", () -> {}, reader -> routes.add(new SimplifiedRoute(reader)));
        jsonReader.iterateReaderArray("sidings", () -> {}, reader -> sidings.add(new Siding(reader, instance)));
        refreshIdCache();
    }

    private void refreshIdCache() {
        stationIdMap.clear();
        platformIdMap.clear();
        routeIdMap.clear();
        sidingIdMap.clear();

        stations.forEach(e -> stationIdMap.put(e.getId(), e));
        platforms.forEach(e -> platformIdMap.put(e.getId(), e));
        routes.forEach(e -> routeIdMap.put(e.getId(), e));
        sidings.forEach(e -> sidingIdMap.put(e.getId(), e));
    }

    public void addStation(Station station) {
        if(station != null && !this.stations.contains(station)) this.stations.add(station);
    }

    public void addPlatform(Platform platform) {
        if(platform != null && !this.platforms.contains(platform)) this.platforms.add(platform);
    }

    public void addSiding(Siding siding) {
        if(siding != null && !this.sidings.contains(siding)) this.sidings.add(siding);
    }

    public void addRoute(SimplifiedRoute route) {
        if(route != null && !this.routes.contains(route)) this.routes.add(route);
    }

    public String serialize() {
        JsonObject rootObject = new JsonObject();
        JsonArray stationsArray = new JsonArray();
        JsonArray platformsArray = new JsonArray();
        JsonArray routesArray = new JsonArray();
        JsonArray sidingsArray = new JsonArray();

        this.stations.forEach(e -> addJsonObject(e, stationsArray));
        this.platforms.forEach(e -> addJsonObject(e, platformsArray));
        this.routes.forEach(e -> addJsonObject(e, routesArray));
        this.sidings.forEach(e -> addJsonObject(e, sidingsArray));

        rootObject.add("stations", stationsArray);
        rootObject.add("platforms", platformsArray);
        rootObject.add("routes", routesArray);
        rootObject.add("sidings", sidingsArray);

        return rootObject.toString();
    }

    public void addFrom(MTRDatasetHolder other) {
        this.stations.addAll(other.stations);
        this.platforms.addAll(other.platforms);
        this.routes.addAll(other.routes);
        this.sidings.addAll(other.sidings);
        refreshIdCache();
    }

    private void addJsonObject(SerializedDataBase dataBase, JsonArray array) {
        JsonObject jsonObject = new JsonObject();
        JsonWriter jsonWriter = new JsonWriter(jsonObject);
        dataBase.serializeFullData(jsonWriter);
        array.add(jsonObject);
    }

    @Override
    public String toString() {
        return serialize();
    }
}
