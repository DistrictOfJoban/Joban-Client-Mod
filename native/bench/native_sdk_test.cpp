#include <mtr/script.hpp>
#include <cstdio>
#include <cstring>

int main() {
  /* ABI 9: JcmInterchange grew from 16 to 24 bytes (type_offset/type_len). */
  static_assert(sizeof(JcmInterchange)==24);
  static_assert(offsetof(JcmInterchange,type_offset)==16);
  static_assert(offsetof(JcmInterchange,type_len)==20);
  static_assert(sizeof(JcmVehicleSnapshot)==192);
  static_assert(offsetof(JcmVehicleSnapshot,vehicle_type_offset)==184);
  static_assert(offsetof(JcmVehicleSnapshot,vehicle_type_len)==188);
  /* ABI 11: the former _pad0 flags byte is promoted to door_direction at
     the same offset — struct size and every other field keep their layout. */
  static_assert(offsetof(JcmVehicleSnapshot,door_direction)==83);
  static_assert(sizeof(JcmPidsSnapshot)==104);
  static_assert(sizeof(JcmArrival)==96);
  static_assert(sizeof(JcmEyecandySnapshot)==88);
  static_assert(sizeof(JcmLiftSnapshot)==136);
  static_assert(sizeof(JcmLiftFloor)==48);
  static_assert(offsetof(JcmDrawText,text_offset)==96);
  static_assert(offsetof(JcmDrawTexture,texture_offset)==68);

  /* Vehicle type + interchange type wrappers read an ABI 9 snapshot blob:
     [ JcmVehicleSnapshot | JcmInterchange | "train_metro" ] in one block. */
  alignas(8) uint8_t blob[sizeof(JcmVehicleSnapshot) + sizeof(JcmInterchange) + 16] = {};
  auto* snap = reinterpret_cast<JcmVehicleSnapshot*>(blob);
  const int32_t icOffset = static_cast<int32_t>(sizeof(JcmVehicleSnapshot));
  const int32_t poolOffset = icOffset + static_cast<int32_t>(sizeof(JcmInterchange));
  const char* typeName = "train_metro";
  const int32_t typeNameLen = static_cast<int32_t>(std::strlen(typeName));
  std::memcpy(blob + poolOffset, typeName, static_cast<size_t>(typeNameLen));

  snap->transport_mode = 0;
  snap->vehicle_type_offset = poolOffset;
  snap->vehicle_type_len = typeNameLen;
  snap->string_pool_offset = poolOffset;

  /* The wrapper is mtr::Vehicle (MTR 4 terminology); the MTR 3-era name
     mtr::Train must remain a working source alias. */
  static_assert(std::is_same<mtr::Train, mtr::Vehicle>::value);
  mtr::Vehicle vehicle(snap);
  if (vehicle.getTransportMode() != mtr::TransportMode::TRAIN) return 10;
  mtr::StrView type = vehicle.getTransportType();
  if (type.len != typeNameLen || !(type == mtr::transport_type::TRAIN_METRO)) return 11;
  if (type == mtr::transport_type::TRAIN) return 12;
  mtr::Train legacy(snap); /* alias compiles and reads the same snapshot */
  if (legacy.getTransportMode() != mtr::TransportMode::TRAIN) return 13;

  /* ABI 11: door_direction byte → typed enum + string form. Zeroed blob
     (memset above) must read as "not" — that is also what an ABI≤10 host
     zero-fills for modules that predate the field. */
  if (vehicle.doorDirection() != mtr::DoorDirection::NOT) return 18;
  if (!(vehicle.getDirectionOfDoorOpening() == mtr::door_direction::NOT)) return 19;
  snap->door_direction = MTR_DOOR_DIRECTION_RIGHT;
  if (vehicle.doorDirection() != mtr::DoorDirection::RIGHT) return 20;
  if (!(vehicle.getDirectionOfDoorOpening() == mtr::door_direction::RIGHT)) return 21;
  if (legacy.getDirectionOfDoorOpening().str() != vehicle.getDirectionOfDoorOpening().str()) return 22;
  snap->door_direction = MTR_DOOR_DIRECTION_ALL;
  if (vehicle.doorDirection() != mtr::DoorDirection::ALL
      || !(vehicle.getDirectionOfDoorOpening() == mtr::door_direction::ALL)) return 23;
  snap->door_direction = MTR_DOOR_DIRECTION_LEFT;
  if (vehicle.doorDirection() != mtr::DoorDirection::LEFT
      || !(vehicle.getDirectionOfDoorOpening() == mtr::door_direction::LEFT)) return 24;
  snap->door_direction = 0; /* restore for the sections below */

  auto* ic = reinterpret_cast<JcmInterchange*>(blob + icOffset);
  ic->color = 0xFF00FF;
  ic->transport_mode = -1;
  ic->route_name_offset = poolOffset;
  ic->route_name_len = 0;   /* empty name */
  ic->type_offset = poolOffset;
  ic->type_len = typeNameLen;

  JcmStop stop{};
  stop.interchange_count = 1;
  stop.interchange_offset = icOffset;
  mtr::Stop wrapped(snap, &stop);
  mtr::Stop::Interchange inter = wrapped.interchange(0);
  if (inter.color != 0xFF00FF) return 14;
  if (inter.getTransportMode() != mtr::TransportMode::UNKNOWN) return 15;
  if (!(inter.getTransportType() == mtr::transport_type::TRAIN_METRO)) return 16;
  if (!inter.name.empty()) return 17;

  JcmLiftSnapshot lift_snap{}; lift_snap.width=3.5; lift_snap.direction=-1;
  mtr::Lift lift(&lift_snap);
  if(lift.width()!=3.5||lift.direction()!=-1) return 1;
  bool caught=false; try { lift.floor(-1); } catch(const std::out_of_range&) {caught=true;}
  if(!caught) return 2;
  JcmEyecandySnapshot eye{}; eye.block_use_events=1;
  mtr::EyeCandy candy(&eye);
  if(!candy.consume_block_use()||candy.consume_block_use()) return 3;
  std::puts("Native SDK layout, transport types, lift and bounds checks passed");
}
