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
        /* Channel-provided: a shm window when the shared-memory transport is
           active (C++ then reads the snapshot straight out of the shared
           mapping), a plain direct buffer otherwise. */
        ByteBuffer snapshot=NativeChannel.newSnapshotBuffer(16*1024);
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
                    /* Channel growth frees the old shm window (for plain
                       direct buffers the old one simply becomes garbage). */
                    state.snapshot=NativeChannel.growSnapshotBuffer(state.snapshot);
                }
            }
            NativeScriptManager.render(key,script,state.snapshot,kind,
                module->state.contexts.computeIfAbsent(module.getNativeHandle(),h->context.get()),wrapper,
                (module,ctx)->{}).forEach(frame->{
                    AbstractScriptContext ctx=state.contexts.get(parseTrailingLong(frame.instanceKey));
                    apply(frame,ctx);
                });
        } catch(Throwable ex) {
            /* Throwable, not Exception: a StackOverflowError from a runaway
               script must hit the same 4s backoff instead of killing the
               render thread. */
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
            /* No transform.copy(): invoke replays every draw call against
               its own pooled snapshot seeded from this transform — one
               shared (read-only) transform serves every context. */
            ctx.getRenderManager().invoke(world,transform,facing,light);
            ctx.getSoundManager().invoke(world,wrapper.blockPos());
        }
    }

    /** Shadow-pass variant of the eyecandy pipeline (shader shadow phase):
     *  the script must NOT execute again — no snapshot, no context reset, no
     *  native render, no event consumption, no sound replay and no texture
     *  upload. Only the draw calls the last main pass captured are replayed,
     *  so models that cast shadows keep their silhouette; everything with a
     *  frame side effect (sounds, uploads, onBlockUse handling) waits for
     *  the next main pass. Mirrors the JS branch's shadow guard in
     *  RenderEyeCandyMixin, which skips invokeRenderFunctions but keeps the
     *  render-manager replay. */
    public static void eyecandy(String script,BlockEyeCandy.BlockEntity block,World world,StoredMatrixTransformations transform,Direction facing,int light,boolean shadowPass) {
        final String key=key("eyecandy",script,block.getPos2());
        if(shadowPass) {
            State state=STATES.get(key);
            if(state==null) return;   /* nothing captured yet: nothing to replay */
            /* Still a live sighting: keep the idle valve from disposing the
               instance while only the shadow camera sees the block. */
            state.touched=System.nanoTime();
            for(AbstractScriptContext context:state.contexts.values()) {
                ((EyeCandyScriptContext)context).getRenderManager().invoke(world,transform,facing,light);
            }
            return;
        }
        EyecandyBlockEntityWrapper wrapper=new EyecandyBlockEntityWrapper(block);
        State previous=STATES.get(key);
        boolean used=false;
        if(previous!=null) for(AbstractScriptContext c:previous.contexts.values()) {
            if(((EyeCandyScriptContext)c).events().onBlockUse.occurred()) { used=true; break; }
        }
        /* Lambda capture requires an effectively final variable — `used` is
           reassigned by the loop above, so snapshot it once. */
        final boolean blockUsed=used;
        State state=run(key,script,NativeScriptManager.MTR_RESOURCE_EYECANDY,wrapper,
                b->NativeContentSnapshot.eyecandy(b,wrapper,blockUsed),()->new EyeCandyScriptContext(block));
        for(AbstractScriptContext context:state.contexts.values()) {
            EyeCandyScriptContext ctx=(EyeCandyScriptContext)context;
            /* No transform.copy(): see the PIDS path above. */
            ctx.getRenderManager().invoke(world,transform,facing,wrapper.getFullBrightness()?GraphicsHolderDefaultLight.VALUE:light);
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
            /* No per-context transform.copy(): the render manager's invoke
               isolates per-draw-call additions with mark/truncate, so ONE
               caller-provided transform serves every context. */
            ctx.getRenderManager().invoke(world,transform,Direction.NORTH,light);
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

    /** Decimal number at the end of a ".../module/<handle>" key, without the
     *  per-frame substring+parseLong allocation. */
    private static long parseTrailingLong(String s) {
        final int end=s.length(); int start=end;
        while(start>0) { final char c=s.charAt(start-1); if(c>='0'&&c<='9') start--; else break; }
        if(start==end) return -1;
        try { return Long.parseLong(s,start,end,10); } catch(NumberFormatException ignored) { return -1; }
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
        /* Interned: C++ scripts emit the same strings every frame — the
           arena bytes are hashed directly and the decoded String (and any
           Identifier derived from it) is reused, so a steady frame decodes
           nothing. */
        return NativeStringCache.string(arena,offset,length);
    }
    private static org.mtr.mapping.holder.Identifier identifier(ByteBuffer arena,int offset,int length) {
        return NativeStringCache.identifier(arena,offset,length);
    }

    /** MtrRenderLayer (the C++ enum in mtr_native.h) → QueuedRenderLayer
     * name for the wrappers' renderType(String) setters. The light
     * variants condense to LIGHT_2 — the TextureWrapper/RectangleWrapper
     * default — and TEXT/EXTERIOR/INTERIOR map one to one. */
    private static String renderLayerName(int layer) {
        switch(layer) {
            case 0: return "TEXT";
            case 2: return "EXTERIOR";
            case 3: return "INTERIOR";
            default: return "LIGHT_2";
        }
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
                org.mtr.mapping.holder.Identifier id=identifier(frame.stringArena,records.getInt(pos+32),records.getInt(pos+36));
                if(kind==5) soundManager(context).playLocalSound(id,records.getFloat(pos+24),records.getFloat(pos+28));
                else soundManager(context).playSound(id,new ScriptVector3f(records.getFloat(pos+12),records.getFloat(pos+16),records.getFloat(pos+20)),records.getFloat(pos+24),records.getFloat(pos+28));
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
                /* Pooled wrapper + interned strings: a native PIDS with a
                   steady screen used to allocate a full TextWrapper, an
                   Identifier per font/texture and two Strings per text
                   record per frame. */
                TextWrapper call=TextWrapper.createPooled().text(string(frame.stringArena,records.getInt(pos+96),records.getInt(pos+100)))
                    .color(records.getInt(pos+16)).pos(records.getDouble(pos+32),records.getDouble(pos+40))
                    .size(records.getDouble(pos+48),records.getDouble(pos+56)).scale(records.getDouble(pos+64));
                if(records.get(pos+8)!=0) call.shadowed(); if(records.get(pos+9)!=0) call.bold(); if(records.get(pos+10)!=0) call.italic();
                int align=records.getInt(pos+20); if(align==0) call.centerAlign(); else if(align>0) call.rightAlign(); else call.leftAlign();
                switch(records.getInt(pos+24)) { case 1:call.stretchXY();break; case 2:call.scaleXY();break; case 3:call.wrapText();break; case 4:call.marquee(records.getDouble(pos+72)).withMarqueeProgress(records.getDouble(pos+80));break; }
                int fontLength=records.getInt(pos+92); if(fontLength>0) call.font(string(frame.stringArena,records.getInt(pos+88),fontLength));
                int z=records.getInt(pos+28); if(z>=0) call.zOrder(z);
                /* Header flags byte (natural light) + the render layer —
                   both ignored by this replay before the flags byte existed;
                   records from older SDKs carry zeros and map onto the
                   wrapper defaults, so behavior is unchanged for them. */
                if((records.get(pos+1)&1)!=0) call.naturalLight();
                call.renderType(renderLayerName(records.getInt(pos+12)));
                ((PIDSScriptContext)context).draw(call);
            } else if(kind==2&&context instanceof PIDSScriptContext) {
                TextureWrapper call=TextureWrapper.createPooled().texture(identifier(frame.stringArena,records.getInt(pos+68),records.getInt(pos+72)))
                    .color(records.getInt(pos+12)).pos(records.getDouble(pos+16),records.getDouble(pos+24))
                    .size(records.getDouble(pos+32),records.getDouble(pos+40)).uv(records.getFloat(pos+48),records.getFloat(pos+52),records.getFloat(pos+56),records.getFloat(pos+60));
                int z=records.getInt(pos+64); if(z>=0) call.zOrder(z);
                if((records.get(pos+1)&1)!=0) call.naturalLight();
                call.renderType(renderLayerName(records.getInt(pos+8)));
                ((PIDSScriptContext)context).draw(call);
            }
            pos+=size;
        }
    }

    /* Same budget shape as the vehicle sweep (yanyang.7): one teardown is
       a dozen GL textures + uploaded models + a full canvas repaint, so
       mass idle-expiry in a single tick was a multi-millisecond hitch. A
       burst beyond MASS_EXPIRY_FLUSH is a teardown (world unload /
       dimension switch / disconnect) where nothing renders anyway — pay
       it all at once instead of dripping GL deletions across a loading
       screen. */
    private static final int MAX_DISPOSES_PER_TICK = 2;
    private static final int MASS_EXPIRY_FLUSH = 8;
    private static final java.util.Comparator<State> TOUCHED_ORDER =
            java.util.Comparator.comparingLong((State s) -> s.touched);

    public static void tick() {
        long now=System.nanoTime();
        /* Candidates first, then a bounded dispose pass — see the budget
           constants above. Explicit removal flags (eyecandy/lift wrappers)
           are a real despawn signal and share the budget: the sweep
           re-derives candidates every tick, so a deferred dispose just
           lands on the next tick. */
        java.util.ArrayList<State> removedStates=null;
        for(State state:STATES.values()) {
            /* Explicit removal flags (eyecandy/lift wrappers) dispose at
               once — a real despawn signal. Time-based expiry is now the
               shared IDLE VALVE, not the old 5 s rule: block-entity
               renderers only run for VISIBLE blocks, so "not rendered" used
               to mean "behind the camera" at least as often as "gone" — a
               PIDS that left the frustum for 5 s was disposed and rebuilt
               (mtrCreate + texture/model churn + full repaint) in the first
               frame after re-entering view, the same silent stutter the
               vehicle sweep had. Living blocks keep their instance until
               the valve horizon (jcm.nativeIdleMillis, default 90 s). */
            boolean removed=false;
            if(state.wrapper instanceof EyecandyBlockEntityWrapper) removed=((EyecandyBlockEntityWrapper)state.wrapper).removed();
            else if(state.wrapper instanceof LiftWrapper) removed=((LiftWrapper)state.wrapper).removed();
            if(!removed&&now-state.touched>NativeScriptManager.IDLE_DISPOSE_NANOS) removed=true;
            if(removed) {
                if(removedStates==null) removedStates=new java.util.ArrayList<>();
                removedStates.add(state);
            }
        }
        if(removedStates==null) return;
        removedStates.sort(TOUCHED_ORDER);   /* oldest first */
        int budget=removedStates.size()>MASS_EXPIRY_FLUSH?Integer.MAX_VALUE:MAX_DISPOSES_PER_TICK;
        for(State state:removedStates) {
            if(budget<=0) break;
            if(!STATES.remove(state.key,state)) continue;
            budget--;
            NativeScriptManager.disposeInstance(state.key,state.script,state.snapshot);
            /* dispose hands the LIVE buffer to mtrDispose one last time;
               release the shm window only after it has come back. */
            NativeChannel.releaseSnapshotBuffer(state.snapshot);
        }
    }
    public static void reset() {
        for(State state:STATES.values()) NativeChannel.releaseSnapshotBuffer(state.snapshot);
        STATES.clear(); NativeStringCache.clear(); com.lx862.jcm.mod.util.CachedIdentifiers.clear();
    }
}
