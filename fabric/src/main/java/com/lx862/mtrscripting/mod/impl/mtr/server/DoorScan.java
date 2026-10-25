package com.lx862.mtrscripting.mod.impl.mtr.server;

import org.mtr.core.data.Rail;
import org.mtr.core.data.RailMath;
import org.mtr.core.data.TransportMode;
import org.mtr.core.tool.Vector;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.Box;
import org.mtr.mapping.holder.World;
import org.mtr.mod.Init;
import org.mtr.mod.block.PlatformHelper;

/**
 * Server-side geometry for the door-opening-direction feature: walks a
 * platform rail and checks, for each side, whether the same blocks
 * {@code RenderVehicleHelper#canOpenDoors} would look at when a train
 * dwells there can actually open its doors against.
 *
 * <p>This mirrors the client scan in
 * {@code com.lx862.jcm.mod.util.MTRUtil#canOpenDoors} — a passive port of
 * MTR's own per-doorway check — with two adaptations for running on a
 * server where no vehicle exists:
 *
 * <ul>
 *   <li><b>No vehicle, so the cars are virtual.</b> A train that stops at
 *       the platform has its head (bogie-1 end) somewhere along the rail;
 *       door-openability only depends on the strip of blocks beside the
 *       track, so the rail is sampled every {@link #STRIDE} blocks and each
 *       sample poses a virtual car exactly the way MTR's
 *       {@code Vehicle#getVehicleCarsAndPositions} would lay out a car
 *       whose bogies straddle that sample: the yaw is the
 *       bogie-1&nbsp;→&nbsp;bogie-2 direction (which points <i>backwards</i>
 *       along a non-reversed train, exactly like the client's
 *       {@code PositionAndRotation}), and the checked doorway boxes are the
 *       same lateral strips the client uses (local {@code -X} is the head's
 *       left, MTR model convention).</li>
 *   <li><b>Sides are canonicalised to the rail's intrinsic direction.</b>
 *       The caller asks for the sides in the frame of a train whose head
 *       points from {@code getPosition1()} to {@code getPosition2()} when
 *       {@code position1 < position2} under {@code Position#compareTo}
 *       (and the reverse otherwise) — the very same "canonical forward"
 *       {@code PathData#getOrderedPosition1/2} defines for client path
 *       segments. {@code VehicleWrapper} converts between that frame and a
 *       concrete train's head-relative frame with the single
 *       {@code reversed == pathData.reversePositions} composition, so
 *       forward and reversed trains, and trains from opposite routes
 *       calling at the same platform, all converge on one stored answer
 *       that never contradicts itself.</li>
 * </ul>
 *
 * <p><b>The block test is platform-only.</b> Per MTR's actual train behavior
 * (in-game verified by the server owners): whether PSD/APG door blocks
 * exist does not influence whether trains open their doors — only platform
 * blocks do. The earlier port of this scan also credited an <i>unlocked</i>
 * PSD/APG door ({@code RenderVehicleHelper#canOpenDoors} does, for its
 * visual door sync), but that clause can only ever mislead here: a scan
 * with no train present always sees PSD/APG doors locked, and doors whose
 * block entities cannot tick (custom JCM door variants are known to end up
 * "invalid for ticking" in some mod packs) never unlock at all — so a
 * door-aware scan could only produce false negatives, never a correction.
 * {@link #isDoorOpenableBlock} therefore tests platform blocks only, which
 * matches both the real door-opening rule and every PSD/APG layout, because
 * the platform blocks the trains actually open against stay within the same
 * lattice either way.
 */
final class DoorScan {

    /**
     * Half width used for the virtual cars. The client uses each car's real
     * {@code width / 2}; nearly every MTR rolling stock model is 2.2 blocks
     * wide, and the ±1-block lattice expansion makes any half width between
     * roughly 0.6 and 1.4 land on the same block column beside the track, so
     * a single constant reproduces the client's answer for every
     * MTR-conformant platform layout.
     */
    private static final double HALF_WIDTH = 1.1;

    /** Half length of one sample's virtual car: samples advance {@link #STRIDE}, so adjacent strips touch. */
    private static final double HALF_LENGTH = 0.25;
    private static final double STRIDE = 0.5;

    /**
     * Separation of the two joggled bogie points that define a sample's
     * pose, mirroring {@code Vehicle#getBogiePositions} (which clamps the
     * joggle to 0.1–1 blocks); 0.1 is the mid-platform value and keeps the
     * pose exact on curves.
     */
    private static final double POSE_EPS = 0.1;

    /* Lattice expansion, identical to MTRUtil.canOpenDoors. */
    private static final int CHECK_DOOR_RADIUS_XZ = 1;
    private static final int CHECK_DOOR_RADIUS_Y = 2;

    private DoorScan() {
    }

    /**
     * A sampled pose of a virtual car on the rail: the fields MTR's
     * {@code PositionAndRotation} would derive, computed without touching
     * any client class so this runs on dedicated servers.
     */
    private static final class Pose {
        final Vector position;
        final double yaw;
        final double pitch;

        Pose(Vector bogieFront, Vector bogieRear, boolean hasPitch) {
            position = Vector.getAverage(bogieFront, bogieRear);
            /* PositionAndRotation#getYaw: atan2(x2 - x1, z2 - z1) over the
               bogie1 → bogie2 pair; bogie1 is the head end, so this yaw
               points backwards along a head-forward train — by design. */
            yaw = Math.atan2(bogieRear.x() - bogieFront.x(), bogieRear.z() - bogieFront.z());
            /* PositionAndRotation#getPitch over the same pair; trains have
               no pitch, sloped modes rotate the doorway boxes with it. */
            pitch = hasPitch ? Math.atan2(
                    bogieRear.y() - bogieFront.y(),
                    Math.hypot(bogieRear.x() - bogieFront.x(), bogieRear.z() - bogieFront.z())
            ) : 0;
        }

        /** Local-space point to world-space, the exact transformForwards chain: rotateX(pitch), rotateY(yaw), translate. */
        Vector transform(double x, double y, double z) {
            return new Vector(x, y, z).rotateX(pitch).rotateY(yaw).add(position.x(), position.y(), position.z());
        }
    }

    /**
     * Scans the blocks beside one platform rail and reports the two
     * canonical openable sides.
     *
     * @param world the server world the rail lives in (block reads may
     *              force-load chunks; the caller budgets how many rails are
     *              scanned per tick)
     * @param rail  a rail with {@code isPlatform() == true}
     * @return {@code [left, right]} in the rail's intrinsic
     *         (ordered-position) frame: left is the {@code -X} side of a
     *         head-forward, non-reversed train travelling along the
     *         canonical forward direction
     */
    static boolean[] scanRailSides(World world, Rail rail) {
        final boolean[] result = new boolean[]{false, false};
        final RailMath railMath = rail.railMath;
        final double length = railMath.getLength();
        /* Rail#isValid (public) includes RailMath#isValid (package-private
           here) plus the speed-limit sanity — the same readiness MTR itself
           requires before a rail can carry trains. */
        if(!rail.isValid() || length <= 0) return result;

        final TransportMode transportMode = rail.getTransportMode();
        final boolean hasPitch = transportMode.hasPitchAscending || transportMode.hasPitchDescending;

        boolean left = false;
        boolean right = false;
        for(double u = 0; u <= length && !(left && right); u += STRIDE) {
            /* Bogie pair straddling the sample, joggled like MTR does and
               clamped to the rail ends the same way a stopped train's
               bogies are clamped to its stopping window.

               The Rail constructor always builds its RailMath from the
               ordered position pair (it swaps both ends when
               position1.compareTo(position2) > 0), so the railMath
               parametrization starts at orderedPosition1 and runs toward
               orderedPosition2 — the canonical forward direction — with
               reverse == false, whatever order the rail's own endpoints
               were declared in. */
            final Vector bogieFront = railMath.getPosition(Math.min(u + POSE_EPS, length), false);
            final Vector bogieRear = railMath.getPosition(Math.max(u - POSE_EPS, 0), false);
            if(bogieFront == null || bogieRear == null) continue;
            final Pose pose = new Pose(bogieFront, bogieRear, hasPitch);

            if(!left) {
                left = canOpenDoors(world, pose, new Box(
                        -HALF_WIDTH - 0.1, -1, -HALF_LENGTH,
                        -HALF_WIDTH, 1, HALF_LENGTH
                ));
            }
            if(!right) {
                right = canOpenDoors(world, pose, new Box(
                        HALF_WIDTH, -1, -HALF_LENGTH,
                        HALF_WIDTH + 0.1, 1, HALF_LENGTH
                ));
            }
        }
        result[0] = left;
        result[1] = right;
        return result;
    }

    /**
     * Server twin of {@code MTRUtil#canOpenDoors(Box, PositionAndRotation)}:
     * transforms the four doorway-box corners to world space, expands the
     * resulting bounds by the client's lattice radii and asks each block in
     * that lattice the same question the client asks while dwelling here.
     * The world is a parameter so this never touches {@code MinecraftClient}.
     */
    private static boolean canOpenDoors(World world, Pose pose, Box doorway) {
        final Vector position1 = pose.transform(doorway.getMinXMapped(), doorway.getMaxYMapped(), doorway.getMinZMapped());
        final Vector position2 = pose.transform(doorway.getMaxXMapped(), doorway.getMaxYMapped(), doorway.getMinZMapped());
        final Vector position3 = pose.transform(doorway.getMaxXMapped(), doorway.getMaxYMapped(), doorway.getMaxZMapped());
        final Vector position4 = pose.transform(doorway.getMinXMapped(), doorway.getMaxYMapped(), doorway.getMaxZMapped());
        final double minX = Math.min(Math.min(position1.x(), position2.x()), Math.min(position3.x(), position4.x()));
        final double maxX = Math.max(Math.max(position1.x(), position2.x()), Math.max(position3.x(), position4.x()));
        final double minY = Math.min(Math.min(position1.y(), position2.y()), Math.min(position3.y(), position4.y()));
        final double maxY = Math.max(Math.max(position1.y(), position2.y()), Math.max(position3.y(), position4.y()));
        final double minZ = Math.min(Math.min(position1.z(), position2.z()), Math.min(position3.z(), position4.z()));
        final double maxZ = Math.max(Math.max(position1.z(), position2.z()), Math.max(position3.z(), position4.z()));

        for(double checkX = minX - CHECK_DOOR_RADIUS_XZ; checkX <= maxX + CHECK_DOOR_RADIUS_XZ; checkX++) {
            for(double checkY = minY - CHECK_DOOR_RADIUS_Y; checkY <= maxY + CHECK_DOOR_RADIUS_Y; checkY++) {
                for(double checkZ = minZ - CHECK_DOOR_RADIUS_XZ; checkZ <= maxZ + CHECK_DOOR_RADIUS_XZ; checkZ++) {
                    if(isDoorOpenableBlock(world, Init.newBlockPos(checkX, checkY, checkZ))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * The block test, platform-only: per MTR's actual train behavior, only
     * platform blocks ({@link PlatformHelper}) decide whether trains open
     * their doors on a side — the presence of PSD/APG doors is irrelevant
     * to that decision, and a scan with no train present could only ever
     * see doors locked anyway, so they are deliberately not consulted.
     */
    private static boolean isDoorOpenableBlock(World world, BlockPos blockPos) {
        return world.getBlockState(blockPos).getBlock().data instanceof PlatformHelper;
    }
}
