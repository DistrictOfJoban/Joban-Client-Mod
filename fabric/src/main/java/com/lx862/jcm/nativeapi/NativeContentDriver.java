package com.lx862.jcm.nativeapi;

import com.lx862.jcm.mod.scripting.pids.*;
import com.lx862.jcm.mod.util.JCMLogger;
import com.lx862.mtrscripting.core.api.AbstractScriptContext;
import com.lx862.mtrscripting.core.util.*;
import com.lx862.mtrscripting.core.util.render.ScriptRenderManager;
import com.lx862.mtrscripting.core.util.sound.ScriptSoundManager;
import com.lx862.mtrscripting.core.integration.VoxelShapeWrapper;
import com.lx862.mtrscripting.mod.impl.mtr.eyecandy.*;
import com.lx862.mtrscripting.mod.impl.mtr.eyecandy.event.BlockUseEvent;
import com.lx862.mtrscripting.mod.impl.mtr.lift.*;
import org.mtr.core.data.Lift;
import org.mtr.mapping.holder.*;
import org.mtr.mod.block.BlockEyeCandy;
import org.mtr.mod.render.StoredMatrixTransformations;

import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;

/** Shared native create/render/dispose driver for PIDS, lifts and eyecandies. */
public final class NativeContentDriver {
    private static final class State {
        final String key, script;
        final Map<Long, AbstractScriptContext> contexts = new LinkedHashMap<>();
        ByteBuffer snapshot=ByteBuffer.allocateDirect(16*1024).order(ByteOrder.nativeOrder());
        long touched=System.nanoTime();
        Object wrapper;
        long errorUntil;
        State(String key,String script) { this.key=key; this.script=script; }
    }
    private static final Map<String,State> STATES=new ConcurrentHashMap<>();
    private static String key(String type, String script, BlockPos pos) { return type+"/"+script+"/"+pos.getX()+"/"+pos.getY()+"/"+pos.getZ(); }

    private static State run(String key,String script,int kind,Object wrapper,
                             Function<ByteBuffer,ByteBuffer> snapshot, Supplier<AbstractScriptContext> context) {
        State state=STATES.computeIfAbsent(key,k->new State(k,script));
        state.touched=System.nanoTime(); state.wrapper=wrapper;
        if(System.currentTimeMillis()<state.errorUntil) return state;
        try {
            state.contexts.values().forEach(AbstractScriptContext::resetForNextRun);
            while (true) {
                try { snapshot.apply(state.snapshot); break; }
                catch(BufferOverflowException|IndexOutOfBoundsException ex) {
                    if(state.snapshot.capacity()>=4*1024*1024) throw ex;
                    state.snapshot=ByteBuffer.allocateDirect(state.snapshot.capacity()*2).order(ByteOrder.nativeOrder());
                }
            }
            NativeScriptManager.render(key,script,state.snapshot,kind,
                module->state.contexts.computeIfAbsent(module.getNativeHandle(),h->context.get()),wrapper,
                (module,ctx)->{}).forEach(frame->{
                    AbstractScriptContext ctx=state.contexts.get(Long.parseLong(frame.instanceKey.substring(frame.instanceKey.lastIndexOf('/')+1)));
                    apply(frame,ctx);
                });
        } catch(Exception ex) {
            state.errorUntil=System.currentTimeMillis()+4000;
            JCMLogger.error("Native {} script {} failed",kind,script,ex);
        }
        return state;
    }

    public static void pids(String script,PIDSWrapper wrapper, World world,StoredMatrixTransformations transform,Direction facing,int light) {
        State state=run(key("pids",script,wrapper.blockPos().rawBlockPos()),script,NativeScriptManager.MTR_RESOURCE_PIDS,wrapper,
                b->NativeContentSnapshot.pids(b,wrapper),()->new PIDSScriptContext(script));
        for(AbstractScriptContext context:state.contexts.values()) {
            PIDSScriptContext ctx=(PIDSScriptContext)context;
            ctx.getRenderManager().invoke(world,transform.copy(),facing,light);
            ctx.getSoundManager().invoke(world,wrapper.blockPos());
        }
    }

    public static void eyecandy(String script,BlockEyeCandy.BlockEntity block,World world,StoredMatrixTransformations transform,Direction facing,int light) {
        EyecandyBlockEntityWrapper wrapper=new EyecandyBlockEntityWrapper(block);
        String key=key("eyecandy",script,block.getPos2());
        State previous=STATES.get(key);
        boolean used=previous!=null&&previous.contexts.values().stream().anyMatch(c->((EyeCandyScriptContext)c).events().onBlockUse.occurred());
        State state=run(key,script,NativeScriptManager.MTR_RESOURCE_EYECANDY,wrapper,
                b->NativeContentSnapshot.eyecandy(b,wrapper,used),()->new EyeCandyScriptContext(wrapper));
        for(AbstractScriptContext context:state.contexts.values()) {
            EyeCandyScriptContext ctx=(EyeCandyScriptContext)context;
            ctx.getRenderManager().invoke(world,transform.copy(),facing,wrapper.getFullBrightness()?GraphicsHolderDefaultLight.VALUE:light);
            ctx.getSoundManager().invoke(world,wrapper.pos());
            ctx.events().handled();
        }
    }

    private static final class GraphicsHolderDefaultLight {
        static final int VALUE=org.mtr.mapping.mapper.GraphicsHolder.getDefaultLight();
    }

    public static void lift(String script,LiftWrapper wrapper,Lift lift) {
        run("lift/"+script+"/"+wrapper.getId(),script,NativeScriptManager.MTR_RESOURCE_LIFT,wrapper,
                b->NativeContentSnapshot.lift(b,wrapper),()->new LiftScriptContext(lift));
    }
    public static void invokeLift(String script,long id,World world,StoredMatrixTransformations transform,int light,ScriptVector3f pos) {
        State state=STATES.get("lift/"+script+"/"+id);
        if(state==null) return;
        for(AbstractScriptContext context:state.contexts.values()) {
            LiftScriptContext ctx=(LiftScriptContext)context;
            ctx.getRenderManager().invoke(world,transform.copy(),Direction.NORTH,light);
            ctx.getSoundManager().invoke(world,pos);
        }
    }

    public static VoxelShape shape(BlockEyeCandy.BlockEntity block,boolean collision) {
        String script=com.lx862.mtrscripting.mod.resource.MtrScriptingResourceManager.eyecandy.getScriptEntryId(block.getModelId());
        State state=STATES.get(key("eyecandy",script,block.getPos2()));
        if(state==null) return null;
        VoxelShape shape=null;
        for(AbstractScriptContext context:state.contexts.values()) {
            EyeCandyScriptContext ctx=(EyeCandyScriptContext)context;
            VoxelShape candidate=collision?ctx.getCollisionShape():ctx.getOutlineShape();
            if(candidate!=null) shape=candidate;
        }
        return shape;
    }
    public static boolean blockUse(BlockEyeCandy.BlockEntity block,BlockUseEvent event) {
        String script=com.lx862.mtrscripting.mod.resource.MtrScriptingResourceManager.eyecandy.getScriptEntryId(block.getModelId());
        if(!NativeScriptManager.isLoaded(script)) return false;
        State state=STATES.get(key("eyecandy",script,block.getPos2()));
        if(state!=null) for(AbstractScriptContext context:state.contexts.values()) ((EyeCandyScriptContext)context).events().onBlockUse.trigger(event);
        return true;
    }

    private static ScriptRenderManager renderManager(AbstractScriptContext context) {
        if(context instanceof PIDSScriptContext) return ((PIDSScriptContext)context).getRenderManager();
        if(context instanceof EyeCandyScriptContext) return ((EyeCandyScriptContext)context).getRenderManager();
        return ((LiftScriptContext)context).getRenderManager();
    }
    private static ScriptSoundManager soundManager(AbstractScriptContext context) {
        if(context instanceof PIDSScriptContext) return ((PIDSScriptContext)context).getSoundManager();
        if(context instanceof EyeCandyScriptContext) return ((EyeCandyScriptContext)context).getSoundManager();
        return ((LiftScriptContext)context).getSoundManager();
    }
    private static String string(ByteBuffer arena,int offset,int length) {
        if(arena==null||offset<0||length<0||(long)offset+length>arena.limit()) throw new IllegalArgumentException("Invalid native string reference");
        byte[] bytes=new byte[length]; ByteBuffer src=arena.duplicate(); src.position(offset); src.get(bytes);
        return new String(bytes,StandardCharsets.UTF_8);
    }
    public static void apply(NativeScriptManager.NativeFrame frame,AbstractScriptContext context) {
        NativeHost.get().setActiveInstance(frame.instanceKey);
        ByteBuffer records=frame.records.order(ByteOrder.nativeOrder()); int pos=0;
        for(int i=0;i<frame.recordCount;i++) {
            if(pos+8>records.limit()) throw new IllegalArgumentException("Truncated native record");
            int kind=records.get(pos)&255, size=records.getInt(pos+4);
            if(size<8||(long)pos+size>records.limit()) throw new IllegalArgumentException("Invalid native record size");
            int minimum = kind==1 ? 104 : kind==2 ? 80 : kind==3 ? 24 : (kind==4||kind==5) ? 40 : kind==6 ? 56 : kind==7 ? 20 : 8;
            if (size < minimum) throw new IllegalArgumentException("Truncated native record payload");
            if(kind==3) {
                NativeModelDrawCall call=NativeModelDrawCall.create(records.getInt(pos+12));
                call.pose(frame.matrixArena,records.getInt(pos+16),records.getInt(pos+20));
                renderManager(context).draw(call);
            } else if(kind==4||kind==5) {
                String id=string(frame.stringArena,records.getInt(pos+32),records.getInt(pos+36));
                if(kind==5) soundManager(context).playLocalSound(new Identifier(id),records.getFloat(pos+24),records.getFloat(pos+28));
                else soundManager(context).playSound(new Identifier(id),new ScriptVector3f(records.getFloat(pos+12),records.getFloat(pos+16),records.getFloat(pos+20)),records.getFloat(pos+24),records.getFloat(pos+28));
            } else if(kind==6) {
                long offset=records.getLong(pos+40),length=records.getLong(pos+48);
                if(frame.pixelArena==null||offset<0||length<0||length>frame.pixelArena.limit()-offset||length>Integer.MAX_VALUE) throw new IllegalArgumentException("Invalid native pixel arena");
                NativeHost.get().uploadPixels(records.getInt(pos+8),records.getInt(pos+20),records.getInt(pos+24),records.getInt(pos+28),records.getInt(pos+32),frame.pixelArena,offset,length);
            } else if(kind==7&&context instanceof EyeCandyScriptContext) {
                int count=records.getInt(pos+12),offset=records.getInt(pos+16);
                if(frame.floatArena==null||count<0||offset<0||(offset&3)!=0||(long)offset+(long)count*24>frame.floatArena.limit()) throw new IllegalArgumentException("Invalid native shape arena");
                VoxelShapeWrapper shape=VoxelShapeWrapper.empty();
                ByteBuffer floats=frame.floatArena.order(ByteOrder.nativeOrder());
                for(int box=0;box<count;box++) {
                    int at=offset+box*24;
                    shape=shape.combine(VoxelShapeWrapper.create(floats.getFloat(at),floats.getFloat(at+4),floats.getFloat(at+8),floats.getFloat(at+12),floats.getFloat(at+16),floats.getFloat(at+20)));
                }
                if(records.get(pos+8)!=0) ((EyeCandyScriptContext)context).setCollisionShape(shape);
                else ((EyeCandyScriptContext)context).setOutlineShape(shape);
            } else if(kind==1&&context instanceof PIDSScriptContext) {
                TextWrapper call=TextWrapper.create().text(string(frame.stringArena,records.getInt(pos+96),records.getInt(pos+100)))
                    .color(records.getInt(pos+16)).pos(records.getDouble(pos+32),records.getDouble(pos+40))
                    .size(records.getDouble(pos+48),records.getDouble(pos+56)).scale(records.getDouble(pos+64));
                if(records.get(pos+8)!=0) call.shadowed(); if(records.get(pos+9)!=0) call.bold(); if(records.get(pos+10)!=0) call.italic();
                int align=records.getInt(pos+20); if(align==0) call.centerAlign(); else if(align>0) call.rightAlign(); else call.leftAlign();
                switch(records.getInt(pos+24)) { case 1:call.stretchXY();break; case 2:call.scaleXY();break; case 3:call.wrapText();break; case 4:call.marquee(records.getDouble(pos+72)).withMarqueeProgress(records.getDouble(pos+80));break; }
                int fontLength=records.getInt(pos+92); if(fontLength>0) call.font(string(frame.stringArena,records.getInt(pos+88),fontLength));
                int z=records.getInt(pos+28); if(z>=0) call.zOrder(z);
                ((PIDSScriptContext)context).draw(call);
            } else if(kind==2&&context instanceof PIDSScriptContext) {
                TextureWrapper call=TextureWrapper.create().texture(string(frame.stringArena,records.getInt(pos+68),records.getInt(pos+72)))
                    .color(records.getInt(pos+12)).pos(records.getDouble(pos+16),records.getDouble(pos+24))
                    .size(records.getDouble(pos+32),records.getDouble(pos+40)).uv(records.getFloat(pos+48),records.getFloat(pos+52),records.getFloat(pos+56),records.getFloat(pos+60));
                int z=records.getInt(pos+64); if(z>=0) call.zOrder(z); ((PIDSScriptContext)context).draw(call);
            }
            pos+=size;
        }
    }

    public static void tick() {
        long now=System.nanoTime();
        for(State state:STATES.values()) {
            boolean removed=now-state.touched>5_000_000_000L;
            if(state.wrapper instanceof EyecandyBlockEntityWrapper) removed|=((EyecandyBlockEntityWrapper)state.wrapper).removed();
            if(state.wrapper instanceof LiftWrapper) removed|=((LiftWrapper)state.wrapper).removed();
            if(removed&&STATES.remove(state.key,state)) NativeScriptManager.disposeInstance(state.key,state.script);
        }
    }
    public static void reset() { STATES.clear(); }
}
