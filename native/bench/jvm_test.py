"""Exercise production Java host/driver through the real JNI bridge without GL.

Only Minecraft/GL dependencies are substituted. NativeScriptManager, NativeHost,
NativeVehicleDriver, NativeDrawRegistry, NativeModelDrawCall and GraphicsTexture
are compiled directly from the production source tree. This catches integration
failures the standalone script smoke test cannot see.
"""
import argparse
import hashlib
import os
from pathlib import Path
import subprocess
import urllib.request

STUBS = {
    'com/lx862/mtrscripting/core/util/ScriptResourceUtil.java': '''package com.lx862.mtrscripting.core.util;
public class ScriptResourceUtil {
 public static java.awt.Font readFont(org.mtr.mapping.holder.Identifier id) {
  String pack=System.getProperty("jcm.test.pack");
  if(pack==null) return getSystemFont("Noto Sans");
  String namespace=id.toString().split(":",2)[0];
  try { return java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT,
    java.nio.file.Path.of(pack,"assets",namespace,id.getPath()).toFile()); }
  catch(Exception e) { throw new AssertionError("Missing requested resource font "+id,e); }
 }
 public static java.awt.Font getSystemFont(String name) {
  try { return java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, new java.io.File(System.getProperty("jcm.test.font"))); }
  catch (Exception e) { throw new AssertionError("Failed to load the bundled JCM CJK font", e); }
 }
}''',
    'com/lx862/jcm/mod/util/JCMLogger.java': '''package com.lx862.jcm.mod.util;
public class JCMLogger {
 public static void info(String s, Object... args) { System.out.println(s + java.util.Arrays.toString(args)); }
 public static void warn(String s, Object... args) { info(s, args); }
 public static void error(String s, Object... args) { throw new AssertionError(s + java.util.Arrays.toString(args)); }
 public static void debug(String s, Object... args) {}
}''',
    'org/mtr/mapping/holder/Identifier.java': '''package org.mtr.mapping.holder;
public record Identifier(String value) {
 public String getPath() { return value.substring(value.indexOf(':') + 1); }
 public String toString() { return value; }
}''',
    'org/mtr/mapping/mapper/ResourceManagerHelper.java': '''package org.mtr.mapping.mapper;
import org.mtr.mapping.holder.Identifier;
public class ResourceManagerHelper {
 public static java.nio.file.Path root;
 public static void readAllResources(Identifier id, java.util.function.Consumer<java.io.InputStream> reader) {
  var source=id.toString().startsWith("legacy:") ? java.nio.file.Path.of(System.getProperty("jcm.test.legacy6")) : root;
  try { reader.accept(java.nio.file.Files.newInputStream(source.resolve(id.getPath()))); }
  catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
 }
}''',
    'com/lx862/mtrscripting/core/annotation/ApiInternal.java': '''package com.lx862.mtrscripting.core.annotation;
public @interface ApiInternal {}''',
    'com/lx862/mtrscripting/mod/MTRScriptingMod.java': '''package com.lx862.mtrscripting.mod;
public class MTRScriptingMod {
 public static org.mtr.mapping.holder.Identifier id(String s) { return new org.mtr.mapping.holder.Identifier("test:" + s); }
}''',
    'com/mojang/blaze3d/platform/GlStateManager.java': '''package com.mojang.blaze3d.platform;
public class GlStateManager { public static void _bindTexture(int n) {} }''',
    'com/mojang/blaze3d/systems/RenderSystem.java': '''package com.mojang.blaze3d.systems;
public class RenderSystem { public static void recordRenderCall(Runnable r) { r.run(); } }''',
    'org/lwjgl/opengl/GL33.java': '''package org.lwjgl.opengl;
public class GL33 {
 public static final int GL_TEXTURE_BINDING_2D=0, GL_TEXTURE_2D=0, GL_TEXTURE_SWIZZLE_RGBA=0, GL_BLUE=0, GL_GREEN=0, GL_RED=0, GL_ALPHA=0;
 public static int glGetInteger(int n) { return 0; }
 public static void glTexParameteriv(int a, int b, int[] c) {}
}''',
    'org/lwjgl/system/MemoryUtil.java': '''package org.lwjgl.system;
public class MemoryUtil {
 public static final java.util.Map<Long, java.nio.ByteBuffer> buffers = new java.util.HashMap<>();
 public static java.nio.ByteBuffer memByteBuffer(long p, int n) { return buffers.get(p).duplicate().order(java.nio.ByteOrder.nativeOrder()); }
}''',
    'org/mtr/mapping/holder/NativeImage.java': '''package org.mtr.mapping.holder;
public class NativeImage {
 private static long next=1; public final long pointer=next++;
 public NativeImage(int w, int h, boolean b) { org.lwjgl.system.MemoryUtil.buffers.put(pointer, java.nio.ByteBuffer.allocateDirect(w*h*4)); }
 public void upload(int a,int b,int c,int d,int e,int f,int g,boolean h,boolean i,boolean j,boolean k) {}
}''',
    'org/mtr/mapping/holder/NativeImageBackedTexture.java': '''package org.mtr.mapping.holder;
public class NativeImageBackedTexture {
 public final NativeImage data; public NativeImageBackedTexture(NativeImage image) { data=image; }
 public NativeImage getImage() { return data; } public void bindTexture() {} public void upload() {}
}''',
    'org/mtr/mapping/holder/AbstractTexture.java': '''package org.mtr.mapping.holder;
public class AbstractTexture { public AbstractTexture(NativeImage n) {} }''',
    'com/lx862/jcm/mapping/LoaderImplClient.java': '''package com.lx862.jcm.mapping;
public class LoaderImplClient { public static long getNativeImagePointer(org.mtr.mapping.holder.NativeImage n) { return n.pointer; } }''',
    'org/mtr/mapping/holder/MinecraftClient.java': '''package org.mtr.mapping.holder;
public class MinecraftClient {
 private static final MinecraftClient instance=new MinecraftClient();
 public static MinecraftClient getInstance() { return instance; } public void execute(Runnable r) { r.run(); }
 public Object getWorldMapped() { return new World(); }
 public MinecraftClient getTextureManager() { return this; }
 public void registerTexture(Identifier id, AbstractTexture t) {} public void destroyTexture(Identifier id) {}
}''',
    'org/mtr/mapping/holder/World.java': '''package org.mtr.mapping.holder;
public class World { public static World cast(Object o) { return (World)o; } }''',
    'org/mtr/mapping/holder/Direction.java': '''package org.mtr.mapping.holder;
public enum Direction { NORTH }''',
    'org/mtr/mod/render/StoredMatrixTransformations.java': '''package org.mtr.mod.render;
public class StoredMatrixTransformations {
 public double y; public StoredMatrixTransformations copy() { var copy=new StoredMatrixTransformations(); copy.y=y; return copy; }
 public void add(java.util.function.Consumer<StoredMatrixTransformations> operation) { operation.accept(this); }
 public void translate(double x,double y,double z) { this.y+=y; }
}''',
    'com/lx862/mtrscripting/core/util/model/RawMeshBuilderJS.java': '''package com.lx862.mtrscripting.core.util.model;
public class RawMeshBuilderJS {
 public final java.util.List<Float> vertices=new java.util.ArrayList<>(), uv=new java.util.ArrayList<>();
 public final org.mtr.mapping.holder.Identifier texture;
 public RawMeshBuilderJS(int n,String stage,org.mtr.mapping.holder.Identifier texture) { this.texture=texture; }
 public void vertex(double x,double y,double z) { vertices.add((float)x); vertices.add((float)y); vertices.add((float)z); }
 public void normal(float x,float y,float z) {} public void uv(float u,float v) { uv.add(u); uv.add(v); } public void endVertex() {}
 public Object asRawModel() { return this; }
}''',
    'com/lx862/mtrscripting/core/util/model/RawModelJS.java': '''package com.lx862.mtrscripting.core.util.model;
public record RawModelJS(Object data) {}''',
    'com/lx862/mtrscripting/core/util/model/ModelManagerJS.java': '''package com.lx862.mtrscripting.core.util.model;
public class ModelManagerJS { public static ModelJS upload(RawModelJS raw) { return new ModelJS((RawMeshBuilderJS)raw.data()); } }''',
    'com/lx862/mtrscripting/core/util/model/ModelJS.java': '''package com.lx862.mtrscripting.core.util.model;
public class ModelJS {
 public static final java.util.List<ModelJS> all=new java.util.ArrayList<>();
 public final RawMeshBuilderJS geometry; public boolean closed; public int draws; public double lastY;
 public ModelJS(RawMeshBuilderJS geometry) { this.geometry=geometry; all.add(this); }
 public void close() { closed=true; }
 public void draw(org.mtr.mod.render.StoredMatrixTransformations transform,int light) { if(closed) throw new AssertionError("closed model replay"); draws++; lastY=transform.y; }
}''',
    'com/lx862/mtrscripting/core/util/render/RenderDrawCall.java': '''package com.lx862.mtrscripting.core.util.render;
public class RenderDrawCall<T> {
 public void validate() {} public void run(org.mtr.mapping.holder.World w,org.mtr.mod.render.StoredMatrixTransformations m,org.mtr.mapping.holder.Direction d,int light) {}
}''',
    'com/lx862/mtrscripting/core/util/render/ScriptRenderManager.java': '''package com.lx862.mtrscripting.core.util.render;
public class ScriptRenderManager {
 public final java.util.List<RenderDrawCall<?>> calls=new java.util.ArrayList<>();
 public void draw(RenderDrawCall<?> c) { c.validate(); calls.add(c); } public void reset() { calls.clear(); }
 public void invoke(org.mtr.mapping.holder.World w,org.mtr.mod.render.StoredMatrixTransformations m,org.mtr.mapping.holder.Direction d,int light) { for(var c:calls) c.run(w,m.copy(),d,light); }
}''',
    'org/mtr/mod/data/VehicleExtension.java': '''package org.mtr.mod.data;
public class VehicleExtension {
 public final Extra vehicleExtraData=new Extra(); public String getHexId() { return "vehicle"; }
 public static class Extra { public final java.util.List<Car> immutableVehicleCars=new java.util.ArrayList<>(); }
 public static class Car { public String getVehicleId() { return "car"; } }
}''',
    'com/lx862/mtrscripting/mod/impl/mtr/vehicle/VehicleScriptContext.java': '''package com.lx862.mtrscripting.mod.impl.mtr.vehicle;
public class VehicleScriptContext { public enum DataFetchMode { SKIP, ALL, MANDATORY } }''',
    'com/lx862/mtrscripting/mod/impl/mtr/vehicle/VehicleWrapper.java': '''package com.lx862.mtrscripting.mod.impl.mtr.vehicle;
public class VehicleWrapper {
 public int count=3; public String siding="10010/01-02-03";
 public boolean[] doorLeftOpen=new boolean[8], doorRightOpen=new boolean[8];
 public final java.util.List<Stop> all=new java.util.ArrayList<>(), current=new java.util.ArrayList<>(), next=new java.util.ArrayList<>();
 public VehicleWrapper(VehicleScriptContext.DataFetchMode mode,org.mtr.mod.data.VehicleExtension v) {}
 public boolean isStopsDataFullyFetched() { return true; }
 public java.util.List<Stop> getStops() { return all; } public java.util.List<Stop> getThisRouteStops() { return current; } public java.util.List<Stop> getNextRouteStops() { return next; }
 public int getCarCount() { return count; } public double getLength(int i) { return 25; } public double getWidth(int i) { return 3; }
 public boolean isCarRendered(int i) { return true; } public String getVehicleId(int i) { return "car"+i; }
 public org.mtr.core.data.Siding getSiding() { return new org.mtr.core.data.Siding(siding); }
 public long getId() { return 17; } public long getDepartureIndex() { return 3; }
 public org.mtr.core.data.Route.CircularState getTransportMode() { return org.mtr.core.data.Route.CircularState.NONE; }
 public double getSpeedKmh() { return 0; } public double getSpeedMs() { return 0; } public double getRailProgress() { return 0; } public double getDoorValue() { return 0; } public int getNotchLevel() { return 0; }
 public boolean isReversed() { return false; } public boolean isOnRoute() { return true; } public boolean isDoorOpening() { return false; }
 public boolean isCurrentlyManual() { return false; } public boolean isManualAllowed() { return false; } public boolean isClientPlayerRiding() { return false; } public boolean isRendered() { return true; }
 public double getTotalDwellTime() { return 15000; } public double getElapsedDwellTime() { return 0; }
 public int getNextStopIndex(java.util.List<Stop> s) { return 0; }
 public static class Stop {
  public String name="Station", destinationName="Terminal", customDestination;
  public org.mtr.core.data.SimplifiedRoute route; public org.mtr.core.data.Station station; public org.mtr.core.data.Siding platform=new org.mtr.core.data.Siding("platform");
  public double distance, dwellTimeMillis=15000; public boolean isRouteSwitchoverStop;
  public java.util.List<RouteInterchange> routeInterchanges=new java.util.ArrayList<>();
  public static class RouteInterchange { public String name="Interchange"; public int color=0xff009bc0; }
 }
}''',
    'org/mtr/core/data/Route.java': '''package org.mtr.core.data;
public class Route { public enum CircularState { NONE, CLOCKWISE, ANTICLOCKWISE } }''',
    'org/mtr/core/data/Siding.java': '''package org.mtr.core.data;
public record Siding(String name) { public long getId() { return 123; } public String getName() { return name; } }''',
    'org/mtr/core/data/SimplifiedRoute.java': '''package org.mtr.core.data;
public record SimplifiedRoute(long id,String name) {
 public long getId() { return id; } public String getName() { return name; } public int getColor() { return 0x009bc0; }
 public Route.CircularState getCircularState() { return Route.CircularState.NONE; }
}''',
    'org/mtr/libraries/it/unimi/dsi/fastutil/objects/ObjectArrayList.java': '''package org.mtr.libraries.it.unimi.dsi.fastutil.objects;
public class ObjectArrayList<T> extends java.util.ArrayList<T> {}''',
    'org/mtr/core/data/StationExit.java': '''package org.mtr.core.data;
public class StationExit {
 public String getName() { return "A"; }
 public org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList<String> getDestinations() {
  var list=new org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList<String>(); list.add("Museum"); list.add("Park"); return list;
 }
}''',
    'org/mtr/core/data/Station.java': '''package org.mtr.core.data;
public class Station {
 public long getId() { return 42; }
 public org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList<StationExit> getExits() {
  var list=new org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList<StationExit>(); list.add(new StationExit()); return list;
 }
}''',
    'com/lx862/mtrscripting/mod/resource/VehicleResourceProvider.java': '''package com.lx862.mtrscripting.mod.resource;
public class VehicleResourceProvider {
 public String getVehicleScriptEntryId(String id) { return "wr2a03"; }
 public VehicleScriptConfiguration getVehicleScript(String id) { return new VehicleScriptConfiguration(com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleScriptContext.DataFetchMode.ALL); }
 public record VehicleScriptConfiguration(com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleScriptContext.DataFetchMode dataFetchMode) {}
}''',
    'com/lx862/mtrscripting/mod/resource/MtrScriptingResourceManager.java': '''package com.lx862.mtrscripting.mod.resource;
public class MtrScriptingResourceManager { public static final VehicleResourceProvider vehicle=new VehicleResourceProvider(); }''',
}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--build', required=True, type=Path)
    parser.add_argument('--java-home', type=Path, default=os.environ.get('JAVA_HOME'))
    parser.add_argument('--source-repo', type=Path)
    parser.add_argument('--platform', default=os.environ.get('NATIVE_TEST_PLATFORM'))
    parser.add_argument('--resource-pack', type=Path, help='Validate the actual WR2-A03 resource fonts and save texture previews')
    parser.add_argument('--legacy6-directory', type=Path, help='Validate already compiled ABI 6 WR2-A03 modules with the new host')
    args = parser.parse_args()
    repo = args.source_repo or Path(__file__).resolve().parents[2]
    build = args.build.resolve()
    work = build / 'jvm-test'
    work.mkdir(exist_ok=True, parents=True)
    gson = work / 'gson-2.11.0.jar'
    if not gson.exists():
        urllib.request.urlretrieve('https://repo.maven.apache.org/maven2/com/google/code/gson/gson/2.11.0/gson-2.11.0.jar', gson)
    if hashlib.sha256(gson.read_bytes()).hexdigest() != '57928d6e5a6edeb2abd3770a8f95ba44dce45f3b23b7a9dc2b309c581552a78b':
        raise RuntimeError('Gson dependency checksum mismatch')
    sources = []
    for path, content in STUBS.items():
        file = work / 'src' / path
        file.parent.mkdir(exist_ok=True, parents=True)
        file.write_text(content, encoding='utf-8')
        sources.append(file)
    main_source = Path(__file__).with_name('jvm') / 'NativeIntegrationTest.java'
    sources.append(main_source)
    sources.append(main_source.with_name('NativeFontTest.java'))
    base = repo / 'fabric/src/main/java'
    sources.extend(base / ('com/lx862/jcm/nativeapi/' + name + '.java') for name in [
        'NativeScriptManager', 'NativeHost', 'NativeFontRenderer', 'NativeVehicleDriver', 'NativeDrawRegistry', 'NativeModelDrawCall', 'NativeSnapshot'])
    sources.append(base / 'com/lx862/mtrscripting/core/util/GraphicsTexture.java')
    classes = work / 'classes'
    classes.mkdir(exist_ok=True)
    def java_tool(name):
        return str(Path(args.java_home) / 'bin' / (name + ('.exe' if os.name == 'nt' else ''))) if args.java_home else name
    argfile = work / 'sources.txt'
    argfile.write_text('\n'.join('"' + str(p).replace('\\', '/') + '"' for p in sources), encoding='utf-8')
    subprocess.run([java_tool('javac'), '-encoding', 'UTF-8', '-cp', str(gson), '-d', str(classes), '@' + str(argfile)], check=True)
    libs = build / 'Release' if (build / 'Release').is_dir() else build
    # Match the layout inside the release jars, so loading the bundled bridge
    # is tested without relying on java.library.path or a manually copied DLL.
    import platform
    key = args.platform or ('windows-x64' if os.name == 'nt' else ('macos-' + ('arm64' if platform.machine() == 'arm64' else 'x64') if platform.system() == 'Darwin' else 'linux-x64'))
    bridge_name = 'jcm_native_bridge.dll' if os.name == 'nt' else ('libjcm_native_bridge.dylib' if platform.system() == 'Darwin' else 'libjcm_native_bridge.so')
    target = classes / 'assets/jcm/natives' / key / bridge_name
    target.parent.mkdir(parents=True, exist_ok=True)
    import shutil
    shutil.copy2(libs / bridge_name, target)
    layout = work / 'abi-layout.txt'
    with layout.open('w', encoding='utf-8') as output:
        subprocess.run([str(libs / ('native_abi_layout.exe' if os.name == 'nt' else 'native_abi_layout'))], stdout=output, check=True)
    font = repo / 'fabric/src/main/resources/assets/mtr/font/noto-sans-cjk-tc-medium.otf'
    options = ['-Djcm.test.output=' + str(work)]
    if args.resource_pack:
        options.append('-Djcm.test.pack=' + str(args.resource_pack.resolve()))
    if args.legacy6_directory:
        options.append('-Djcm.test.legacy6=' + str(args.legacy6_directory.resolve()))
    subprocess.run([java_tool('java'), '-Xmx1G', '-Djava.awt.headless=true', '-Djcm.test.font=' + str(font), *options, '-cp', str(classes) + os.pathsep + str(gson), 'com.lx862.jcm.nativeapi.NativeIntegrationTest', str(libs), str(layout)], check=True)


if __name__ == '__main__':
    main()
