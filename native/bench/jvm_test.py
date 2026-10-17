"""Production manager/API bridge through actual JNI; Minecraft services are fixtures."""
import argparse, hashlib, os, platform, shutil, subprocess, urllib.request
from content_fixtures import STUBS as CONTENT_STUBS
from pathlib import Path
STUBS = {
 'com/lx862/mtrscripting/core/util/TimingJS.java': 'package com.lx862.mtrscripting.core.util; public class TimingJS { public static double globalElapsed(){return System.nanoTime()/1e9;} }',
 'com/lx862/jcm/mod/util/JCMLogger.java': 'package com.lx862.jcm.mod.util; public class JCMLogger { public static void info(String s,Object... a) {} public static void warn(String s,Object... a) {System.out.println(s);} public static void error(String s,Object... a) {throw new AssertionError(s+java.util.Arrays.toString(a));} public static void debug(String s,Object... a) {} }',
 'org/mtr/mapping/holder/Identifier.java': 'package org.mtr.mapping.holder; public record Identifier(String value) { public String getPath(){return value.substring(value.indexOf(":")+1);} public String getNamespace(){return value.substring(0,value.indexOf(":"));} public String toString(){return value;} }',
 'org/mtr/mapping/mapper/ResourceManagerHelper.java': 'package org.mtr.mapping.mapper; public class ResourceManagerHelper { public static java.nio.file.Path root; public static void readAllResources(org.mtr.mapping.holder.Identifier id,java.util.function.Consumer<java.io.InputStream> callback) {try(var input=java.nio.file.Files.newInputStream(root.resolve(id.getPath()))){callback.accept(input);}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}} }',
 'com/lx862/jcm/nativeapi/NativeVehicleDriver.java': 'package com.lx862.jcm.nativeapi; public class NativeVehicleDriver { public static void reset(){} }',
 'com/lx862/jcm/nativeapi/NativeContentDriver.java': 'package com.lx862.jcm.nativeapi; public class NativeContentDriver { public static void reset(){} }',
 'com/lx862/jcm/nativeapi/NativeSnapshot.java': 'package com.lx862.jcm.nativeapi; public class NativeSnapshot { public static final int CAPACITY=262144; public static int groupOf(int abi){return abi>=9?9:abi==8?8:7;} public static java.nio.ByteBuffer build(java.nio.ByteBuffer b,com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleWrapper w,int abi){return b;} public static java.nio.ByteBuffer build(java.nio.ByteBuffer b,com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleWrapper w,int abi,Builder builder){return b;} public static class Builder {} }',
 'com/lx862/mtrscripting/mod/impl/mtr/vehicle/VehicleWrapper.java': 'package com.lx862.mtrscripting.mod.impl.mtr.vehicle; public class VehicleWrapper {}',
 'com/lx862/mtrscripting/lib/org/mozilla/javascript/NativeObject.java': 'package com.lx862.mtrscripting.lib.org.mozilla.javascript; public class NativeObject extends java.util.HashMap<Object,Object> {public void put(String key,NativeObject owner,Object value){put(key,value);}}',
 'com/lx862/mtrscripting/core/util/ConsoleJS.java': 'package com.lx862.mtrscripting.core.util; public class ConsoleJS {public static void log(String... args){} public void count(String name){} }',
 'org/mtr/core/data/TransportMode.java': 'package org.mtr.core.data; public enum TransportMode {TRAIN,BOAT,CABLE_CAR,AIRPLANE}',
 'com/lx862/jcm/nativeapi/NativeHost.java': """package com.lx862.jcm.nativeapi;
 public class NativeHost {
  private static final NativeHost INSTANCE=new NativeHost(); public static NativeHost get(){return INSTANCE;}
  public void reset(){} public void setPendingInstance(String key){} public void releaseInstance(String key){}
  public int[] createResources(int[] widths,int[] heights,float[] vertices,float[] uv,int[] slots,int[] stages){return new int[slots.length];}
  public void uploadPixels(int handle,int x,int y,int w,int h,byte[] pixels){}
  public int rasterizeText(byte[] text,int x,int y,int width,int r,int g,int b,java.nio.ByteBuffer data,int w,int h){return -1;}
  public double[] measureFontText(byte[] font,byte[] text,double size,double x,double y){return new double[]{0,0,0,0};}
  public int[] drawFontText(byte[] font,byte[] text,double size,double x,double base,double sx,double sy,double compress,int argb,int x0,int y0,int x1,int y1,java.nio.ByteBuffer data,int w,int h){return new int[]{0,0,0,0};}
 }
 """,
}
def main():
 p=argparse.ArgumentParser();p.add_argument('--build',required=True,type=Path);a=p.parse_args()
 repo=Path(__file__).resolve().parents[2]; build=a.build.resolve(); work=build/'api-jvm-test'; work.mkdir(exist_ok=True,parents=True)
 gson=work/'gson.jar'
 if not gson.exists(): urllib.request.urlretrieve('https://repo.maven.apache.org/maven2/com/google/code/gson/gson/2.11.0/gson-2.11.0.jar',gson)
 assert hashlib.sha256(gson.read_bytes()).hexdigest()=='57928d6e5a6edeb2abd3770a8f95ba44dce45f3b23b7a9dc2b309c581552a78b'
 sources=[]
 STUBS.update(CONTENT_STUBS)
 for path,content in STUBS.items():
  f=work/'src'/path;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(content,encoding='utf-8');sources.append(f)
 base=repo/'fabric/src/main/java/com/lx862/jcm/nativeapi'
 sources += [base/'NativeScriptManager.java',base/'NativeApiSession.java',base/'NativeContentSnapshot.java',Path(__file__).with_name('jvm')/'NativeIntegrationTest.java']
 classes=work/'classes'; classes.mkdir(exist_ok=True)
 def tool(name): return str(Path(os.environ['JAVA_HOME'])/'bin'/(name+('.exe' if os.name=='nt' else ''))) if os.environ.get('JAVA_HOME') else name
 argfile=work/'sources.txt';argfile.write_text('\n'.join('"'+str(s).replace('\\','/')+'"' for s in sources),encoding='utf-8')
 subprocess.run([tool('javac'),'-encoding','UTF-8','-cp',str(gson),'-d',str(classes),'@'+str(argfile)],check=True)
 libs=build/'Release' if (build/'Release').is_dir() else build
 key=os.environ.get('NATIVE_TEST_PLATFORM') or ('windows-x64' if os.name=='nt' else ('macos-'+('arm64' if platform.machine()=='arm64' else 'x64') if platform.system()=='Darwin' else 'linux-x64'))
 bridge='jcm_native_bridge.dll' if os.name=='nt' else 'libjcm_native_bridge.'+('dylib' if platform.system()=='Darwin' else 'so')
 target=classes/'assets/jcm/natives'/key/bridge;target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(libs/bridge,target)
 subprocess.run([tool('java'),'-Djava.awt.headless=true','-cp',str(classes)+os.pathsep+str(gson),'com.lx862.jcm.nativeapi.NativeIntegrationTest',str(libs)],check=True)
if __name__=='__main__': main()
