"""Deterministic fixtures used with the production non-vehicle snapshot writer."""
STUBS = {
 'com/lx862/mtrscripting/core/util/ScriptVector3f.java': 'package com.lx862.mtrscripting.core.util; public record ScriptVector3f(float x,float y,float z){}',
 'org/mtr/mapping/holder/MinecraftClient.java': 'package org.mtr.mapping.holder; public class MinecraftClient {public static MinecraftClient getInstance(){return new MinecraftClient();} public Object getWorldMapped(){return this;}}',
 'com/lx862/mtrscripting/core/integration/MinecraftClientWrapper.java': 'package com.lx862.mtrscripting.core.integration; public class MinecraftClientWrapper {public static long worldDayTime(){return 12000;}}',
 'org/mtr/core/data/Station.java': 'package org.mtr.core.data; public class Station {public long getId(){return 9223372036854775000L;}}',
 'com/lx862/jcm/mod/scripting/pids/PIDSWrapper.java': '''package com.lx862.jcm.mod.scripting.pids;
 import java.util.*; import com.lx862.mtrscripting.core.util.ScriptVector3f;
 public class PIDSWrapper {
  public final int width=42,height=24,rows=2;
  public ScriptVector3f blockPos(){return new ScriptVector3f(1,2,3);}
  public org.mtr.core.data.Station station(){return new org.mtr.core.data.Station();}
  public boolean isKeyBlock(){return true;} public boolean isPlatformNumberHidden(){return false;}
  public boolean isRowHidden(int i){return i==1;} public String getCustomMessage(int i){return i==0?"自定义":null;}
  public List<com.lx862.mtrscripting.mod.impl.mtr.pids.ArrivalWrapper> arrivals(){return List.of(new com.lx862.mtrscripting.mod.impl.mtr.pids.ArrivalWrapper());}
  public LongIds getTargetPlatformIds(){return new LongIds();}
  public static class LongIds {public int size(){return 1;} public long getLong(int i){return 9223372036854775000L;}}
 }''',
 'com/lx862/mtrscripting/mod/impl/mtr/pids/ArrivalWrapper.java': '''package com.lx862.mtrscripting.mod.impl.mtr.pids;
 public class ArrivalWrapper {
  public long arrivalTime(){return 1700000000000L;} public long departureTime(){return arrivalTime()+30000;}
  public long deviation(){return 2500;} public long departureIndex(){return 17;}
  public long routeId(){return 9223372036854775000L;} public long platformId(){return routeId();}
  public int routeColor(){return 0x123456;} public int carCount(){return 8;}
  public boolean realtime(){return true;} public boolean terminating(){return false;}
  public enum Circular {NONE,CLOCKWISE,ANTICLOCKWISE} public Circular circularState(){return Circular.ANTICLOCKWISE;}
  public String routeNumber(){return "A1";} public String routeName(){return "测试线路";}
  public String destination(){return "测试终点";} public String platformName(){return "站台";}
 }''',
 'com/lx862/mtrscripting/mod/impl/mtr/eyecandy/EyecandyBlockEntityWrapper.java': '''package com.lx862.mtrscripting.mod.impl.mtr.eyecandy;
 import com.lx862.mtrscripting.core.util.ScriptVector3f;
 public class EyecandyBlockEntityWrapper {
  public ScriptVector3f blockPos(){return new ScriptVector3f(1,2,3);}
  public float getTranslateX(){return 1;} public float getTranslateY(){return 2;} public float getTranslateZ(){return 3;}
  public float getRotateX(){return 0.1f;} public float getRotateY(){return 0.2f;} public float getRotateZ(){return 0.3f;}
  public enum Facing {DOWN,UP,NORTH,SOUTH,WEST,EAST} public Facing facing(){return Facing.EAST;}
  public boolean getFullBrightness(){return true;} public boolean isCrosshairTarget(){return true;}
  public int redstoneLevel(){return 13;} public String getModelId(){return "test:decoration";}
 }''',
 'com/lx862/mtrscripting/mod/impl/mtr/lift/LiftWrapper.java': '''package com.lx862.mtrscripting.mod.impl.mtr.lift;
 import java.util.*; import com.lx862.mtrscripting.core.util.ScriptVector3f;
 public class LiftWrapper {
  public long getId(){return 9223372036854775000L;}
  public ScriptVector3f getRawPos(){return new ScriptVector3f(1,2,3);} public ScriptVector3f getOffset(){return new ScriptVector3f(4,5,6);}
  public double getWidth(){return 3.5;} public double getHeight(){return 4;} public double getDepth(){return 2;}
  public float getAngleDegrees(){return 90;} public double getAngleRadians(){return Math.PI/2;} public float getDoorValue(){return 0.5f;}
  public int getDirection(){return -1;} public boolean shouldRender(){return true;} public boolean isDoubleSided(){return true;}
  public boolean isClientPlayerRiding(){return false;} public List<Floor> getFloors(){return List.of(new Floor(0),new Floor(1));}
  public static class Floor {final int index; Floor(int i){index=i;} public int getIndex(){return index;}
   public boolean isCurrentFloor(){return index==0;} public boolean isTargetFloor(){return index==1;}
   public ScriptVector3f getPos(){return new ScriptVector3f(1,index*10,3);}
   public String getNumber(){return index==0?"地面":"一层";} public String getDescription(){return "测试楼层";}
  }
 }'''
}
