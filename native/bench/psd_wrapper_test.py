"""Run production PSD/PIDS wrappers and route cache with mutable MTR fixtures.

The production classes are compiled intact; only Minecraft/MTR data and wall
scanning are stubbed. Intended for GitHub Actions, not local execution.
"""
import argparse
import os
from pathlib import Path
import subprocess

from psd_wall_test import STUBS as WALL_STUBS
from psd_bindings_test import STUBS as BINDING_STUBS


STUBS = dict(WALL_STUBS)
del STUBS['com/lx862/jcm/mod/scripting/psd/PSDWrapper.java']
STUBS['com/lx862/mtrscripting/core/util/ScriptVector3f.java'] = BINDING_STUBS['com/lx862/mtrscripting/core/util/ScriptVector3f.java']
STUBS.update({
    'org/mtr/core/data/SavedRailBase.java': 'package org.mtr.core.data; public class SavedRailBase {}',
    'org/mtr/core/data/Platform.java': '''package org.mtr.core.data;
import java.util.*;
public final class Platform extends SavedRailBase {
 public final Station area;public final List<Route> routes=new ArrayList<>();private final long id;private final String name;
 public Platform(long id,String name,Station area){this.id=id;this.name=name;this.area=area;}
 public long getId(){return id;}public String getName(){return name;}public String getStationName(){return area.getName();}
 public long getDwellTime(){return 1000;}public Position getMidPosition(){return new Position();}
}''',
    'org/mtr/core/data/Route.java': '''package org.mtr.core.data;
public final class Route {
 public enum CircularState {NONE}
 private final long id;private final String name;public Route(long id,String name){this.id=id;this.name=name;}
 public long getId(){return id;}public String getName(){return name;}public String getRouteNumber(){return "1";}
 public int getColor(){return (int)id;}public TransportMode getTransportMode(){return TransportMode.TRAIN;}
 public int getRouteType(){return 0;}public CircularState getCircularState(){return CircularState.NONE;}
}''',
    'org/mtr/core/data/Station.java': '''package org.mtr.core.data;
import java.util.*;
public final class Station {
 private final long id;private final String name;public Station(long id,String name){this.id=id;this.name=name;}
 public final List<SavedRailBase> savedRails=new ArrayList<>();public final List<Station> connectedStations=new ArrayList<>();
 public final List<StationExit> exits=new ArrayList<>();
 public final Map<Station,Map<Integer,List<Route>>> interchanges=new HashMap<>();
 public Map<Station,Map<Integer,List<Route>>> getInterchangeStationToColorToRoutesMap(boolean b){return interchanges;}
 public List<StationExit> getExits(){return exits;}
 public long getId(){return id;}public String getName(){return name;}public int getColor(){return 0;}
 public String getColorHex(){return "#000000";}public long getZone1(){return 1;}public long getZone2(){return 2;}public long getZone3(){return 3;}
}''',
    'org/mtr/core/data/StationExit.java': '''package org.mtr.core.data;
import java.util.*;
public record StationExit(String name,List<String> destinations) {
 public String getName(){return name;}public List<String> getDestinations(){return destinations;}
}''',
    'org/mtr/core/data/SimplifiedRoutePlatform.java': '''package org.mtr.core.data;
public record SimplifiedRoutePlatform(long stationId,long platformId,String stationName,String destination) {
 public long getStationId(){return stationId;}public long getPlatformId(){return platformId;}
 public String getStationName(){return stationName;}public String getDestination(){return destination;}
}''',
    'org/mtr/core/data/SimplifiedRoute.java': '''package org.mtr.core.data;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
public final class SimplifiedRoute {
 public final ObjectArrayList<SimplifiedRoutePlatform> platforms=new ObjectArrayList<>();
 public ObjectArrayList<SimplifiedRoutePlatform> getPlatforms(){return platforms;}
}''',
    'org/mtr/libraries/it/unimi/dsi/fastutil/longs/LongOpenHashSet.java': '''package org.mtr.libraries.it.unimi.dsi.fastutil.longs;
public final class LongOpenHashSet extends java.util.HashSet<Long> {public boolean add(long value){return super.add(value);}}''',
    'org/mtr/libraries/it/unimi/dsi/fastutil/longs/LongImmutableList.java': '''package org.mtr.libraries.it.unimi.dsi.fastutil.longs;
public final class LongImmutableList {public LongImmutableList(long[] values){}}''',
    'org/mtr/mod/client/MinecraftClientData.java': '''package org.mtr.mod.client;
import java.util.*;import org.mtr.core.data.*;
public final class MinecraftClientData {
 private static final MinecraftClientData INSTANCE=new MinecraftClientData();
 public static MinecraftClientData getInstance(){return INSTANCE;}
 public final Map<Long,Station> stationIdMap=new HashMap<>();public final Map<Long,Platform> platformIdMap=new HashMap<>();
 public final Map<Long,SimplifiedRoute> simplifiedRouteIdMap=new HashMap<>();public final Map<Long,Route> routeIdMap=new HashMap<>();
}''',
    'org/mtr/core/operation/ArrivalResponse.java': '''package org.mtr.core.operation;
import java.util.function.Consumer;import org.mtr.core.data.Route;
public final class ArrivalResponse {
 private final long arrival,departure,routeId;
 public ArrivalResponse(long arrival,long departure,long routeId){this.arrival=arrival;this.departure=departure;this.routeId=routeId;}
 public void iterateCarDetails(Consumer<CarDetails> c){}public String getDestination(){return "Terminus";}
 public long getArrival(){return arrival;}public long getDeparture(){return departure;}public long getDeviation(){return 0;}
 public boolean getRealtime(){return true;}public long getDepartureIndex(){return 0;}public boolean getIsTerminating(){return false;}
 public long getRouteId(){return routeId;}public String getRouteName(){return "Line 1";}public String getRouteNumber(){return "1";}
 public int getRouteColor(){return 1;}public Route.CircularState getCircularState(){return Route.CircularState.NONE;}
 public long getPlatformId(){return 100;}public String getPlatformName(){return "10";}public int getCarCount(){return 1;}
}''',
    'org/mtr/mod/data/ArrivalsCacheClient.java': '''package org.mtr.mod.data;
import org.mtr.core.operation.ArrivalResponse;import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongImmutableList;
public final class ArrivalsCacheClient {
 public static final ArrivalsCacheClient INSTANCE=new ArrivalsCacheClient();
 public final ObjectArrayList<ArrivalResponse> responses=new ObjectArrayList<>();
 public ObjectArrayList<ArrivalResponse> requestArrivals(LongImmutableList ids){return responses;}public long getMillisOffset(){return 0;}
}''',
    'com/lx862/mtrscripting/mod/impl/mtr/vehicle/MagicRouteTypes.java': '''package com.lx862.mtrscripting.mod.impl.mtr.vehicle;
import org.mtr.core.data.TransportMode;
public final class MagicRouteTypes {public static String extendedType(long id,TransportMode mode,int type){return "train";}}''',
    'com/lx862/jcm/mod/scripting/psd/PSDWallScanner.java': '''package com.lx862.jcm.mod.scripting.psd;
import java.util.*;import org.mtr.mapping.holder.BlockPos;import org.mtr.core.data.*;import org.mtr.core.operation.CarDetails;
public final class PSDWallScanner {
 public static final class DoorUnit {public BlockPos pos;public int number;public boolean open,locked;}
 public static final class WallRun {public final List<DoorUnit> doors=new ArrayList<>();public double doorValue;}
 public static List<Integer> predictDoorNumbers(WallRun run,boolean arrived,List<CarDetails> cars,int count,Platform platform,Route route,List<Integer> open){return open;}
}''',
})


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--build', required=True, type=Path)
    parser.add_argument('--java-home', type=Path)
    args = parser.parse_args()
    repo = Path(__file__).resolve().parents[2]
    work = args.build.resolve() / 'psd-wrapper-test'
    sources = []
    for name, content in STUBS.items():
        target = work / 'src' / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(content, encoding='utf-8')
        sources.append(target)
    base = repo / 'fabric/src/main/java'
    for name in ('PSDWrapper', 'PSDRouteStopCache', 'PSDBinding'):
        sources.append(base / 'com/lx862/jcm/mod/scripting/psd' / (name + '.java'))
    for name in ('ArrivalWrapper', 'ArrivalsWrapper'):
        sources.append(base / 'com/lx862/mtrscripting/mod/impl/mtr/pids' / (name + '.java'))
    for name in ('ApiInternal', 'ValueNullable'):
        sources.append(base / 'com/lx862/mtrscripting/core/annotation' / (name + '.java'))
    sources.append(Path(__file__).with_name('java') / 'PSDWrapperIsolationTest.java')
    classes = work / 'classes'
    classes.mkdir(parents=True, exist_ok=True)
    argfile = work / 'sources.txt'
    argfile.write_text('\n'.join('"' + path.as_posix() + '"' for path in sources), encoding='utf-8')
    java_home = args.java_home or os.environ.get('JAVA_HOME')

    def tool(name):
        return str(Path(java_home) / 'bin' / (name + ('.exe' if os.name == 'nt' else ''))) if java_home else name

    subprocess.run([tool('javac'), '-J-Dfile.encoding=UTF-8', '--release', '17', '-encoding', 'UTF-8', '-d', str(classes), '@' + str(argfile)], check=True)
    subprocess.run([tool('java'), '-cp', str(classes), 'com.lx862.jcm.test.PSDWrapperIsolationTest'], check=True)


if __name__ == '__main__':
    main()
