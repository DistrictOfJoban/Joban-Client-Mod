package com.lx862.mtrscripting.mod.impl.mtr.util;

import com.lx862.mtrscripting.core.annotation.ValueNullable;
import com.lx862.mtrscripting.core.util.ScriptVector3f;
import com.lx862.mtrscripting.mod.impl.mtr.pids.ArrivalsWrapper;
import org.mtr.core.data.*;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongImmutableList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mod.Init;
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

        /* Depot */
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

        public static VehicleExtension getPlayerMountedVehicle() {
            for(VehicleExtension vehicleExtension : MinecraftClientData.getInstance().vehicles) {
                if(isPlayerMounted(vehicleExtension.getId())) {
                    return vehicleExtension;
                }
            }
            return null;
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

        public static Lift getPlayerMountedLift() {
            for(MinecraftClientData.LiftWrapper liftWrapper : MinecraftClientData.getInstance().liftWrapperList.values()) {
                if(VehicleRidingMovement.isRiding(liftWrapper.getLift().getId())) {
                    return liftWrapper.getLift();
                }
            }
            return null;
        }

        public static List<Lift> getKnownLifts() {
            return new ArrayList<>(MinecraftClientData.getInstance().lifts);
        }

        /* Siding */
        public static @ValueNullable Siding getSiding(long platformId) {
            return MinecraftClientData.getInstance().sidingIdMap.get(platformId);
        }

        public static List<Siding> getKnownSidings() {
            return new ArrayList<>(MinecraftClientData.getInstance().sidings);
        }

        public static List<Siding> findNearbySidings(ScriptVector3f pos, int radius) {
            List<Siding> sidings = new ArrayList<>();
            Position position = Init.blockPosToPosition(pos.rawBlockPos());
            MinecraftClientData.getInstance().sidings
                    .stream()
                    .filter((siding) -> siding.closeTo(position, radius))
                    .forEach(sidings::add);
            return sidings;
        }

        /* Platform */
        public static @ValueNullable Platform getPlatform(long platformId) {
            return MinecraftClientData.getInstance().platformIdMap.get(platformId);
        }

        public static List<Platform> getKnownPlatforms() {
            return new ArrayList<>(MinecraftClientData.getInstance().platforms);
        }

        public static List<Platform> findNearbyPlatforms(ScriptVector3f pos, int radius) {
            List<Platform> platforms = new ArrayList<>();
            Position position = Init.blockPosToPosition(pos.rawBlockPos());
            MinecraftClientData.getInstance().platforms
                    .stream()
                    .filter((platform) -> platform.closeTo(position, radius))
                    .forEach(platforms::add);
            return platforms;
        }

        /* Rail */
        public static Rail getRailFromPath(PathData pathData) {
            String hexId = pathData.getRail().getHexId();
            for(MinecraftClientData.RailWrapper railWrapper : MinecraftClientData.getInstance().railWrapperList.values()) {
                if(railWrapper.getRail().getHexId().equals(hexId)) return railWrapper.getRail();
            }
            return pathData.getRail(); // Fallback
        }

        public static List<Rail> getKnownRails() {
            return new ArrayList<>(MinecraftClientData.getInstance().railWrapperList.values().stream().map(e -> e.getRail()).collect(Collectors.toList()));
        }

        public static boolean isRailBlocked(String railHexId) {
            return MinecraftClientData.getInstance().blockedRailIds.contains(railHexId);
        }

        public static List<String> getKnownBlockedRails() {
            return new ArrayList<>(MinecraftClientData.getInstance().blockedRailIds);
        }

        public static List<Long> getBlockedSignalColors(String railHexId) {
            return new ArrayList<>(MinecraftClientData.getInstance().railIdToCurrentlyBlockedSignalColors.getOrDefault(railHexId, new LongArrayList()));
        }

        /* ETAs */
        public static ArrivalsWrapper getArrivals(long platformId) {
            return getArrivals(new long[]{platformId});
        }

        public static ArrivalsWrapper getArrivals(long[] platformId) {
            return new ArrivalsWrapper(ArrivalsCacheClient.INSTANCE.requestArrivals(new LongImmutableList(platformId)));
        }
    }
}
