package com.lx862.jcm.nativeapi;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lx862.mtrscripting.core.util.model.ModelJS;
import com.lx862.mtrscripting.core.util.render.ScriptRenderManager;
import com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleScriptContext;
import com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleWrapper;
import org.mtr.core.data.SimplifiedRoute;
import org.mtr.core.data.Station;
import org.mtr.mapping.mapper.ResourceManagerHelper;
import org.mtr.mod.data.VehicleExtension;
import org.mtr.mod.render.StoredMatrixTransformations;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class NativeIntegrationTest {
    private static int assertions;
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        assertions++;
    }
    private static String owner(NativeScriptManager.NativeFrame frame) throws Exception {
        return (String) frame.getClass().getField("instanceKey").get(frame);
    }
    private static String string(ByteBuffer b, int offset, int len) {
        byte[] data=new byte[len]; b.duplicate().position(offset).get(data);
        return new String(data, StandardCharsets.UTF_8);
    }
    private static void load() {
        String extension=NativeScriptManager.getPlatformKey().equals("windows") ? ".dll" :
                NativeScriptManager.getPlatformKey().equals("macos") ? ".dylib" : ".so";
        String prefix=extension.equals(".dll") ? "" : "lib";
        JsonArray libraries=new JsonArray();
        libraries.add("test:"+prefix+"wr2a03_lcd"+extension);
        libraries.add("test:"+prefix+"wr2a03_train_num"+extension);
        NativeScriptManager.loadManyFromDeclaration("wr2a03",libraries);
        NativeScriptManager.loadManyFromDeclaration("wr2a03",libraries);
        check(NativeScriptManager.getModules("wr2a03").size()==2,"repeated discovery must keep exactly two modules");
    }
    private static Map<Integer, ScriptRenderManager> apply(List<NativeScriptManager.NativeFrame> frames,int cars) throws Exception {
        var managers=NativeDrawRegistry.begin("vehicle","wr2a03",cars);
        Method apply=NativeVehicleDriver.class.getDeclaredMethod("applyFrame", NativeScriptManager.NativeFrame.class,Map.class);
        apply.setAccessible(true);
        for(var frame:frames) check((boolean)apply.invoke(null,frame,managers),"frame must reach production replay driver");
        return managers;
    }
    private static List<ModelJS> models(NativeScriptManager.NativeFrame frame, int expected) {
        List<ModelJS> models=new ArrayList<>();
        var records=frame.records.duplicate().order(ByteOrder.nativeOrder());
        int pos=0;
        for(int i=0;i<frame.recordCount;i++) {
            int size=records.getInt(pos+4);
            check(size>=8 && pos+size<=records.limit(),"valid JNI record stream");
            if(records.get(pos)==3) {
                int handle=records.getInt(pos+12);
                ModelJS model=NativeHost.get().model(handle);
                check(model!=null && !model.closed,"every returned model handle must remain live (handle="+handle+", record="+i+", model="+model+")");
                check(model.geometry.vertices.size()==12 && model.geometry.uv.size()==8,"each quad retains its own geometry");
                check(model.geometry.uv.equals(List.of(0f,0f,0f,1f,1f,1f,1f,0f)),"DisplayHelper UV orientation");
                models.add(model);
            }
            pos+=size;
        }
        check(models.size()==expected,"expected native quad count "+expected+", got "+models.size());
        return models;
    }
    public static void main(String[] args) throws Exception {
        ResourceManagerHelper.root=Path.of(args[0]);
        var glyph=ByteBuffer.allocateDirect(64*64*4).order(ByteOrder.nativeOrder());
        check(NativeHost.get().rasterizeText("站".getBytes(StandardCharsets.UTF_8),0,0,32,17,34,51,glyph,64,64)>0,"host font callback rasterises Chinese with the bundled Noto CJK font");
        boolean hasInk=false;
        for(int i=0;i<64*64;i++) if(glyph.getInt(i*4)!=0) { hasInk=true; break; }
        check(hasInk,"font callback writes visible glyph pixels");
        for(String row:Files.readAllLines(Path.of(args[1]))) {
            var parts=row.split("="); var field=NativeSnapshot.class.getDeclaredField(parts[0]); field.setAccessible(true);
            check(field.getInt(null)==Integer.parseInt(parts[1]),"Java/C++ ABI mismatch at "+row);
        }
        var vehicle=new VehicleExtension();
        var wrapper=new VehicleWrapper(VehicleScriptContext.DataFetchMode.ALL,vehicle);
        var first=new VehicleWrapper.Stop(); first.name="Previous"; first.route=new SimplifiedRoute(1,"1号线|Line 1"); first.station=new Station();
        var current=new VehicleWrapper.Stop(); current.name="Current"; current.route=new SimplifiedRoute(2,"2号线|Line 2"); current.station=new Station();
        current.routeInterchanges.add(new VehicleWrapper.Stop.RouteInterchange());
        var next=new VehicleWrapper.Stop(); next.name="Next"; next.route=new SimplifiedRoute(3,"3号线|Line 3"); next.station=new Station();
        wrapper.all.addAll(List.of(first,current,next)); wrapper.current.add(current); wrapper.next.add(next);
        ByteBuffer snapshot=NativeSnapshot.build(ByteBuffer.allocateDirect(NativeSnapshot.CAPACITY),wrapper);
        int allOffset=snapshot.getInt(128), currentOffset=snapshot.getInt(136), nextOffset=snapshot.getInt(144);
        check(allOffset%8==0,"stop array must be aligned even for an odd car count");
        check(snapshot.getLong(allOffset)==1 && snapshot.getLong(currentOffset)==2 && snapshot.getLong(nextOffset)==3,"independent current/next route ranges");
        check(snapshot.getDouble(88)==15000 && snapshot.getLong(104)>0,"dwell/time fields must not overlap visibility flags");
        int exit=snapshot.getInt(currentOffset+76), refs=snapshot.getInt(exit+12);
        check(string(snapshot,snapshot.getInt(refs),snapshot.getInt(refs+4)).equals("Museum"),"first exit destination reference");
        check(string(snapshot,snapshot.getInt(refs+8),snapshot.getInt(refs+12)).equals("Park"),"second exit destination reference");
        load();
        var frames=NativeScriptManager.renderVehicle("vehicle/wr2a03","wr2a03",snapshot);
        check(frames.size()==2,"both native modules must return a frame");
        var lcd=models(frames.get(0),30); var numbers=models(frames.get(1),14);
        int retiredHandle=0;
        for(int i=1;i<10000;i++) if(NativeHost.get().model(i)==lcd.get(0)) { retiredHandle=i; break; }
        check(!owner(frames.get(0)).equals(owner(frames.get(1))),"LCD and numbers must have independent resource owners");
        check(lcd.stream().map(m->m.geometry.texture).distinct().count()==6,"three cars have six independent LCD textures");
        check(numbers.stream().map(m->m.geometry.texture).distinct().count()==8,"per-car number plates and two end plates use independent textures");
        Set<Float> leftPositions=new HashSet<>();
        for(ModelJS model:lcd) if(model.geometry.vertices.get(0)>0) leftPositions.add(model.geometry.vertices.get(2));
        check(leftPositions.size()==5,"five LCD installation positions per car side");
        check(lcd.stream().anyMatch(m->m.geometry.vertices.get(0)<0) && lcd.stream().anyMatch(m->m.geometry.vertices.get(0)>0),"left/right geometry must both survive JNI capture");
        var managers=apply(frames,3);
        check(managers.get(0).calls.size()==15 && managers.get(1).calls.size()==14 && managers.get(2).calls.size()==15,"all LCD and number calls are captured per car");
        NativeVehicleDriver.invokeCar(vehicle,"wr2a03",0,new StoredMatrixTransformations(),0);
        check(lcd.get(0).draws==1 && lcd.get(0).lastY==-1,"native replay must apply the JS car-space height offset");
        var headTexture=NativeHost.get().texture(6,owner(frames.get(1)));
        int headHash=Arrays.hashCode(((java.awt.image.DataBufferInt)headTexture.bufferedImage.getRaster().getDataBuffer()).getData());
        check(headHash!=0,"head plate receives uploaded pixels");
        var side0=NativeHost.get().texture(0,owner(frames.get(1)));
        var side1=NativeHost.get().texture(2,owner(frames.get(1)));
        check(!Arrays.equals(((java.awt.image.DataBufferInt)side0.bufferedImage.getRaster().getDataBuffer()).getData(),
                            ((java.awt.image.DataBufferInt)side1.bufferedImage.getRaster().getDataBuffer()).getData()),"different cars retain different car numbers");
        // BGRA conversion and clipping through the real GraphicsTexture upload.
        NativeHost.get().setPendingInstance("pixel-test");
        NativeHost.get().createResources(new int[]{2},new int[]{2},new float[0],new float[0],new int[0],new int[0]);
        NativeHost.get().setActiveInstance("pixel-test");
        byte[] pixels={(byte)0x33,(byte)0x22,(byte)0x11,(byte)0xff, 6,5,4,(byte)0xff, 9,8,7,(byte)0xff, 12,11,10,(byte)0xff};
        NativeHost.get().uploadPixels(0,-1,0,2,2,pixels);
        var pixelTexture=NativeHost.get().texture(0,"pixel-test");
        check(pixelTexture.bufferedImage.getRGB(0,0)==0xff040506 && pixelTexture.bufferedImage.getRGB(0,1)==0xff0a0b0c,"clipped BGRA upload preserves source rows and red/blue channels");
        // A pixel upload alone is not a renderable model frame. This used to
        // report withDrawCalls=1 even when every GPU model upload had failed.
        var uploadRecord=ByteBuffer.allocateDirect(56).order(ByteOrder.nativeOrder());
        uploadRecord.put(0,(byte)6).putInt(4,56).putInt(8,0).putInt(28,1).putInt(32,1).putLong(48,4);
        var pixelArena=ByteBuffer.allocateDirect(4).put(new byte[]{3,2,1,(byte)255}); pixelArena.flip();
        var uploadOnly=new NativeScriptManager.NativeFrame(uploadRecord,1,null,pixelArena,null);
        uploadOnly.instanceKey="pixel-test";
        var applyMethod=NativeVehicleDriver.class.getDeclaredMethod("applyFrame",NativeScriptManager.NativeFrame.class,Map.class);
        applyMethod.setAccessible(true);
        check(!(boolean)applyMethod.invoke(null,uploadOnly,managers),"texture-only frames cannot be counted as model draw calls");
        // A new vehicle uses the same slots without replacing the first vehicle.
        var other=NativeScriptManager.renderVehicle("other/wr2a03","wr2a03",snapshot);
        models(other.get(0),30); models(other.get(1),14); apply(other,3);
        check(!lcd.get(0).closed,"another vehicle cannot close the first vehicle's resources");
        wrapper.count=2; wrapper.siding="10010/01-02";
        snapshot=NativeSnapshot.build(snapshot,wrapper);
        var resized=NativeScriptManager.renderVehicle("vehicle/wr2a03","wr2a03",snapshot);
        models(resized.get(0),20); models(resized.get(1),10); apply(resized,2);
        check(lcd.stream().allMatch(m->m.closed),"resizing retires old per-car LCD models");
        check(NativeHost.get().model(retiredHandle)==null,"retired models are removed from the global handle table");
        check(NativeHost.get().texture(6,owner(resized.get(1)))==headTexture,"resizing preserves unchanged head texture");
        check(Arrays.hashCode(((java.awt.image.DataBufferInt)headTexture.bufferedImage.getRaster().getDataBuffer()).getData())==headHash,"preserved head plate contents survive resource synchronisation");
        NativeScriptManager.reload();
        check(ModelJS.all.stream().allMatch(m->m.closed),"reload closes all models across modules and vehicles");
        load();
        var reloaded=NativeScriptManager.renderVehicle("vehicle/wr2a03","wr2a03",snapshot);
        models(reloaded.get(0),20); models(reloaded.get(1),10); apply(reloaded,2);
        NativeScriptManager.reload();
        System.out.println("JVM JNI INTEGRATION OK ("+assertions+" assertions)");
    }
}
