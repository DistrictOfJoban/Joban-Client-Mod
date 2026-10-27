#include <mtr/script.hpp>
#include <mtr/gfx2d.hpp>
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
  /* Header flags byte (natural light): the old _pad0[0] at offset 1 —
     no struct size or field offset changes, old modules emit zeros. */
  static_assert(offsetof(JcmRecordHeader,flags)==1);
  static_assert(sizeof(JcmRecordHeader)==8);
  static_assert(sizeof(JcmDrawText)==104);
  static_assert(sizeof(JcmDrawTexture)==80);

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

  /* ---- facingAngle() (upstream v2.3.0-prerelease.2 port) ---- */
  { int ok=1;
    const int32_t ordinals[4]={2,3,4,5}; const float angles[4]={180,0,90,270};
    for(int i=0;i<4;i++){ eye.facing=ordinals[i]; if(candy.facing_angle()!=angles[i]) ok=0; }
    eye.facing=0; if(candy.facing_angle()!=0) ok=0; /* vertical: never occurs */
    if(!ok){ std::fprintf(stderr,"FAIL: facing_angle()\n"); return 30; }
  }

  /* ---- StateTracker<T> — upstream setState()→boolean semantics ---- */
  { mtr::StateTracker<const char*> st;
    if(!st.set_state("a",100.0)) { std::fprintf(stderr,"FAIL: first setState must report true\n"); return 31; }
    if(st.set_state("a",200.0))   { std::fprintf(stderr,"FAIL: repeat setState must report false\n"); return 32; }
    if(!st.set_state("b",300.0)) { std::fprintf(stderr,"FAIL: new state must report true\n"); return 33; }
    if(!(st.state_now()==std::string("b")&&st.state_last()==std::string("a")))
      { std::fprintf(stderr,"FAIL: stateNow/stateLast\n"); return 34; }
    if(st.state_now_duration(350.0)!=50.0){ std::fprintf(stderr,"FAIL: stateNowDuration\n"); return 35; }
    if(!st.changed_to("b")||!st.changed_from_to("a","b")||st.changed_from_to("x","b"))
      { std::fprintf(stderr,"FAIL: changedTo/changedFromTo\n"); return 36; }
    if(st.set_state("b",400.0)||st.changed_to("b")||st.state_now_first())
      { std::fprintf(stderr,"FAIL: changedTo must reset on repeat frames\n"); return 37; }
  }

  /* ---- renderType(String) parsing ---- */
  { if(mtr::parse_render_layer("TEXT")!=MTR_LAYER_TEXT) return 40;
    if(mtr::parse_render_layer("LIGHT_2")!=MTR_LAYER_LIGHT) return 41;
    if(mtr::parse_render_layer("LIGHT_1")!=MTR_LAYER_LIGHT) return 42;
    if(mtr::parse_render_layer("EXTERIOR")!=MTR_LAYER_EXTERIOR) return 43;
    if(mtr::parse_render_layer("INTERIOR")!=MTR_LAYER_INTERIOR) return 44;
    bool threw=false; try{ mtr::parse_render_layer("BOGUS"); }catch(const std::invalid_argument&){ threw=true; }
    if(!threw) return 45;
  }

  /* ---- Rect + naturalLight flag emission (upstream Rect port) ---- */
  {
    mtr::FrameRecorder frame;
    alignas(8) unsigned char records[512]{};
    float matrices[64]{}, floats[64]{};
    char strings[128]{};
    uint8_t pixels[512]{};
    frame.attach(records,matrices,strings,pixels,sizeof(pixels),floats,
                 sizeof(records),sizeof(matrices),sizeof(strings),sizeof(floats));
    frame.begin_frame();
    JcmFrameInput in{};
    mtr::PidsContext ctx(frame,in);

    mtr::Rect::create().color(0x00FC9700).pos(2,3).size(10,6).z_order(4).natural_light().draw(ctx);
    mtr::Texture::create("t").texture("example:textures/a.png").natural_light().draw(ctx);
    mtr::Texture::create("t2").texture("example:textures/b.png").draw(ctx);
    mtr::Text::create().text("hello").natural_light().draw(ctx);
    mtr::Text::create().text("plain").draw(ctx);

    JcmFrameOutput out{}; frame.write_output(out);
    if(out.record_count!=5){ std::fprintf(stderr,"FAIL: expected 5 records, got %d\n",(int)out.record_count); return 50; }
    const auto* rect=(const JcmDrawTexture*)(out.records);
    if(rect->header.kind!=JCM_DRAW_TEXTURE) return 51;
    if(rect->header.flags!=MTR_RECORD_FLAG_NATURAL_LIGHT) { std::fprintf(stderr,"FAIL: Rect natural_light flag\n"); return 52; }
    if(rect->layer!=(int32_t)MTR_LAYER_LIGHT) return 53;
    /* Java: ARGB_BLACK + color (int wraparound) — opaque tint. */
    if(rect->color!=(int32_t)0xFFFC9700u) { std::fprintf(stderr,"FAIL: Rect tint 0x%X\n",rect->color); return 54; }
    if(rect->x!=2||rect->y!=3||rect->w!=10||rect->h!=6||rect->z_order!=4) return 55;
    if(rect->u1!=0||rect->v1!=0||rect->u2!=1||rect->v2!=1) return 56;
    if(std::strncmp(out.string_arena+rect->texture_offset,"mtr:textures/block/white.png",(size_t)rect->texture_len)!=0
       ||rect->texture_len!=28) return 57;

    const uint8_t* walk=(const uint8_t*)out.records;
    const auto* rect_hdr=(const JcmDrawTexture*)walk;
    walk+=rect_hdr->header.record_size;
    const auto* tex1=(const JcmDrawTexture*)walk;
    if(tex1->header.flags!=MTR_RECORD_FLAG_NATURAL_LIGHT) return 58;
    walk+=tex1->header.record_size;
    const auto* tex2=(const JcmDrawTexture*)walk;
    if(tex2->header.flags!=0) { std::fprintf(stderr,"FAIL: Texture without natural_light must carry no flags\n"); return 59; }

    /* Rect defaults: 20x20 (PIDSDrawCall(20,20)), ARGB_WHITE tint. The tint
       reads 0xFEFFFFFF — ARGB_BLACK + ARGB_WHITE wraps in Java int arithmetic
       too, so upstream's default JS Rect is drawn at alpha 254/255. The C++
       port reproduces the wraparound exactly (bug-compatible by design). */
    frame.begin_frame();
    mtr::Rect::create().draw(ctx);
    frame.write_output(out);
    const auto* def=(const JcmDrawTexture*)(out.records);
    if(def->w!=20||def->h!=20||def->color!=(int32_t)0xFEFFFFFFu||def->z_order!=-1) return 60;

    /* Texture with no .texture(): must not crash (skip instead of
       strlen(nullptr)); Text with no .text() already degrades to empty. */
    frame.begin_frame();
    mtr::Texture::create().draw(ctx);
    mtr::Text::create().draw(ctx);
    frame.write_output(out);
    if(out.record_count!=1){ std::fprintf(stderr,"FAIL: texture-less Texture must be skipped\n"); return 61; }

    /* intern_const: the white-texture literal is interned ONCE per frame —
       a full record budget of Rects (HostBuffers::MAX_RECORDS) must leave
       ~28 arena bytes, not 256×28 = 7 KB, keeping headroom for the text
       payloads that share the 32 KB string arena. */
    {
      mtr::FrameRecorder big;
      alignas(8) unsigned char big_records[sizeof(JcmDrawText) * 256]{};
      float bm[64]{}, bf[64]{};
      char bs[32 * 1024]{};
      uint8_t bp[512]{};
      big.attach(big_records,bm,bs,bp,sizeof(bp),bf,
                 sizeof(big_records),sizeof(bm),sizeof(bs),sizeof(bf));
      big.begin_frame();
      JcmFrameInput bin{};
      mtr::PidsContext bctx(big,bin);
      for(int i=0;i<256;i++) mtr::Rect::create().pos(i,0).draw(bctx);
      JcmFrameOutput bout{}; big.write_output(bout);
      if(bout.record_count!=256){ std::fprintf(stderr,"FAIL: 256 rects emitted %d records\n",(int)bout.record_count); return 62; }
      if(bout.string_arena_len>64){ std::fprintf(stderr,"FAIL: white texture must dedup per frame (%lld bytes)\n",(long long)bout.string_arena_len); return 63; }
    }
  }

  std::puts("Native SDK facingAngle/StateTracker/Rect/flags checks passed");
}
