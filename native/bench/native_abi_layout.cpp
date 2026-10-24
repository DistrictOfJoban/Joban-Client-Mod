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
}
