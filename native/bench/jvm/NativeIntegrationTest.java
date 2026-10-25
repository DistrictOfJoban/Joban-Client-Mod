package com.lx862.jcm.nativeapi;
import com.google.gson.*;
import java.awt.image.BufferedImage;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
/**
 * Headless integration test for the REAL production pipeline (real bridge,
 * real C++ script modules, real shared memory), run by jvm_test.py on every
 * CI platform in BOTH invocation-plane configurations:
 *
 *   - the Panama FFM plane (JDK 22+ final API, or 21 with --enable-preview
 *     plus -Djcm.ffm.allow_preview=true): libraryLookup loading, jcm_module_*
 *     lifecycle downcalls, jcm_shm_kick per-frame trigger, upcall-stub host
 *     services, jcm_module_invoke_api api re-entry — a fully JNI-free
 *     session that never touches System.load.
 *
 *   - the shared-memory poller plane (every JVM, and explicitly exercised
 *     via -Djcm.ffm.disable / by running without preview flags): the
 *     bridge is loaded ONLY for its constructor, its poller thread finds
 *     the region through the filesystem handshake, module lifecycle and
 *     renders travel the control slot as requests, host services come
 *     back as SVC requests this test's waiting threads answer, and api
 *     re-entry travels the CBQ ring — also a fully JNI-free session, with
 *     ZERO native calls in either direction.
 *
 * Phase A below is plane-agnostic: every call site is identical on both
 * planes (that is the point of the channel abstraction). The last sections
 * assert the plane-specific observables.
 */
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
  /* ---------------- Phase A: the full pipeline (plane-agnostic) --------
     loadFromDeclaration loads the bridge (pure FFM on 22+ without
     -Djcm.ffm.disable; System.load + poller otherwise) and — on the
     poller plane — creates the shared region and waits for the bridge's
     poller thread to attach. The plane detection happens right after. */
  var module=NativeScriptManager.loadFromDeclaration("test:probe",new JsonPrimitive("test:"+prefix+"api_probe"+ext));
  final boolean isPollerPlane=!PanamaFfmChannel.ready();
  System.out.println("Session plane: "+(isPollerPlane?"shared-memory poller (no JNI)":"Panama FFM (no JNI)"));
  if(isPollerPlane) check(PanamaFfmChannel.reason()!=null&&!PanamaFfmChannel.reason().isEmpty(),"poller-plane session: FFM unavailability has a reason: "+PanamaFfmChannel.reason());
  else check(PanamaFfmChannel.hostReady(),"FFM session: host upcall vtable installed");
  check(module!=null&&module.getAbiVersion()==11,"ABI 11 loaded from resource declaration");
  Fixture first=new Fixture(),second=new Fixture();
  NativeScriptManager.configure("test:probe",JsonParser.parseString("{\"message\":\"原生 API\"}"),"test:natives/api_probe");
  ByteBuffer snapshot=ByteBuffer.allocateDirect(184).order(ByteOrder.nativeOrder());snapshot.limit(184);
  for(int i=0;i<2;i++){
   Fixture fixture=i==0?first:second;String key="fixture"+i;
   var frames=NativeScriptManager.render(key,"test:probe",snapshot,0,m->fixture,fixture,null);
   check(frames.size()==1,"real C++ -> Java API frame (host services crossed the plane)");
   check(fixture.latch.await(5,TimeUnit.SECONDS),"real asynchronous C++ callback (api re-entry crossed the plane)");
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
  /* Yanyang regression ("LCD无法显示"): the vehicle driver sweeps states
     whose train left the camera frustum for >5s, disposing the bridge
     instance — and the bridge forgets the shm instance id together with it.
     The Java-side id cache (NativeChannel.INSTANCE_IDS) used to keep the
     stale id, so every later frame of that vehicle shipped a dead id, the
     bridge answered ERR_INSTANCE ("instance not registered") and both
     modules returned null — forever, no self-healing. Disposing must
     forget the cached id; the next render re-registers it and the script
     starts over (fresh create). */
  var revived=NativeScriptManager.render("fixture0","test:probe",snapshot,0,m->first,first,null);
  check(revived.size()==1,"re-render after dispose re-registers the shm instance id (stale-id regression)");
  check(first.marks.equals(List.of("create","render","render","dispose","create","render")),"revived instance is a fresh create");
  NativeScriptManager.disposeInstance("fixture0","test:probe");
  check(first.marks.equals(List.of("create","render","render","dispose","create","render","dispose")),"revived instance disposes cleanly");
  /* ---- shared-memory transport specifics ---- */
  check(NativeChannel.activeTransport()==NativeChannel.Transport.SHARED_MEMORY,"default transport is shared memory");
  ByteBuffer windowA=NativeChannel.newSnapshotBuffer(4096);
  long addressA=NativeArenaView.addressOf(windowA);
  check(addressA!=0,"snapshot buffer lives in native memory");
  NativeChannel.releaseSnapshotBuffer(windowA);
  ByteBuffer windowB=NativeChannel.newSnapshotBuffer(4096);
  check(NativeArenaView.addressOf(windowB)==addressA,"released shm window is immediately reused");
  NativeChannel.releaseSnapshotBuffer(windowB);
  /* The same LIVE instance must keep its state across a transport switch:
     shm renders so far, one direct-call render, then back to shm. On the
     FFM plane the direct call is the jcm_module_render downcall; on the
     poller plane it degrades to the same shared-memory request path. */
  NativeChannel.configure(NativeChannel.Transport.JNI);
  var directFrames=NativeScriptManager.render("fixture1","test:probe",snapshot,0,m->second,second,null);
  check(directFrames.size()==1,"direct-call transport renders the same live instance");
  check(second.marks.equals(List.of("create","render","render","render")),"direct render reuses instance state");
  NativeChannel.configure(NativeChannel.Transport.SHARED_MEMORY);
  var shmFrames=NativeScriptManager.render("fixture1","test:probe",snapshot,0,m->second,second,null);
  check(shmFrames.size()==1,"shared memory transport resumes");
  check(second.marks.equals(List.of("create","render","render","render","render")),"shm render reuses instance state");
  NativeScriptManager.reload();
  check(second.marks.equals(List.of("create","render","render","render","render","dispose")),"reload disposes each instance with live APIs");
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

  /* ---------------- Phase B: plane-specific observables --------------- */
  if(isPollerPlane) {
   /* ---- shared-memory poller plane ---- */
   /* The reload above tore the region down; the next module load rebuilds
      it (ensurePollerPlane runs before CMD_OPEN), so load first. */
   module=NativeScriptManager.loadFromDeclaration("test:probe",new JsonPrimitive("test:"+prefix+"api_probe"+ext));
   check(ShmPoller.active(),"poller attached to the shared region");
   check(NativeChannel.describe().contains("no JNI"),"census advertises the JNI-free poller plane: "+NativeChannel.describe());
   check(module!=null&&module.getAbiVersion()==11,"module loads over the poller CMD_OPEN");
   check("test:api_probe".equals(module.getScriptId()),"module identity over the poller commands");
   NativeScriptManager.configure("test:probe",JsonParser.parseString("{\"message\":\"原生 API\"}"),"test:natives/api_probe");
   Fixture poller=new Fixture();
   var pollerFrames=NativeScriptManager.render("poller","test:probe",snapshot,0,m->poller,poller,null);
   check(pollerFrames.size()==1,"poller renders a frame from the shared pages");
   check(poller.marks.equals(List.of("create","render")),"poller lifecycle create/render (api via SVC block)");
   check(poller.latch.await(5,TimeUnit.SECONDS),"poller async API callback (BackgroundWorker -> CBQ ring)");
   NativeScriptManager.render("poller","test:probe",snapshot,0,m->poller,poller,null);
   check(poller.marks.equals(List.of("create","render","render")),"poller persistent state");
   NativeScriptManager.disposeInstance("poller","test:probe");
   check(poller.marks.equals(List.of("create","render","render","dispose")),"per-instance dispose over the poller command");
   long pending=ShmPoller.reclaimerQuiesce(ShmPoller.region());
   check(pending==0,"deferred instance memory drained off-thread (poller diagnostics): "+pending);
   NativeScriptManager.reload();
   check(!NativeScriptManager.isLoaded("test:probe"),"reload unloads the module over the poller CMD_CLOSE");
   /* The reload above tore the region down again; a fresh module load
      re-creates it (and re-attaches the poller), then the deferred
      module memory check can run against the live region. */
   module=NativeScriptManager.loadFromDeclaration("test:probe",new JsonPrimitive("test:"+prefix+"api_probe"+ext));
   check(module!=null&&ShmPoller.active(),"poller re-attached after the reload");
   pending=ShmPoller.reclaimerQuiesce(ShmPoller.region());
   check(pending==0,"deferred module memory drained off-thread (poller diagnostics): "+pending);
   System.out.println("Shared-memory poller plane exercised (JNI-free session: filesystem handshake + shared-page commands + SVC host services + CBQ api re-entry)");
  } else {
   /* ---- Panama FFM plane ---- */
   check(!ShmPoller.active(),"FFM session never attaches the poller plane");
   NativeChannel.configure(NativeChannel.Transport.PANAMA_FFM);
   check(NativeChannel.activeTransport()==NativeChannel.Transport.PANAMA_FFM,"PANAMA_FFM selected");
   check(NativeChannel.describe().contains("no JNI"),"census advertises the JNI-free FFM plane");
   module=NativeScriptManager.loadFromDeclaration("test:probe",new JsonPrimitive("test:"+prefix+"api_probe"+ext));
   check(module!=null&&module.getAbiVersion()==11,"module loads over jcm_module_open");
   check("test:api_probe".equals(module.getScriptId()),"module identity via jcm_module_script_id");
   NativeScriptManager.configure("test:probe",JsonParser.parseString("{\"message\":\"原生 API\"}"),"test:natives/api_probe");
   Fixture ffm=new Fixture();
   var ffmFrames=NativeScriptManager.render("ffm","test:probe",snapshot,0,m->ffm,ffm,null);
   check(ffmFrames.size()==1,"FFM kick renders a frame");
   check(ffm.marks.equals(List.of("create","render")),"FFM lifecycle create/render (api via upcall vtable)");
   check(ffm.latch.await(5,TimeUnit.SECONDS),"FFM async API callback (BackgroundWorker thread downcall->upcall)");
   NativeScriptManager.render("ffm","test:probe",snapshot,0,m->ffm,ffm,null);
   check(ffm.marks.equals(List.of("create","render","render")),"FFM persistent state");
   /* Direct-render downcall (the "JNI" transport on an FFM session):
     same instance, same state, arena descriptors into the reused buffer. */
   NativeChannel.configure(NativeChannel.Transport.JNI);
   var direct2=NativeScriptManager.render("ffm","test:probe",snapshot,0,m->ffm,ffm,null);
   check(direct2.size()==1,"jcm_module_render renders the same live instance");
   check(ffm.marks.equals(List.of("create","render","render","render")),"direct render reuses instance state");
   check(direct2.get(0).recordCount==ffmFrames.get(0).recordCount,"direct render reports the same records");
   NativeChannel.configure(NativeChannel.Transport.PANAMA_FFM);
   NativeScriptManager.disposeInstance("ffm","test:probe");
   check(ffm.marks.equals(List.of("create","render","render","render","dispose")),"per-instance dispose under FFM session");
   PanamaFfmChannel.reclaimerQuiesce();
   check(PanamaFfmChannel.reclaimerPending()==0,"deferred instance memory drained off-thread");
   NativeScriptManager.reload();
   PanamaFfmChannel.reclaimerQuiesce();
   check(PanamaFfmChannel.reclaimerPending()==0,"deferred module memory drained off-thread");
   check(!NativeScriptManager.isLoaded("test:probe"),"reload unloads the module over jcm_module_close");
   NativeChannel.configure(NativeChannel.Transport.SHARED_MEMORY);
   System.out.println("Panama FFM transport exercised (JNI-free session: libraryLookup + downcalls + upcall vtable)");
  }
  NativeScriptManager.reload();
  System.out.println("Native API real integration passed: "+checks+" checks");
 }
}
