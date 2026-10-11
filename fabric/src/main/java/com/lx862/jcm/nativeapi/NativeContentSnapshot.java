package com.lx862.jcm.nativeapi;

import com.lx862.jcm.mod.scripting.pids.PIDSWrapper;
import com.lx862.mtrscripting.mod.impl.mtr.pids.ArrivalWrapper;
import com.lx862.mtrscripting.mod.impl.mtr.eyecandy.EyecandyBlockEntityWrapper;
import com.lx862.mtrscripting.mod.impl.mtr.lift.LiftWrapper;
import com.lx862.mtrscripting.core.util.ScriptVector3f;
import org.mtr.mapping.holder.MinecraftClient;

import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** POD snapshots for all non-vehicle script kinds; offsets are blob-relative. */
public final class NativeContentSnapshot {
    private final ByteBuffer buffer;
    private final Map<String, int[]> strings = new HashMap<>();
    private NativeContentSnapshot(ByteBuffer buffer, int header) {
        this.buffer = buffer;
        buffer.clear().order(ByteOrder.nativeOrder());
        for (int i = 0; i < header; i++) buffer.put((byte)0);
    }
    private int reserve(int size, int alignment) {
        int pos = (buffer.position() + alignment - 1) & -alignment;
        if (size < 0 || (long)pos + size > buffer.capacity()) throw new BufferOverflowException();
        buffer.position(pos + size);
        return pos;
    }
    private void string(int at, String text) {
        if (text == null) { buffer.putInt(at, -1); buffer.putInt(at + 4, 0); return; }
        int[] ref = strings.get(text);
        if (ref == null) {
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            ref = new int[]{buffer.position(), bytes.length};
            buffer.put(bytes); strings.put(text, ref);
        }
        buffer.putInt(at, ref[0]); buffer.putInt(at + 4, ref[1]);
    }
    private static long day() {
        return MinecraftClient.getInstance().getWorldMapped() == null ? 0 : com.lx862.mtrscripting.core.integration.MinecraftClientWrapper.worldDayTime();
    }
    private ByteBuffer finish() { buffer.flip(); return buffer; }

    public static ByteBuffer pids(ByteBuffer buffer, PIDSWrapper pids) {
        NativeContentSnapshot out = new NativeContentSnapshot(buffer,104);
        ScriptVector3f pos = pids.blockPos();
        buffer.putLong(0,(long)pos.x()); buffer.putLong(8,(long)pos.y()); buffer.putLong(16,(long)pos.z());
        buffer.putLong(24,pids.station() == null ? 0 : pids.station().getId());
        buffer.putLong(32,System.currentTimeMillis()); buffer.putLong(40,day());
        buffer.putInt(48,pids.width); buffer.putInt(52,pids.height); buffer.putInt(56,pids.rows);
        buffer.put(61,(byte)(pids.isKeyBlock()?1:0)); buffer.put(62,(byte)(pids.isPlatformNumberHidden()?1:0));
        int rows=0; for (int i=0;i<Math.min(pids.rows,32);i++) if(pids.isRowHidden(i)) rows |= 1 << i;
        buffer.putInt(72,rows);
        List<ArrivalWrapper> arrivals = new ArrayList<>(); pids.arrivals().forEach(arrivals::add);
        int arrivalOffset=out.reserve(arrivals.size()*96,8);
        buffer.putInt(64,arrivals.size()); buffer.putInt(68,arrivalOffset);
        int messages=out.reserve(pids.rows*8,4);
        buffer.putInt(76,pids.rows); buffer.putInt(80,messages);
        int platforms=out.reserve(pids.getTargetPlatformIds().size()*8,8);
        buffer.putInt(84,pids.getTargetPlatformIds().size()); buffer.putInt(88,platforms);
        for(int i=0;i<pids.getTargetPlatformIds().size();i++) buffer.putLong(platforms+i*8,pids.getTargetPlatformIds().getLong(i));
        int pool=buffer.position(); buffer.putInt(92,pool);
        for (int i=0;i<arrivals.size();i++) {
            ArrivalWrapper arrival=arrivals.get(i); int at=arrivalOffset+i*96;
            buffer.putLong(at,arrival.arrivalTime()); buffer.putLong(at+8,arrival.departureTime()); buffer.putLong(at+16,arrival.deviation());
            buffer.putLong(at+24,arrival.departureIndex()); buffer.putLong(at+32,arrival.routeId()); buffer.putLong(at+40,arrival.platformId());
            buffer.putInt(at+48,arrival.routeColor()); buffer.putInt(at+52,arrival.carCount());
            buffer.put(at+56,(byte)(arrival.realtime()?1:0)); buffer.put(at+57,(byte)(arrival.terminating()?1:0));
            buffer.put(at+58,(byte)arrival.circularState().ordinal());
            out.string(at+60,arrival.routeNumber()); out.string(at+68,arrival.routeName());
            out.string(at+76,arrival.destination()); out.string(at+84,arrival.platformName());
        }
        for(int i=0;i<pids.rows;i++) out.string(messages+i*8,pids.getCustomMessage(i));
        buffer.putInt(96,buffer.position()-pool);
        return out.finish();
    }

    public static ByteBuffer eyecandy(ByteBuffer buffer, EyecandyBlockEntityWrapper block, boolean used) {
        NativeContentSnapshot out = new NativeContentSnapshot(buffer,88);
        ScriptVector3f pos=block.blockPos();
        buffer.putLong(0,(long)pos.x()); buffer.putLong(8,(long)pos.y()); buffer.putLong(16,(long)pos.z());
        buffer.putLong(24,System.currentTimeMillis()); buffer.putLong(32,day());
        buffer.putFloat(40,block.getTranslateX()); buffer.putFloat(44,block.getTranslateY()); buffer.putFloat(48,block.getTranslateZ());
        buffer.putFloat(52,block.getRotateX()); buffer.putFloat(56,block.getRotateY()); buffer.putFloat(60,block.getRotateZ());
        buffer.put(64,(byte)(block.getFullBrightness()?1:0)); buffer.put(65,(byte)block.facing().ordinal());
        buffer.put(66,(byte)(block.isCrosshairTarget()?1:0)); buffer.putInt(68,block.redstoneLevel());
        buffer.putInt(80,used?1:0); out.string(72,block.getModelId());
        return out.finish();
    }

    public static ByteBuffer lift(ByteBuffer buffer, LiftWrapper lift) {
        NativeContentSnapshot out = new NativeContentSnapshot(buffer,136);
        buffer.putLong(0,lift.getId()); buffer.putLong(8,System.currentTimeMillis()); buffer.putLong(16,day());
        ScriptVector3f pos=lift.getRawPos(), offset=lift.getOffset();
        buffer.putDouble(24,pos.x()); buffer.putDouble(32,pos.y()); buffer.putDouble(40,pos.z());
        buffer.putDouble(48,offset.x()); buffer.putDouble(56,offset.y()); buffer.putDouble(64,offset.z());
        buffer.putDouble(72,lift.getWidth()); buffer.putDouble(80,lift.getHeight()); buffer.putDouble(88,lift.getDepth());
        buffer.putDouble(96,lift.getAngleDegrees()); buffer.putDouble(104,lift.getAngleRadians()); buffer.putDouble(112,lift.getDoorValue());
        buffer.putInt(120,lift.getDirection()); buffer.put(124,(byte)(lift.shouldRender()?1:0));
        buffer.put(125,(byte)(lift.isDoubleSided()?1:0)); buffer.put(126,(byte)(lift.isClientPlayerRiding()?1:0));
        List<LiftWrapper.Floor> floors=lift.getFloors(); int floorOffset=out.reserve(floors.size()*48,8);
        buffer.putInt(128,floors.size()); buffer.putInt(132,floorOffset);
        for(int i=0;i<floors.size();i++) {
            LiftWrapper.Floor floor=floors.get(i); int at=floorOffset+i*48;
            buffer.putInt(at,floor.getIndex()); buffer.put(at+4,(byte)(floor.isCurrentFloor()?1:0)); buffer.put(at+5,(byte)(floor.isTargetFloor()?1:0));
            ScriptVector3f floorPos=floor.getPos(); buffer.putDouble(at+8,floorPos.x()); buffer.putDouble(at+16,floorPos.y()); buffer.putDouble(at+24,floorPos.z());
            out.string(at+32,floor.getNumber()); out.string(at+40,floor.getDescription());
        }
        return out.finish();
    }
}
