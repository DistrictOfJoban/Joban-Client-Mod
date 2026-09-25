package com.lx862.mtrscripting.mod.impl.mtr.lift;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.util.ScriptVector3f;
import org.mtr.core.data.Lift;
import org.mtr.core.data.LiftDirection;
import org.mtr.core.data.LiftFloor;
import org.mtr.core.tool.Angle;
import org.mtr.core.tool.Vector;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.BlockEntity;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.World;
import org.mtr.mod.block.BlockLiftTrackFloor;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.client.VehicleRidingMovement;

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

        this.pos = new ScriptVector3f(getLiftPosition(liftWrapper.getLift()));

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
        return this.angleDegrees;
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
        return !style.equals(liftObject.getStyle());
    }

    @ApiInternal
    public boolean removed() {
        return MinecraftClientData.getLift(id) == null;
    }

    @ApiInternal
    private static Vector getLiftPosition(Lift lift) {
        return lift.getPosition((floorPosition1, floorPosition2) -> ObjectArrayList.of(
                new Vector(floorPosition1.getX(), floorPosition1.getY(),floorPosition1.getZ()),
                new Vector(floorPosition2.getX(), floorPosition2.getY(), floorPosition2.getZ())
        ));
    }

    public static class Floor {
        private final int index;
        private final String number;
        private final String description;
        private final ScriptVector3f pos;
        private final boolean isTargetFloor;
        private final boolean isCurrentFloor;

        public Floor(Lift currentLift, LiftFloor liftFloor, BlockLiftTrackFloor.BlockEntity be) {
            // TODO: Add TSC Position as constructor for v3f
            this.pos = new ScriptVector3f(liftFloor.getPosition().getX(), liftFloor.getPosition().getY(), liftFloor.getPosition().getZ());
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
