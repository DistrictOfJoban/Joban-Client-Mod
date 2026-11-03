"""Deterministic fixtures used with the production non-vehicle snapshot writer."""
STUBS = {
 'com/lx862/mtrscripting/core/util/ScriptVector3f.java': 'package com.lx862.mtrscripting.core.util; public record ScriptVector3f(float x,float y,float z){public org.mtr.mapping.holder.BlockPos rawBlockPos(){return new org.mtr.mapping.holder.BlockPos((int)x,(int)y,(int)z);}}',
 'org/mtr/mapping/holder/BlockPos.java': 'package org.mtr.mapping.holder; public class BlockPos {final int x,y,z; public BlockPos(int x,int y,int z){this.x=x;this.y=y;this.z=z;} public int getX(){return x;} public int getY(){return y;} public int getZ(){return z;}}',
 'org/mtr/mapping/holder/MinecraftClient.java': 'package org.mtr.mapping.holder; public class MinecraftClient {public static MinecraftClient getInstance(){return new MinecraftClient();} public Object getWorldMapped(){return this;}}',
 'com/lx862/mtrscripting/core/integration/MinecraftClientWrapper.java': 'package com.lx862.mtrscripting.core.integration; public class MinecraftClientWrapper {public static long worldDayTime(){return 12000;}}',
 'org/mtr/core/data/SavedRailBase.java': 'package org.mtr.core.data; public class SavedRailBase {}',
 'org/mtr/core/data/Route.java': '''package org.mtr.core.data;
 public class Route {
  final long id; final int color; final String name; final String routeType;
  public Route(long id,int color,String name,String routeType){this.id=id;this.color=color;this.name=name;this.routeType=routeType;}
  public long getId(){return id;} public int getColor(){return color;} public String getName(){return name;}
  public TransportMode getTransportMode(){return TransportMode.TRAIN;}
  public String getRouteType(){return routeType;}
 }''',
 'org/mtr/core/data/StationExit.java': '''package org.mtr.core.data;
 public class StationExit {
  public String getName(){return "A";}
  public java.util.List<String> getDestinations(){return java.util.List.of("车站出口目的地甲","车站出口目的地乙");}
 }''',
 'org/mtr/core/data/Station.java': '''package org.mtr.core.data;
 import java.util.*;
 /* ABI 13 fixture station: zones 1/2, color #1E90FF, 2 interchange routes
    (42 from the interchange map, 43 from a platform), 1 connecting group
    (station 换乘车站, route 44), 2 connected stations, 1 exit with 2
    destinations, platforms "1" and "2" — the PSDStationWrapper stub walks
    these exactly like the real wrapper. */
 public class Station {
  private static final List<SavedRailBase> SAVED_RAILS=new ArrayList<>();
  private static final List<Station> CONNECTED_STATIONS=new ArrayList<>();
  private static final Map<Station,Map<Integer,List<Route>>> INTERCHANGE_MAP=new HashMap<>();
  static {
   CONNECTED_STATIONS.add(new Station(9223372036854775001L,"连接车站甲"));
   CONNECTED_STATIONS.add(new Station(9223372036854775002L,"连接车站乙"));
   INTERCHANGE_MAP.put(new Station(9223372036854775000L,"测试车站"),Map.of(0xFC9700,List.of(new Route(42,0xFC9700,"换乘线","train_metro"))));
   INTERCHANGE_MAP.put(new Station(9223372036854775003L,"换乘车站"),Map.of(0x9966CC,List.of(new Route(44,0x9966CC,"接驳线","train_light_rail"))));
   SAVED_RAILS.add(new Platform("1",List.of(new Route(42,0xFC9700,"换乘线","train_metro"))));
   SAVED_RAILS.add(new Platform("2",List.of(new Route(43,0x00A870,"本站线路","train"))));
  }
  final long id; final String name;
  public Station(){this(9223372036854775000L,"测试车站");}
  Station(long id,String name){this.id=id;this.name=name;}
  public long getId(){return id;}
  public String getName(){return name;}
  public int getColor(){return 0x1E90FF;}
  public String getColorHex(){return "#1E90FF";}
  public long getZone1(){return 1;} public long getZone2(){return 2;} public long getZone3(){return 0;}
  public final List<SavedRailBase> savedRails=SAVED_RAILS;
  public final List<Station> connectedStations=CONNECTED_STATIONS;
  public List<StationExit> getExits(){return List.of(new StationExit());}
  public Map<Station,Map<Integer,List<Route>>> getInterchangeStationToColorToRoutesMap(boolean sorted){return INTERCHANGE_MAP;}
 }''',
 'org/mtr/core/data/Platform.java': '''package org.mtr.core.data;
 import java.util.*;
 public class Platform extends SavedRailBase {
  public final Station area=new Station();
  public final List<Route> routes;
  final String name;
  public Platform(){this("1",List.of());}
  Platform(String name,List<Route> routes){this.name=name;this.routes=routes;}
  public long getId(){return 9223372036854775000L;}
  public String getName(){return name;}
  public String getStationName(){return "测试车站";}
  public long getDwellTime(){return 15000;}
 }''',
 'com/lx862/jcm/mod/scripting/psd/PSDWallScanner.java': '''package com.lx862.jcm.mod.scripting.psd;
 public class PSDWallScanner {public static class WallRun {public double doorValue=0.5;}}''',
 'com/lx862/jcm/mod/scripting/psd/PSDWrapper.java': '''package com.lx862.jcm.mod.scripting.psd;
 import java.util.*; import com.lx862.mtrscripting.core.util.ScriptVector3f;
 public class PSDWrapper {
  public enum PSDType {GLASS, TOP}
  public enum DataFetchMode {SKIP, MANDATORY, ALL}
  public enum Circular {NONE,CLOCKWISE,ANTICLOCKWISE}
  public enum TM {TRAIN}
  public final PSDType type=PSDType.GLASS; public final int width=96, height=150;
  public ScriptVector3f blockPos(){return new ScriptVector3f(1,2,3);}
  public org.mtr.core.data.Platform platformData(){return new org.mtr.core.data.Platform();}
  public PSDWallScanner.WallRun wallRun(){return new PSDWallScanner.WallRun();}
  public PSDStationWrapper stop(){return PSDStationWrapper.of(platformData().area);}
  public List<PSDDoorWrapper> doors(){return List.of(new PSDDoorWrapper(1,true,false),new PSDDoorWrapper(3,false,true));}
  public List<PSDArrivalWrapper> arrivalList(){return List.of(new PSDArrivalWrapper());}
  public List<PSDRouteWrapper> routes(){return List.of(new PSDRouteWrapper());}
  public static class PSDDoorWrapper {final int n; final boolean open,locked; PSDDoorWrapper(int n,boolean open,boolean locked){this.n=n;this.open=open;this.locked=locked;}
   public int number(){return n;} public boolean isOpen(){return open;} public boolean isLocked(){return locked;}
   public ScriptVector3f pos(){return new ScriptVector3f(1,2,3);}}
  public static class PSDArrivalWrapper extends com.lx862.mtrscripting.mod.impl.mtr.pids.ArrivalWrapper {
   public List<Integer> doorNumbers(){return List.of(1,3);}}
  public static class PSDRouteWrapper {
   public long id(){return 9223372036854775000L;} public int color(){return 0x00A870;}
   public Circular circularState(){return Circular.CLOCKWISE;} public String name(){return "测试线路";}
   public String routeNumber(){return "A1";} public String getTransportType(){return "train";}
   public List<PSDStopWrapper> stops(){return List.of(new PSDStopWrapper("车站甲","测试终点",true),new PSDStopWrapper("车站乙","测试终点",false));}}
  public static class PSDStopWrapper {public final String name,destinationName;
   PSDStopWrapper(String name,String dest,boolean withInterchange){this.name=name;this.destinationName=dest;this.withInterchange=withInterchange;}
   final boolean withInterchange;
   public String name(){return name;} public String destinationName(){return destinationName;}
   public String customDestination(){return null;}
   public org.mtr.core.data.Station station(){return new org.mtr.core.data.Station();}
   public org.mtr.core.data.Platform platform(){return new org.mtr.core.data.Platform();}
   public List<PSDInterchangeWrapper> interchanges(){return withInterchange?List.of(new PSDInterchangeWrapper()):List.of();}
   public List<PSDExitWrapper> exits(){return withInterchange?List.of():List.of(new PSDExitWrapper());}}
  public static class PSDInterchangeWrapper {
   final long routeId; final int color; final String name; final String transportType;
   public PSDInterchangeWrapper(){this(42,0xFC9700,"换乘线","train_metro");}
   PSDInterchangeWrapper(long routeId,int color,String name,String transportType){this.routeId=routeId;this.color=color;this.name=name;this.transportType=transportType;}
   public long routeId(){return routeId;} public int color(){return color;} public String name(){return name;}
   public TM transportMode(){return TM.TRAIN;} public String getTransportType(){return transportType;}}
  public static class PSDExitWrapper {
   private final String name; private final List<String> destinations;
   public PSDExitWrapper(){this("A",List.of("出口目的地"));}
   public PSDExitWrapper(String name,List<String> destinations){this.name=name;this.destinations=destinations;}
   public String name(){return name;} public List<String> destinations(){return destinations;}}
  public static class PSDStationWrapper {
   private final org.mtr.core.data.Station station;
   private final List<PSDInterchangeWrapper> interchanges=new ArrayList<>();
   private final Map<String,List<PSDInterchangeWrapper>> connectingInterchanges=new HashMap<>();
   private final List<PSDExitWrapper> exits=new ArrayList<>();
   private final List<String> platformNames=new ArrayList<>();
   private final List<String> connectedStationNames=new ArrayList<>();
   public static PSDStationWrapper of(org.mtr.core.data.Station station){return new PSDStationWrapper(station);}
   private PSDStationWrapper(org.mtr.core.data.Station station){
    this.station=station;
    station.getInterchangeStationToColorToRoutesMap(true).forEach((icStation,colorToRoutes)->{
     if(icStation.getId()==station.getId()) colorToRoutes.forEach((color,routes)->routes.forEach(route->interchanges.add(wrap(route,color))));
     else colorToRoutes.forEach((color,routes)->routes.forEach(route->connectingInterchanges.computeIfAbsent(icStation.getName(),k->new ArrayList<>()).add(wrap(route,color))));
    });
    for(org.mtr.core.data.SavedRailBase savedRail:station.savedRails){
     if(!(savedRail instanceof org.mtr.core.data.Platform)) continue;
     org.mtr.core.data.Platform platform=(org.mtr.core.data.Platform)savedRail;
     platformNames.add(platform.getName());
     platform.routes.forEach(route->{if(interchanges.stream().noneMatch(e->e.routeId()==route.getId())) interchanges.add(wrap(route,route.getColor()));});
    }
    for(org.mtr.core.data.StationExit exit:station.getExits()) exits.add(new PSDExitWrapper(exit.getName(),exit.getDestinations()));
    for(org.mtr.core.data.Station connected:station.connectedStations) connectedStationNames.add(connected.getName());
   }
   private PSDInterchangeWrapper wrap(org.mtr.core.data.Route route,int color){return new PSDInterchangeWrapper(route.getId(),color,route.getName(),route.getRouteType());}
   public long id(){return station.getId();}
   public String name(){return station.getName();}
   public int color(){return station.getColor();}
   public String colorHex(){return station.getColorHex();}
   public long zone1(){return station.getZone1();} public long zone2(){return station.getZone2();} public long zone3(){return station.getZone3();}
   public List<PSDInterchangeWrapper> interchanges(){return interchanges;}
   public Map<String,List<PSDInterchangeWrapper>> connectingInterchanges(){return connectingInterchanges;}
   public List<String> connectedStations(){return connectedStationNames;}
   public List<PSDExitWrapper> exits(){return exits;}
   public List<String> platforms(){return platformNames;}
   public int platformCount(){return platformNames.size();}
  }
 }''',
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
