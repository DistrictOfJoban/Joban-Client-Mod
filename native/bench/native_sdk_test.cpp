#include <mtr/script.hpp>
#include <cstdio>
int main() {
  static_assert(sizeof(JcmInterchange)==16);
  static_assert(sizeof(JcmPidsSnapshot)==104);
  static_assert(sizeof(JcmArrival)==96);
  static_assert(sizeof(JcmEyecandySnapshot)==88);
  static_assert(sizeof(JcmLiftSnapshot)==136);
  static_assert(sizeof(JcmLiftFloor)==48);
  static_assert(offsetof(JcmDrawText,text_offset)==96);
  static_assert(offsetof(JcmDrawTexture,texture_offset)==68);
  JcmLiftSnapshot snap{}; snap.width=3.5; snap.direction=-1;
  mtr::Lift lift(&snap);
  if(lift.width()!=3.5||lift.direction()!=-1) return 1;
  bool caught=false; try { lift.floor(-1); } catch(const std::out_of_range&) {caught=true;}
  if(!caught) return 2;
  JcmEyecandySnapshot eye{}; eye.block_use_events=1;
  mtr::EyeCandy candy(&eye);
  if(!candy.consume_block_use()||candy.consume_block_use()) return 3;
  std::puts("Native SDK layout, lift and bounds checks passed");
}
