package com.lx862.mtrscripting.mod.network.packet.data;

import com.lx862.mtrscripting.mod.impl.mtr.vehicle.ServerDoorDirections;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;

/**
 * Server → client: the answer to {@link RequestDoorDirectionsC2SPacket} —
 * the stored canonical door-opening sides of every platform the route calls
 * at. Sides are in the platform rail's intrinsic frame; the client converts
 * them to its own train's head-relative frame with the same
 * {@code reversed == reversePositions} composition it already uses for its
 * local observations.
 *
 * <p>{@code complete == false} means "not scanned yet": the server is
 * scheduling a one-route scan and the client should retry shortly, while
 * continuing to fall back to its own in-view world scan.
 */
public class DoorDirectionsS2CPacket extends PacketHandler {
    private final long routeId;
    private final boolean complete;
    private final long[] platformIds;
    private final boolean[] left;
    private final boolean[] right;

    public DoorDirectionsS2CPacket(PacketBufferReceiver packetBufferReceiver) {
        this.routeId = packetBufferReceiver.readLong();
        this.complete = packetBufferReceiver.readBoolean();
        final int size = packetBufferReceiver.readInt();
        this.platformIds = new long[size];
        this.left = new boolean[size];
        this.right = new boolean[size];
        for(int i = 0; i < size; i++) {
            this.platformIds[i] = packetBufferReceiver.readLong();
            this.left[i] = packetBufferReceiver.readBoolean();
            this.right[i] = packetBufferReceiver.readBoolean();
        }
    }

    public DoorDirectionsS2CPacket(long routeId, boolean complete, long[] platformIds, boolean[] left, boolean[] right) {
        if(platformIds.length != left.length || platformIds.length != right.length) {
            throw new IllegalArgumentException("DoorDirectionsS2CPacket arrays must have equal length");
        }
        this.routeId = routeId;
        this.complete = complete;
        this.platformIds = platformIds;
        this.left = left;
        this.right = right;
    }

    public static DoorDirectionsS2CPacket empty(long routeId) {
        return new DoorDirectionsS2CPacket(routeId, false, new long[0], new boolean[0], new boolean[0]);
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeLong(routeId);
        packetBufferSender.writeBoolean(complete);
        packetBufferSender.writeInt(platformIds.length);
        for(int i = 0; i < platformIds.length; i++) {
            packetBufferSender.writeLong(platformIds[i]);
            packetBufferSender.writeBoolean(left[i]);
            packetBufferSender.writeBoolean(right[i]);
        }
    }

    @Override
    public void runClient() {
        ServerDoorDirections.accept(routeId, complete, platformIds, left, right);
    }
}
