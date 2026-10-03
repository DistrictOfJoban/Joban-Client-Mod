package com.lx862.mtrscripting.mod.impl.mtr.util;

import com.lx862.mtrscripting.core.annotation.ValueNullable;
import com.lx862.mtrscripting.core.util.ScriptVector3f;
import com.lx862.mtrscripting.mod.impl.mtr.pids.ArrivalsWrapper;
import org.mtr.core.data.*;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongImmutableList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mod.InitClient;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.client.VehicleRidingMovement;
import org.mtr.mod.config.Config;
import org.mtr.mod.data.ArrivalsCacheClient;
import org.mtr.mod.data.VehicleExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MTRWrapper {

    public static class ClientConfig {
        public static boolean isChatAnnouncementsEnabled() {
            return Config.getClient().getChatAnnouncements();
        }

        public static boolean isTtsAnnouncementsEnabled() {
            return Config.getClient().getTextToSpeechAnnouncements();
        }

        public static boolean shouldHideTranslucentParts() {
            return Config.getClient().getHideTranslucentParts();
        }

        public static String getLanguageDisplay() {
            return Config.getClient().getLanguageDisplay().name();
        }

        public static int getDynamicTextureResolution() {
            return Config.getClient().getDynamicTextureResolution();
        }

        public static double getVehicleOscillationMultiplier() {
            return Config.getClient().getVehicleOscillationMultiplier();
        }

        public static boolean getDefaultRail3D() {
            return Config.getClient().getDefaultRail3D();
        }

        public static boolean isCustomFontEnabled() {
            return Config.getClient().getUseMTRFont();
        }
    }

    public static class Data {
        /* Station */
        public static @ValueNullable Station findStation(ScriptVector3f pos) {
            return InitClient.findStation(pos.rawBlockPos());
        }

        public static @ValueNullable Station getStation(long stationId) {
            return MinecraftClientData.getInstance().stationIdMap.get(stationId);
        }

        public static List<Station> getKnownStations() {
            return new ArrayList<>(MinecraftClientData.getInstance().stations);
        }

        /* Depots */
        public static @ValueNullable Depot findDepot(ScriptVector3f pos) {
            return InitClient.findDepot(pos.rawBlockPos());
        }

        public static @ValueNullable Depot getDepot(long depotId) {
            return MinecraftClientData.getInstance().depotIdMap.get(depotId);
        }

        public static List<Depot> getKnownDepots() {
            return new ArrayList<>(MinecraftClientData.getInstance().depots);
        }

        /* Route */
        public static SimplifiedRoute getRoute(long routeId) {
            return MinecraftClientData.getInstance().simplifiedRouteIdMap.get(routeId);
        }

        public static List<SimplifiedRoute> getKnownRoutes() {
            return new ArrayList<>(MinecraftClientData.getInstance().simplifiedRoutes);
        }

        public static List<SimplifiedRoute> getKnownRoutesPassingPlatform(long platformId) {
            ObjectArrayList<SimplifiedRoute> list = new ObjectArrayList<>();
            for(SimplifiedRoute route : MinecraftClientData.getInstance().simplifiedRoutes) {
                if(route.getPlatformIndex(platformId) >= 0) list.add(route);
            }
            return list;
        }

        /* Vehicles */
        public static VehicleExtension getVehicle(long vehicleId) {
            return MinecraftClientData.getInstance().vehicles.stream().filter(e -> e.getId() == vehicleId).findFirst().orElse(null);
        }

        public static List<VehicleExtension> getKnownVehicles() {
            return new ArrayList<>(MinecraftClientData.getInstance().vehicles);
        }

        public static boolean isPlayerMounted(long vehicleId) {
            return VehicleRidingMovement.isRiding(vehicleId);
        }

        /* Lifts */
        public static Lift getLift(long vehicleId) {
            return MinecraftClientData.getLift(vehicleId);
        }

        public static List<Lift> getKnownLifts() {
            return new ArrayList<>(MinecraftClientData.getInstance().lifts);
        }

        /* Rail */
        public static List<Platform> findNearbyPlatforms(ScriptVector3f pos, int radius) {
            List<Platform> platforms = new ArrayList<>();
            InitClient.findClosePlatform(pos.rawBlockPos(), radius, platforms::add);
            return platforms;
        }

        public static Rail getRailFromPath(PathData pathData) {
            String hexId = pathData.getRail().getHexId();
            for(MinecraftClientData.RailWrapper railWrapper : MinecraftClientData.getInstance().railWrapperList.values()) {
                if(railWrapper.getRail().getHexId().equals(hexId)) return railWrapper.getRail();
            }
            return pathData.getRail();
        }

        public static List<Rail> getKnownRails() {
            return new ArrayList<>(MinecraftClientData.getInstance().railWrapperList.values().stream().map(e -> e.getRail()).collect(Collectors.toList()));
        }

        public static List<Long> getBlockedSignalColors(String railHexId) {
            return new ArrayList<>(MinecraftClientData.getInstance().railIdToCurrentlyBlockedSignalColors.getOrDefault(railHexId, new LongArrayList()));
        }

        public static List<String> getKnownBlockedRails() {
            return new ArrayList<>(MinecraftClientData.getInstance().blockedRailIds);
        }

        /* ETAs */
        public static ArrivalsWrapper findArrivals(long platformId) {
            return findArrivals(List.of(platformId));
        }

        public static ArrivalsWrapper findArrivals(List<Long> platformId) {
            return new ArrivalsWrapper(ArrivalsCacheClient.INSTANCE.requestArrivals(new LongImmutableList(platformId)));
        }
    }
}
