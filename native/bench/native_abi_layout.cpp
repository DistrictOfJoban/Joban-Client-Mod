#include <mtr/mtr_native.h>
#include <cstddef>
#include <cstdio>

#define SIZE(key, type) std::printf(#key "=%zu\n", sizeof(type))
#define FIELD(key, type, field) std::printf(#key "=%zu\n", offsetof(type, field))
int main() {
    SIZE(SZ_VEHICLE, JcmVehicleSnapshot);
    SIZE(SZ_CAR, JcmCar);
    SIZE(SZ_STOP, JcmStop);
    SIZE(SZ_INTERCHANGE, JcmInterchange);
    SIZE(SZ_EXIT, JcmExit);
    SIZE(SZ_STR_REF, JcmStrRef);
    FIELD(OFF_VEHICLE_ID, JcmVehicleSnapshot, vehicle_id);
    FIELD(OFF_SIDING_ID, JcmVehicleSnapshot, siding_id);
    FIELD(OFF_THIS_ROUTE_ID, JcmVehicleSnapshot, this_route_id);
    FIELD(OFF_DEPARTURE_INDEX, JcmVehicleSnapshot, departure_index);
    FIELD(OFF_CAR_COUNT, JcmVehicleSnapshot, car_count);
    FIELD(OFF_TRANSPORT_MODE, JcmVehicleSnapshot, transport_mode);
    FIELD(OFF_SPEED_KMH, JcmVehicleSnapshot, speed_kmh);
    FIELD(OFF_SPEED_MS, JcmVehicleSnapshot, speed_ms);
    FIELD(OFF_RAIL_PROGRESS, JcmVehicleSnapshot, rail_progress);
    FIELD(OFF_DOOR_VALUE, JcmVehicleSnapshot, door_value);
    FIELD(OFF_NOTCH_LEVEL, JcmVehicleSnapshot, notch_level);
    FIELD(OFF_FLAGS, JcmVehicleSnapshot, reversed);
    FIELD(OFF_DOOR_DIRECTION, JcmVehicleSnapshot, door_direction);
    FIELD(OFF_TOTAL_DWELL, JcmVehicleSnapshot, total_dwell_time_millis);
    FIELD(OFF_ELAPSED_DWELL, JcmVehicleSnapshot, elapsed_dwell_time_millis);
    FIELD(OFF_GAME_TIME, JcmVehicleSnapshot, game_time_millis);
    FIELD(OFF_IN_GAME_TIME, JcmVehicleSnapshot, in_game_time);
    FIELD(OFF_CAR_OFFSET, JcmVehicleSnapshot, car_offset);
    FIELD(OFF_STOP_COUNT, JcmVehicleSnapshot, stop_count);
    FIELD(OFF_STOP_OFFSET, JcmVehicleSnapshot, stop_offset);
    FIELD(OFF_THIS_ROUTE_STOP_COUNT, JcmVehicleSnapshot, this_route_stop_count);
    FIELD(OFF_THIS_ROUTE_STOP_OFFSET, JcmVehicleSnapshot, this_route_stop_offset);
    FIELD(OFF_NEXT_ROUTE_STOP_COUNT, JcmVehicleSnapshot, next_route_stop_count);
    FIELD(OFF_NEXT_ROUTE_STOP_OFFSET, JcmVehicleSnapshot, next_route_stop_offset);
    FIELD(OFF_NEXT_STOP_INDEX, JcmVehicleSnapshot, next_stop_index);
    FIELD(OFF_ROUTE_NAME_OFFSET, JcmVehicleSnapshot, route_name_offset);
    FIELD(OFF_ROUTE_NAME_LEN, JcmVehicleSnapshot, route_name_len);
    FIELD(OFF_ROUTE_COLOR, JcmVehicleSnapshot, route_color);
    FIELD(OFF_CIRCULAR_STATE, JcmVehicleSnapshot, circular_state);
    FIELD(OFF_SIDING_NAME_OFFSET, JcmVehicleSnapshot, siding_name_offset);
    FIELD(OFF_SIDING_NAME_LEN, JcmVehicleSnapshot, siding_name_len);
    FIELD(OFF_STRING_POOL_OFFSET, JcmVehicleSnapshot, string_pool_offset);
    FIELD(OFF_STRING_POOL_LEN, JcmVehicleSnapshot, string_pool_len);
    FIELD(CAR_LENGTH, JcmCar, length);
    FIELD(CAR_WIDTH, JcmCar, width);
    FIELD(CAR_LEFT_DOOR, JcmCar, left_door_open);
    FIELD(CAR_RIGHT_DOOR, JcmCar, right_door_open);
    FIELD(CAR_RENDERED, JcmCar, rendered);
    FIELD(CAR_TYPE_OFFSET, JcmCar, vehicle_type_offset);
    FIELD(CAR_TYPE_LEN, JcmCar, vehicle_type_len);
    FIELD(STOP_ROUTE_ID, JcmStop, route_id);
    FIELD(STOP_STATION_ID, JcmStop, station_id);
    FIELD(STOP_PLATFORM_ID, JcmStop, platform_id);
    FIELD(STOP_DISTANCE, JcmStop, distance);
    FIELD(STOP_DWELL, JcmStop, dwell_time_millis);
    FIELD(STOP_NAME_OFFSET, JcmStop, name_offset);
    FIELD(STOP_NAME_LEN, JcmStop, name_len);
    FIELD(STOP_DEST_OFFSET, JcmStop, destination_offset);
    FIELD(STOP_DEST_LEN, JcmStop, destination_len);
    FIELD(STOP_CUSTOM_OFFSET, JcmStop, custom_destination_offset);
    FIELD(STOP_CUSTOM_LEN, JcmStop, custom_destination_len);
    FIELD(STOP_IC_COUNT, JcmStop, interchange_count);
    FIELD(STOP_IC_OFFSET, JcmStop, interchange_offset);
    FIELD(STOP_EXIT_COUNT, JcmStop, exit_count);
    FIELD(STOP_EXIT_OFFSET, JcmStop, exit_offset);
    FIELD(STOP_ROUTE_CIRCULAR, JcmStop, route_circular_state);
    FIELD(STOP_SWITCHOVER, JcmStop, is_route_switchover);
    /* ABI 12: PSD snapshots (pinned by the static_asserts in mtr_native.h
       and hard-coded in NativeContentSnapshot.psd). */
    SIZE(SZ_PSD_ARRIVAL, JcmPsdArrival);
    SIZE(SZ_PSD_ROUTE, JcmPsdRoute);
    SIZE(SZ_PSD_DOOR, JcmPsdDoor);
    SIZE(SZ_PSD, JcmPsdSnapshot);
    FIELD(PSD_PLATFORM_ID, JcmPsdSnapshot, platform_id);
    FIELD(PSD_STATION_ID, JcmPsdSnapshot, station_id);
    FIELD(PSD_GAME_TIME, JcmPsdSnapshot, game_time_millis);
    FIELD(PSD_IN_GAME_TIME, JcmPsdSnapshot, in_game_time);
    FIELD(PSD_DOOR_VALUE, JcmPsdSnapshot, door_value);
    FIELD(PSD_TYPE, JcmPsdSnapshot, psd_type);
    FIELD(PSD_WIDTH, JcmPsdSnapshot, width);
    FIELD(PSD_HEIGHT, JcmPsdSnapshot, height);
    FIELD(PSD_DOOR_COUNT, JcmPsdSnapshot, door_count);
    FIELD(PSD_DOOR_OFFSET, JcmPsdSnapshot, door_offset);
    FIELD(PSD_ARRIVAL_COUNT, JcmPsdSnapshot, arrival_count);
    FIELD(PSD_ARRIVAL_OFFSET, JcmPsdSnapshot, arrival_offset);
    FIELD(PSD_ROUTE_COUNT, JcmPsdSnapshot, route_count);
    FIELD(PSD_ROUTE_OFFSET, JcmPsdSnapshot, route_offset);
    FIELD(PSD_PLATFORM_NUMBER_OFFSET, JcmPsdSnapshot, platform_number_offset);
    FIELD(PSD_STATION_NAME_OFFSET, JcmPsdSnapshot, station_name_offset);
    FIELD(PSD_STRING_POOL_OFFSET, JcmPsdSnapshot, string_pool_offset);
    FIELD(PSD_STRING_POOL_LEN, JcmPsdSnapshot, string_pool_len);
    FIELD(PSD_ARRIVAL_DOOR_COUNT, JcmPsdArrival, door_count);
    FIELD(PSD_ARRIVAL_DOOR_OFFSET, JcmPsdArrival, door_offset);
    FIELD(PSD_ROUTE_STOP_COUNT, JcmPsdRoute, stop_count);
    FIELD(PSD_ROUTE_STOP_OFFSET, JcmPsdRoute, stop_offset);
    FIELD(PSD_DOOR_NUMBER, JcmPsdDoor, number);
    FIELD(PSD_DOOR_OPEN, JcmPsdDoor, open);
    /* ABI 13: PSD station info — JcmPsdSnapshot._pad0 (124) repurposed as
       station_offset; JcmPsdStation + its interchange / connecting-group
       pools (pinned by the static_asserts in mtr_native.h and hard-coded
       in NativeContentSnapshot.psd). */
    FIELD(PSD_STATION_OFFSET, JcmPsdSnapshot, station_offset);
    SIZE(SZ_PSD_STATION_INTERCHANGE, JcmPsdStationInterchange);
    SIZE(SZ_PSD_STATION_CONNECTING_GROUP, JcmPsdStationConnectingGroup);
    SIZE(SZ_PSD_STATION, JcmPsdStation);
    FIELD(PSD_STATION_STATION_ID, JcmPsdStation, station_id);
    FIELD(PSD_STATION_COLOR, JcmPsdStation, color);
    FIELD(PSD_STATION_ZONE1, JcmPsdStation, zone1);
    FIELD(PSD_STATION_ZONE2, JcmPsdStation, zone2);
    FIELD(PSD_STATION_ZONE3, JcmPsdStation, zone3);
    FIELD(PSD_STATION_NAME_OFFSET, JcmPsdStation, name_offset);
    FIELD(PSD_STATION_COLOR_HEX_OFFSET, JcmPsdStation, color_hex_offset);
    FIELD(PSD_STATION_INTERCHANGE_COUNT, JcmPsdStation, interchange_count);
    FIELD(PSD_STATION_INTERCHANGE_OFFSET, JcmPsdStation, interchange_offset);
    FIELD(PSD_STATION_CONNECTING_COUNT, JcmPsdStation, connecting_count);
    FIELD(PSD_STATION_CONNECTING_OFFSET, JcmPsdStation, connecting_offset);
    FIELD(PSD_STATION_CONNECTED_COUNT, JcmPsdStation, connected_station_count);
    FIELD(PSD_STATION_CONNECTED_OFFSET, JcmPsdStation, connected_station_offset);
    FIELD(PSD_STATION_EXIT_COUNT, JcmPsdStation, exit_count);
    FIELD(PSD_STATION_EXIT_OFFSET, JcmPsdStation, exit_offset);
    FIELD(PSD_STATION_PLATFORM_COUNT, JcmPsdStation, platform_count);
    FIELD(PSD_STATION_PLATFORM_OFFSET, JcmPsdStation, platform_offset);
    FIELD(PSD_STATION_IC_ROUTE_ID, JcmPsdStationInterchange, route_id);
    FIELD(PSD_STATION_IC_COLOR, JcmPsdStationInterchange, color);
    FIELD(PSD_STATION_IC_TRANSPORT_MODE, JcmPsdStationInterchange, transport_mode);
    FIELD(PSD_STATION_IC_NAME_OFFSET, JcmPsdStationInterchange, name_offset);
    FIELD(PSD_STATION_IC_TYPE_OFFSET, JcmPsdStationInterchange, type_offset);
    FIELD(PSD_STATION_CG_NAME_OFFSET, JcmPsdStationConnectingGroup, station_name_offset);
    FIELD(PSD_STATION_CG_ROUTE_COUNT, JcmPsdStationConnectingGroup, route_count);
    FIELD(PSD_STATION_CG_ROUTE_OFFSET, JcmPsdStationConnectingGroup, route_offset);
}
