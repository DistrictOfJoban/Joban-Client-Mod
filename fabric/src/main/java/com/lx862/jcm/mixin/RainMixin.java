package com.lx862.jcm.mixin;

import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import org.mtr.core.data.Vehicle;
import org.mtr.core.data.VehicleCar;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.ClientPlayerEntity;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.Vector3d;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.VehicleExtension;
import org.mtr.mod.render.RenderVehicleHelper;
import org.mtr.mod.render.RenderVehicleTransformationHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.stream.Collectors;

@Mixin(World.class)
public class RainMixin {
    @Inject(method = "getTopY", at = @At("HEAD"), cancellable = true)
    public void getTopY(Heightmap.Type heightmap, int x, int z, CallbackInfoReturnable<Integer> cir) {
        final ClientPlayerEntity player = MinecraftClient.getInstance().getPlayerMapped();
        if(player != null) {
            final Vector3d pPos = player.getPos();
            if(pPos.distanceTo(new Vector3d(x, pPos.getYMapped(), z)) <= 12 /* Rain radius */) {
                outerLoop:
                for (VehicleExtension vehicle : MinecraftClientData.getInstance().vehicles) {
                    final ObjectArrayList<RenderVehicleHelper.VehicleProperties> vehiclePropertiesList = RenderVehicleHelper.getTransformedVehiclePropertiesList(vehicle, vehicle.getVehicleCarsAndPositions().stream()
                            .map(vehicleCarAndPosition -> new RenderVehicleHelper.VehicleProperties(vehicleCarAndPosition, !vehicle.getTransportMode().hasPitchAscending && !vehicle.getTransportMode().hasPitchDescending))
                            .collect(Collectors.toCollection(ObjectArrayList::new)), Vector3d.getZeroMapped());
                    for (int i = 0; i < vehiclePropertiesList.size(); i++) {
                        final int trainY = getAlteredTopY(vehicle, vehiclePropertiesList, i, x, z);
                        if (trainY != Integer.MAX_VALUE) {
                            cir.setReturnValue(trainY);
                            break outerLoop;
                        }
                    }
                }
            }
        }
    }

    /**
     * Gets the new topY.
     * If the position has a train, it returns the y position of the top of the train.
     * Otherwise, it returns Integer.MAX_VALUE
     */
    @Unique
    private static int getAlteredTopY(Vehicle vehicle, ObjectArrayList<RenderVehicleHelper.VehicleProperties> vehiclePropertiesList, int i, int x, int z) {
        RenderVehicleHelper.VehicleProperties vehicleProperties = vehiclePropertiesList.get(i);
        final VehicleCar car = vehicle.vehicleExtraData.immutableVehicleCars.get(i);
        final RenderVehicleTransformationHelper renderVehicleTransformationHelperAbsolute = vehicleProperties.renderVehicleTransformationHelperAbsolute;

        final double halfW = car.getWidth() / 2;
        final double halfL = car.getLength() / 2;

        final Vector3d point = new Vector3d(x, 0, z);
        Vector3d A = new Vector3d(-halfW,4, -halfL - 1);
        // FIXME: We have to add 1 here, or part of the train width won't be covered. This seems to work properly, but I suspect some maths below might be wrong.
        Vector3d B = new Vector3d(+halfW + 1,4, -halfL - 1);
        Vector3d C = new Vector3d(+halfW + 1,4, +halfL + 1);
        Vector3d D = new Vector3d(-halfW,4, +halfL + 1);

        A = renderVehicleTransformationHelperAbsolute.transformForwards(A, Vector3d::rotateX, Vector3d::rotateY, Vector3d::add);
        B = renderVehicleTransformationHelperAbsolute.transformForwards(B, Vector3d::rotateX, Vector3d::rotateY, Vector3d::add);
        C = renderVehicleTransformationHelperAbsolute.transformForwards(C, Vector3d::rotateX, Vector3d::rotateY, Vector3d::add);
        D = renderVehicleTransformationHelperAbsolute.transformForwards(D, Vector3d::rotateX, Vector3d::rotateY, Vector3d::add);

        boolean isLeftCD = ((D.getXMapped() - C.getXMapped()) * (point.getZMapped() - C.getZMapped()) - (D.getZMapped() - C.getZMapped()) * (point.getXMapped() - C.getXMapped())) > 0;
        boolean isLeftDA = ((A.getXMapped() - D.getXMapped()) * (point.getZMapped() - D.getZMapped()) - (A.getZMapped() - D.getZMapped()) * (point.getXMapped() - D.getXMapped())) > 0;
        boolean isLeftAB = ((B.getXMapped() - A.getXMapped()) * (point.getZMapped() - A.getZMapped()) - (B.getZMapped() - A.getZMapped()) * (point.getXMapped() - A.getXMapped())) > 0;
        boolean isLeftBC = ((C.getXMapped() - B.getXMapped()) * (point.getZMapped() - B.getZMapped()) - (C.getZMapped() - B.getZMapped()) * (point.getXMapped() - B.getXMapped())) > 0;

        return (isLeftCD && isLeftDA && isLeftAB && isLeftBC) ? (int)Math.ceil(A.getYMapped()) : Integer.MAX_VALUE;
    }
}