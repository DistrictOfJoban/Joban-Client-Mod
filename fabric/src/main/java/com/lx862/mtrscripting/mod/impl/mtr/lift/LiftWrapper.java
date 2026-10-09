package com.lx862.mtrscripting.mod.impl.mtr.lift;

import com.lx862.jcm.mod.util.MTRUtil;
import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.util.ScriptVector3f;
import org.mtr.core.data.Lift;
import org.mtr.core.data.LiftDirection;
import org.mtr.core.data.LiftFloor;
import org.mtr.core.tool.Angle;
import org.mtr.core.tool.Vector;
import org.mtr.mapping.holder.*;
import org.mtr.mod.block.BlockLiftTrackFloor;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.client.VehicleRidingMovement;
import org.mtr.mod.item.ItemLiftRefresher;
import org.mtr.mod.render.PositionAndRotation;

import java.util.ArrayList;
import java.util.List;

public class LiftWrapper {
    private final Lift liftObject;
    private final List<Floor> floors;
    private final ScriptVector3f pos;
    private final boolean shouldRender;
    private final long id;
    private final int direction;
    private final boolean isDoubleSided;
    private final double width;
    private final double height;
    private final double depth;
    private final double offsetX;
    private final double offsetY;
    private final double offsetZ;
    private final float doorValue;
    private final float angleDegrees;
    private final double angleRadians;
    private final boolean doorway1Openable;
    private final boolean doorway2Openable;

    public LiftWrapper(MinecraftClientData.LiftWrapper liftWrapper, World worldView) {
        this.liftObject = liftWrapper.getLift();
        this.floors = new ArrayList<>();
        this.shouldRender = liftWrapper.shouldRender;
        this.id = liftWrapper.getLift().getId();
        this.direction = liftWrapper.getLift().getDirection().sign;
        this.width = liftWrapper.getLift().getWidth();
        this.height = liftWrapper.getLift().getHeight();
        this.depth = liftWrapper.getLift().getDepth();
        this.offsetX = liftWrapper.getLift().getOffsetX();
        this.offsetY = liftWrapper.getLift().getOffsetY();
        this.offsetZ = liftWrapper.getLift().getOffsetZ();
        this.isDoubleSided = liftWrapper.getLift().getIsDoubleSided();
        this.doorValue = liftWrapper.getLift().getDoorValue();

        Angle angle = liftWrapper.getLift().getAngle();
        this.angleDegrees = angle.angleDegrees;
        this.angleRadians = angle.angleRadians;

        PositionAndRotation absolutePositionAndRotation = getLiftPositionAndRotation(ClientWorld.cast(worldView), liftObject);
        this.pos = new ScriptVector3f(absolutePositionAndRotation.position);

        /* Populate floors */
        liftWrapper.getLift().iterateFloors(liftFloor -> {
            BlockPos floorBlockPos = new BlockPos((int)liftFloor.getPosition().getX(), (int)liftFloor.getPosition().getY(), (int)liftFloor.getPosition().getZ());
            BlockEntity be = worldView.getBlockEntity(floorBlockPos);

            // Block entity contains all the necessary info TSC does not sync over
            BlockLiftTrackFloor.BlockEntity floorBE = be == null ? null : (BlockLiftTrackFloor.BlockEntity)be.data;

            Floor floor = new Floor(liftWrapper.getLift(), liftFloor, floorBE);
            this.floors.add(floor);
        });
        this.floors.sort((e, f) -> e.index - f.index);
        Box doorway1 = new Box(-0.75F, 0.0F, -liftObject.getDepth() / (double)2.0F + (double)0.25F, 0.75F, 0.0F, -liftObject.getDepth() / (double)2.0F);
        Box doorway2 = new Box(-0.75F, 0.0F, liftObject.getDepth() / (double)2.0F - (double)0.25F, 0.75F, 0.0F, liftObject.getDepth() / (double)2.0F);

        if (!liftObject.hasCoolDown()) {
            doorway1Openable = false;
            doorway2Openable = false;
        } else {
            doorway1Openable = MTRUtil.canOpenDoors(doorway1, absolutePositionAndRotation);
            doorway2Openable = liftObject.getIsDoubleSided() && MTRUtil.canOpenDoors(doorway2, absolutePositionAndRotation);
        }
    }

    public Lift getMtrLift() {
        return this.liftObject;
    }

    public boolean shouldRender() {
        return this.shouldRender;
    }

    public long getId() {
        return this.id;
    }

    public double getWidth() {
        return this.width;
    }

    public double getHeight() {
        return this.height;
    }

    public double getDepth() {
        return this.depth;
    }

    public ScriptVector3f getOffset() {
        return new ScriptVector3f((float)this.offsetX, (float)this.offsetY, (float)this.offsetZ);
    }

    public float getAngleDegrees() {
        return Angle.fromAngle(-90 + this.angleDegrees).angleDegrees;
    }

    public double getAngleRadians() {
        return this.angleRadians;
    }

    public boolean isDoubleSided() {
        return this.isDoubleSided;
    }

    public float getDoorValue() {
        return this.doorValue;
    }

    public boolean isDoorway1Openable() {
        return this.doorway1Openable;
    }

    public boolean isDoorway2Openable() {
        return this.doorway2Openable;
    }

    public boolean isDoorway1Open() {
        return isDoorway1Openable() && getDoorValue() > 0;
    }

    public boolean isDoorway2Open() {
        return isDoorway2Openable() && getDoorValue() > 0;
    }

    public int getDirection() {
        return this.direction;
    }

    public ScriptVector3f getPos() {
        return this.pos.copy().add(getOffset());
    }

    public ScriptVector3f getRawPos() {
        return this.pos.copy();
    }

    public List<Floor> getFloors() {
        return new ArrayList<>(this.floors);
    }

    public boolean isClientPlayerRiding() {
        return VehicleRidingMovement.isRiding(liftObject.getId());
    }

    @ApiInternal
    public boolean styleChanged(String style) {
        Lift lift = MinecraftClientData.getLift(id); // our reference of lift may be discarded, and thus don't have up to date info
        if(lift == null) return true;
        return !style.equals(lift.getStyle());
    }

    @ApiInternal
    public boolean removed() {
        return MinecraftClientData.getLift(id) == null;
    }

    @ApiInternal
    private static PositionAndRotation getLiftPositionAndRotation(ClientWorld clientWorld, Lift lift) {
        Vector position = lift.getPosition((floorPosition1, floorPosition2) -> ItemLiftRefresher.findPath(new World(clientWorld.data), floorPosition1, floorPosition2));
        return new PositionAndRotation(new Vector(position.x + lift.getOffsetX(), position.y + lift.getOffsetY(), position.z + lift.getOffsetZ()), (-Math.PI / 2D) - lift.getAngle().angleRadians, (double)0.0F);
    }

    public static class Floor {
        private final int index;
        private final String number;
        private final String description;
        private final ScriptVector3f pos;
        private final boolean isTargetFloor;
        private final boolean isCurrentFloor;

        public Floor(Lift currentLift, LiftFloor liftFloor, BlockLiftTrackFloor.BlockEntity be) {
            this.pos = new ScriptVector3f(liftFloor.getPosition());
            this.number = be == null ? liftFloor.getNumber() : be.getFloorNumber();
            this.description = be == null ? liftFloor.getDescription() : be.getFloorDescription();
            this.index = currentLift.getFloorIndex(liftFloor.getPosition());
            this.isTargetFloor = currentLift.hasInstruction(this.index).contains(LiftDirection.NONE);
            this.isCurrentFloor = currentLift.getCurrentFloor().getPosition().equals(liftFloor.getPosition());
        }

        public int getIndex() {
            return this.index;
        }

        public String getNumber() {
            return this.number;
        }

        public String getDescription() {
            return this.description;
        }

        public ScriptVector3f getPos() {
            return this.pos.copy();
        }

        public boolean isCurrentFloor() {
            return this.isCurrentFloor;
        }

        public boolean isTargetFloor() {
            return this.isTargetFloor;
        }
    }
}
