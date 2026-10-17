package com.lx862.mtrscripting.mod.impl.mtr;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleWrapper;
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

import org.mtr.core.data.TransportMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApiInternal
public class MTRDatasetHolder {
    public final List<Station> stations = new ArrayList<>();
    public final List<Platform> platforms = new ArrayList<>();
    public final List<SimplifiedRoute> routes = new ArrayList<>();
    public final List<Siding> sidings = new ArrayList<>();
    public final Map<Long, Map<String, List<VehicleWrapper.Stop.RouteInterchange>>> connectingStationInterchangeMap = new HashMap<>();
    public final Map<Long, List<VehicleWrapper.Stop.RouteInterchange>> stationInterchangeMap = new HashMap<>();

    public final Map<Long, Station> stationIdMap = new HashMap<>();
    public final Map<Long, Platform> platformIdMap = new HashMap<>();
    public final Map<Long, SimplifiedRoute> routeIdMap = new HashMap<>();
    public final Map<Long, Siding> sidingIdMap = new HashMap<>();

    /**
     * Monotonic data revision, bumped whenever the holder's contents are
     * regenerated or merged. Caches built on top of this dataset (notably
     * VehicleDataCache's per-vehicle StopsData) compare the revision to detect
     * staleness without re-allocating anything per frame.
     */
    private volatile long revision;

    public MTRDatasetHolder() {
    }

    public long getRevision() {
        return revision;
    }

    public MTRDatasetHolder(String json) {
        Data instance = MinecraftClientData.getInstance();

        JsonReader jsonReader = new JsonReader(Utilities.parseJson(json));
        jsonReader.iterateReaderArray("stations", () -> {}, reader -> stations.add(new Station(reader, instance)));
        jsonReader.iterateReaderArray("stationInterchanges", () -> {}, reader -> {
            List<VehicleWrapper.Stop.RouteInterchange> interchanges = new ArrayList<>();
            reader.iterateReaderArray("interchanges", () -> {}, route -> interchanges.add(
                    new VehicleWrapper.Stop.RouteInterchange(route.getLong("routeId", 0), route.getInt("color", 0),
                            route.getString("name", ""),
                            parseTransportMode(route.getString("transportMode", "")),
                            route.getString("transportType", null))));
            stationInterchangeMap.put(reader.getLong("id", 0), interchanges);
        });
        jsonReader.iterateReaderArray("connectingStationInterchanges", () -> {}, reader -> {
            long stationId = reader.getLong("id", 0);

            Map<String, List<VehicleWrapper.Stop.RouteInterchange>> interchangeForStations = new HashMap<>();
            reader.iterateReaderArray("connectingStations", () -> {}, elementReader -> {
                List<VehicleWrapper.Stop.RouteInterchange> routeInterchanges = new ArrayList<>();
                String stationName = elementReader.getString("name", "");
                elementReader.iterateReaderArray("interchanges", () -> {}, interchangeReader -> {
                    String routeName = interchangeReader.getString("name", "");
                    int color = interchangeReader.getInt("color", 0);
                    VehicleWrapper.Stop.RouteInterchange routeInterchange = new VehicleWrapper.Stop.RouteInterchange(
                            interchangeReader.getLong("routeId", 0), color, routeName,
                            parseTransportMode(interchangeReader.getString("transportMode", "")),
                            interchangeReader.getString("transportType", null));
                    routeInterchanges.add(routeInterchange);
                });

                interchangeForStations.put(stationName, routeInterchanges);
            });

            connectingStationInterchangeMap.put(stationId, interchangeForStations);
        });
        jsonReader.iterateReaderArray("platforms", () -> {}, reader -> platforms.add(new Platform(reader, instance)));
        jsonReader.iterateReaderArray("routes", () -> {}, reader -> routes.add(new SimplifiedRoute(reader)));
        jsonReader.iterateReaderArray("sidings", () -> {}, reader -> sidings.add(new Siding(reader, instance)));
        generateIdMapCache();
    }

    private static TransportMode parseTransportMode(String name) {
        try { return TransportMode.valueOf(name); }
        catch (IllegalArgumentException ex) { return null; }
    }

    public void generateIdMapCache() {
        stationIdMap.clear();
        platformIdMap.clear();
        routeIdMap.clear();
        sidingIdMap.clear();

        stations.forEach(e -> stationIdMap.put(e.getId(), e));
        platforms.forEach(e -> platformIdMap.put(e.getId(), e));
        routes.forEach(e -> routeIdMap.put(e.getId(), e));
        sidings.forEach(e -> sidingIdMap.put(e.getId(), e));
        revision++;
    }

    public void addStation(Station station) {
        if(station != null && !this.stations.contains(station)) {
            this.stations.add(station);

            Map<String, List<VehicleWrapper.Stop.RouteInterchange>> connectingStations = new HashMap<>();
            station.getInterchangeStationToColorToRoutesMap(true).forEach((connectingStation, routeInterchanges) -> {
                List<VehicleWrapper.Stop.RouteInterchange> interchanges = new ArrayList<>();
                routeInterchanges.forEach((color, routes) -> routes.forEach(route ->
                        interchanges.add(new VehicleWrapper.Stop.RouteInterchange(route.getId(), color, route.getName(),
                                route.getTransportMode(), null))));
                if (connectingStation.getId() == station.getId()) stationInterchangeMap.put(station.getId(), interchanges);
                else connectingStations.put(connectingStation.getName(), interchanges);
            });
            if(!connectingStations.isEmpty()) {
                this.connectingStationInterchangeMap.put(station.getId(), connectingStations);
            }
            revision++;
        }
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
        JsonArray connectingStationInterchangeArray = new JsonArray();
        JsonArray stationInterchangeArray = new JsonArray();
        stationInterchangeMap.forEach((id, interchanges) -> {
            JsonObject station = new JsonObject();
            station.addProperty("id", id);
            JsonArray routes = new JsonArray();
            interchanges.forEach(route -> {
                JsonObject entry = new JsonObject();
                entry.addProperty("name", route.name); entry.addProperty("color", route.color);
                if (route.getTransportMode() != null) entry.addProperty("transportMode", route.getTransportMode().name());
                entry.addProperty("transportType", route.getTransportType());
                if (route.getRouteId() != 0) entry.addProperty("routeId", route.getRouteId());
                routes.add(entry);
            });
            station.add("interchanges", routes); stationInterchangeArray.add(station);
        });

        this.stations.forEach(e -> addJsonObject(e, stationsArray));
        this.platforms.forEach(e -> addJsonObject(e, platformsArray));
        this.routes.forEach(e -> addJsonObject(e, routesArray));
        this.sidings.forEach(e -> addJsonObject(e, sidingsArray));

        this.connectingStationInterchangeMap.forEach((stationId, connectingStations) -> {
            JsonObject connectingStationInterchanges = new JsonObject();
            connectingStationInterchanges.addProperty("id", stationId);

            JsonArray connectingStationsArray = new JsonArray();
            connectingStations.forEach((stationName, interchanges) -> {
                JsonObject stationInterchanges = new JsonObject();
                stationInterchanges.addProperty("name", stationName);
                JsonArray interchangeArray = new JsonArray();
                interchanges.forEach(routeInterchange -> {
                    JsonObject interchangeObject = new JsonObject();
                    interchangeObject.addProperty("name", routeInterchange.name);
                    interchangeObject.addProperty("color", routeInterchange.color);
                    if (routeInterchange.getTransportMode() != null) {
                        interchangeObject.addProperty("transportMode", routeInterchange.getTransportMode().name());
                    }
                    interchangeObject.addProperty("transportType", routeInterchange.getTransportType());
                    if (routeInterchange.getRouteId() != 0) {
                        interchangeObject.addProperty("routeId", routeInterchange.getRouteId());
                    }
                    interchangeArray.add(interchangeObject);
                });
                stationInterchanges.add("interchanges", interchangeArray);

                connectingStationsArray.add(stationInterchanges);
            });
            connectingStationInterchanges.add("connectingStations", connectingStationsArray);
            connectingStationInterchangeArray.add(connectingStationInterchanges);
        });

        rootObject.add("stations", stationsArray);
        rootObject.add("platforms", platformsArray);
        rootObject.add("routes", routesArray);
        rootObject.add("sidings", sidingsArray);
        rootObject.add("connectingStationInterchanges", connectingStationInterchangeArray);
        rootObject.add("stationInterchanges", stationInterchangeArray);

        return rootObject.toString();
    }

    public void addFrom(MTRDatasetHolder other) {
        this.stations.addAll(other.stations);
        this.platforms.addAll(other.platforms);
        this.routes.addAll(other.routes);
        this.sidings.addAll(other.sidings);
        this.connectingStationInterchangeMap.putAll(other.connectingStationInterchangeMap);
        this.stationInterchangeMap.putAll(other.stationInterchangeMap);
        generateIdMapCache();
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
