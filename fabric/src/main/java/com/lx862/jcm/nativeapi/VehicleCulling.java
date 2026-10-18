package com.lx862.jcm.nativeapi;

import com.lx862.mtrscripting.mod.resource.MtrScriptingResourceManager;
import org.mtr.core.data.VehicleCar;
import org.mtr.core.tool.Vector;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import org.mtr.mapping.holder.Box;
import org.mtr.mapping.holder.Frustum;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.Vector3d;
import org.mtr.mod.data.VehicleExtension;

/**
 * Frustum culling for native (C++) vehicle scripts.
 *
 * <h2>Why this exists</h2>
 * {@code NativeVehicleDriver.render(vehicle)} runs the script module for every
 * vehicle {@code MinecraftClientData.vehicles} contains — which is every train
 * the client knows about, anywhere in the world. Each run costs a full snapshot
 * marshal, the module's whole canvas repaint for every car, and one texture
 * upload per dirty rectangle. A train that is nowhere near the camera pays all
 * of that for pixels that are never drawn.
 *
 * <h2>How the frustum is obtained</h2>
 * The frustum is captured from the same Fabric {@code WorldRenderEvents} hook
 * MTR itself caches its block-entity culling frustum from
 * ({@code org.mtr.mod.render.BlockEntityRenderCulling#init}) and published via
 * {@link NativeCullFrustum}. It is tested through MTR's own {@link Frustum}
 * mapping wrapper ({@link Frustum#isVisible(Box)} → {@code class_4604.method_23093}),
 * so a vehicle is culled exactly where MTR's frustum would reject a box of that
 * extent. MTR additionally occlusion-culls each car against the chunk it sits
 * in before queueing it for drawing, so a train hidden behind geometry is not
 * drawn either.
 *
 * <h2>Failure policy</h2>
 * Every path fails <em>open</em>: no frustum, no world, missing car positions or
 * an unexpected exception all mean "visible", so a mistake here can never blank
 * a train that should be drawn. A culled vehicle simply keeps its last drawn
 * model and canvas; the next visible frame updates both.
 */
public final class VehicleCulling {
    private VehicleCulling() {}

    /**
     * Padding around the car envelope, in blocks.
     *
     * <p>{@code Vehicle#getVehicleCarsAndPositions()} reports each car's two
     * bogie positions, and MTR's own occlusion box widens those by
     * {@code longestDimensions[i] / 2} on x and z plus 8 blocks on y. Rather
     * than re-derive per-axis extents we pad the whole envelope by this amount:
     * 10 blocks comfortably covers 25 m carriages and the 8-block y offset for
     * any single car, and only ever makes culling more conservative.
     */
    private static final double PADDING = 10.0;

    private static boolean haveFrustum() {
        final Frustum current = NativeCullFrustum.frustum();
        if (current == null || NativeCullFrustum.world() == null) return false;
        try {
            return NativeCullFrustum.world() == MinecraftClient.getInstance().getWorldMapped().data;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Whether {@code vehicle}'s native scripts need to run this frame.
     *
     * <p>Returns {@code true} (run them) unless the vehicle's envelope is
     * provably outside the camera frustum.
     */
    public static boolean shouldRender(VehicleExtension vehicle) {
        try {
            if (vehicle == null) return false;
            if (!haveFrustum()) return true;
            final Box box = vehicleBox(vehicle);
            if (box == null) return true;
            return NativeCullFrustum.frustum().isVisible(box);
        } catch (Throwable t) {
            return true;
        }
    }

    /**
     * World-space envelope around every car of {@code vehicle}, or {@code null}
     * when the position data is unavailable (which means "assume visible").
     */
    private static Box vehicleBox(VehicleExtension vehicle) {
        ObjectArrayList<ObjectObjectImmutablePair<VehicleCar, ObjectArrayList<ObjectObjectImmutablePair<Vector, Vector>>>> cars =
                vehicle.getVehicleCarsAndPositions();
        if (cars == null || cars.isEmpty()) return null;

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        boolean any = false;

        for (int i = 0; i < cars.size(); i++) {
            final ObjectArrayList<ObjectObjectImmutablePair<Vector, Vector>> positions = cars.get(i).right();
            if (positions == null) continue;
            for (int p = 0; p < positions.size(); p++) {
                final ObjectObjectImmutablePair<Vector, Vector> pair = positions.get(p);
                if (pair == null) continue;
                /* Unrolled: a two-element Vector[] was allocated per bogie pair
                 * per car per frame for every vehicle in the world. */
                final Vector left = pair.left();
                final Vector right = pair.right();
                if (left != null) {
                    any = true;
                    if (left.x() < minX) minX = left.x();
                    if (left.y() < minY) minY = left.y();
                    if (left.z() < minZ) minZ = left.z();
                    if (left.x() > maxX) maxX = left.x();
                    if (left.y() > maxY) maxY = left.y();
                    if (left.z() > maxZ) maxZ = left.z();
                }
                if (right != null) {
                    any = true;
                    if (right.x() < minX) minX = right.x();
                    if (right.y() < minY) minY = right.y();
                    if (right.z() < minZ) minZ = right.z();
                    if (right.x() > maxX) maxX = right.x();
                    if (right.y() > maxY) maxY = right.y();
                    if (right.z() > maxZ) maxZ = right.z();
                }
            }
        }
        if (!any) return null;

        return new Box(new Vector3d(minX - PADDING, minY - PADDING, minZ - PADDING),
                       new Vector3d(maxX + PADDING, maxY + PADDING, maxZ + PADDING));
    }

    /** Script ids of {@code vehicle} that resolve to a loaded native module. */
    static boolean hasNativeScript(VehicleExtension vehicle) {
        final int carCount = vehicle.vehicleExtraData.immutableVehicleCars.size();
        for (int i = 0; i < carCount; i++) {
            final String scriptId = MtrScriptingResourceManager.vehicle.getVehicleScriptEntryId(
                    vehicle.vehicleExtraData.immutableVehicleCars.get(i).getVehicleId());
            if (scriptId != null && NativeScriptManager.isLoaded(scriptId)) return true;
        }
        return false;
    }
}
