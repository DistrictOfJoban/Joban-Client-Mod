package com.lx862.jcm.nativeapi;
import com.google.gson.*;
import java.awt.image.BufferedImage;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
public class NativeIntegrationTest {
 static int checks;
 static void check(boolean ok,String name){checks++;if(!ok)throw new AssertionError(name);}
 public static class Fixture {
  final BufferedImage image=new BufferedImage(8,8,BufferedImage.TYPE_INT_ARGB);
  final List<String> marks=new CopyOnWriteArrayList<>(); final CountDownLatch latch=new CountDownLatch(1);
  Consumer<Integer> retained;
  public long largeId(){return 9223372036854775000L;}
  public int[] array(){return new int[]{10,20,30};}
  public String join(String... values){return String.join("/",values);}
  public boolean config(com.lx862.mtrscripting.lib.org.mozilla.javascript.NativeObject config){
   return config.get("image")==image&&((com.lx862.mtrscripting.lib.org.mozilla.javascript.NativeObject)config.get("nested")).get("label").equals("配置");
  }
  public BufferedImage image(){return image;}
  public java.awt.Graphics2D graphics(){return image.createGraphics();}
  public void consume(Consumer<Integer> callback){retained=callback;callback.accept(7);}
  public void backgroundDone(String text){check(text.equals("异步"),"background UTF-8");latch.countDown();}
  public void mark(String mark){marks.add(mark);}
  public void setAutoZOrdering(boolean on){check(!on,"C++ PIDS z ordering setting");}
  public void setZOrderStep(double step){check(step==0.001,"C++ PIDS z order step");}
 }
 static JsonElement call(NativeApiSession s,JsonObject request){JsonObject reply=JsonParser.parseString(s.invoke(request.toString())).getAsJsonObject();if(reply.has("error"))throw new AssertionError(reply);return reply.get("value");}
 public static void main(String[] args)throws Exception {
  Path libs=Path.of(args[0]);org.mtr.mapping.mapper.ResourceManagerHelper.root=libs;
  String ext=System.getProperty("os.name").toLowerCase().contains("win")?".dll":System.getProperty("os.name").toLowerCase().contains("mac")?".dylib":".so";
  String prefix=ext.equals(".dll")?"":"lib";
  var module=NativeScriptManager.loadFromDeclaration("test:probe",new JsonPrimitive("test:"+prefix+"api_probe"+ext));
  check(module!=null&&module.getAbiVersion()==9,"ABI 9 loaded from resource declaration");
  Fixture first=new Fixture(),second=new Fixture();
  NativeScriptManager.configure("test:probe",JsonParser.parseString("{\"message\":\"原生 API\"}"),"test:natives/api_probe");
  ByteBuffer snapshot=ByteBuffer.allocateDirect(184).order(ByteOrder.nativeOrder());snapshot.limit(184);
  for(int i=0;i<2;i++){
   Fixture fixture=i==0?first:second;String key="fixture"+i;
   var frames=NativeScriptManager.render(key,"test:probe",snapshot,0,m->fixture,fixture,null);
   check(frames.size()==1,"real C++ -> JNI -> Java API frame");
   check(fixture.latch.await(5,TimeUnit.SECONDS),"real asynchronous C++ callback");
   check(fixture.image.getRGB(1,1)==0xffff0000,"C++ drawing into actual Java2D image");
   check(fixture.marks.equals(List.of("create","render")),"create/render order");
   NativeScriptManager.render(key,"test:probe",snapshot,0,m->fixture,fixture,null);
   check(fixture.marks.equals(List.of("create","render","render")),"persistent instance state");
  }
  check(first.marks.size()==3&&second.marks.size()==3,"instances are independent");
  NativeScriptManager.disposeInstance("fixture0","test:probe");
  check(first.marks.equals(List.of("create","render","render","dispose")),"dispose can still call the API");
  first.retained.accept(1);
  check(first.marks.size()==4,"retained callback is disabled before state destruction");
  NativeScriptManager.reload();
  check(second.marks.equals(List.of("create","render","render","dispose")),"reload disposes each instance with live APIs");
  check(!NativeScriptManager.isLoaded("test:probe"),"reload unloads module");
  for(int kind=1;kind<=3;kind++) {
   String id="test:content"+kind;
   module=NativeScriptManager.loadFromDeclaration(id,new JsonPrimitive("test:"+prefix+"content_probe_"+kind+ext));
   check(module!=null,"load content script "+kind);
   ByteBuffer data=ByteBuffer.allocateDirect(8192).order(ByteOrder.nativeOrder());
   Object wrapper;
   if(kind==1) {
    var pids=new com.lx862.jcm.mod.scripting.pids.PIDSWrapper(); wrapper=pids; NativeContentSnapshot.pids(data,pids);
    check(data.getLong(24)==9223372036854775000L,"PIDS station ID");
    check(data.getLong(data.getInt(88))==9223372036854775000L,"PIDS platform ID");
   } else if(kind==2) {
    var eye=new com.lx862.mtrscripting.mod.impl.mtr.eyecandy.EyecandyBlockEntityWrapper();wrapper=eye;NativeContentSnapshot.eyecandy(data,eye,true);
    check(data.get(65)==5&&data.getFloat(56)==0.2f,"Eyecandy facing and rotation");
   } else {
    var lift=new com.lx862.mtrscripting.mod.impl.mtr.lift.LiftWrapper();wrapper=lift;NativeContentSnapshot.lift(data,lift);
    check(data.getDouble(48)==4&&data.getLong(0)==9223372036854775000L,"Lift offset and ID");
   }
   Fixture context=new Fixture();
   var frames=NativeScriptManager.render(id,id,data,kind,m->context,wrapper,null);
   check(frames.size()==1,"content script create/render "+kind);
   check(context.marks.equals(List.of("create","render")),"content lifecycle "+kind);
   var frame=frames.get(0);frame.records.order(ByteOrder.nativeOrder());
   check(frame.recordCount==(kind==2?3:1),"content draw records "+kind);
   if(kind==2) {
    check(frame.floatArena.limit()==48,"both create-time shapes retained");
    check(frame.records.getInt(16)==0&&frame.records.getInt(40)==24,"shape byte offsets");
   }
   NativeScriptManager.disposeInstance(id,id);
   check(context.marks.equals(List.of("create","render","dispose")),"content dispose "+kind);
  }
  NativeScriptManager.reload();
  for(int abi=6;abi<=7;abi++) {
   String id="test:legacy"+abi;
   module=NativeScriptManager.loadFromDeclaration(id,new JsonPrimitive("test:"+prefix+"legacy_probe_"+abi+ext));
   check(module!=null&&module.getAbiVersion()==abi,"old ABI accepted "+abi);
   check(NativeScriptManager.renderVehicle(id,id,new NativeScriptManager.VehicleSnapshotBuffers(),
     new com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleWrapper(),m->new Fixture())
     .size()==1,"old ABI renders "+abi);
   NativeScriptManager.disposeInstance(id,id);
  }
  NativeScriptManager.reload();
  System.out.println("Native API real JNI integration passed: "+checks+" checks");
 }
}
