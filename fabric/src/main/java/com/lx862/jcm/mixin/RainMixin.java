package com.lx862.jcm.mixin;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.NotNull;
import org.mtr.core.data.Vehicle;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.Box;
import org.mtr.mapping.holder.Vector3d;
import org.mtr.mod.InitClient;
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
    public void getPrecipitation(Heightmap.Type heightmap, int x, int z, CallbackInfoReturnable<Integer> cir) {
        if(heightmap == Heightmap.Type.MOTION_BLOCKING) {
            outerLoop:
            for (VehicleExtension vehicle : MinecraftClientData.getInstance().vehicles) {
                final ObjectArrayList<RenderVehicleHelper.VehicleProperties> vehiclePropertiesList = RenderVehicleHelper.getTransformedVehiclePropertiesList(vehicle, vehicle.getVehicleCarsAndPositions().stream()
                        .map(vehicleCarAndPosition -> new RenderVehicleHelper.VehicleProperties(vehicleCarAndPosition, !vehicle.getTransportMode().hasPitchAscending && !vehicle.getTransportMode().hasPitchDescending))
                        .collect(Collectors.toCollection(ObjectArrayList::new)), new Vector3d(0, 0, 0));
                for (int i = 0; i < vehiclePropertiesList.size(); i++) {
                    Box box = getBox(vehicle, vehiclePropertiesList, i);

                    if (x >= box.getMinXMapped() && x <= box.getMaxXMapped() && z >= box.getMinZMapped() && z <= box.getMaxZMapped()) {
                        cir.setReturnValue((int)Math.ceil(box.getMaxYMapped()));
                        break outerLoop;
                    }
                }
            }
        }
    }

    @Unique
    private static @NotNull Box getBox(Vehicle vehicle, ObjectArrayList<RenderVehicleHelper.VehicleProperties> vehiclePropertiesList, int i) {
        RenderVehicleHelper.VehicleProperties vehicleProperties = vehiclePropertiesList.get(i);
        final RenderVehicleTransformationHelper renderVehicleTransformationHelperAbsolute = vehicleProperties.renderVehicleTransformationHelperOffset;

        final double w = vehicle.vehicleExtraData.immutableVehicleCars.get(i).getWidth();
        final double l = vehicle.vehicleExtraData.immutableVehicleCars.get(i).getLength();

        Vector3d aPos = new Vector3d(
                -w,
                4,
                -l / 2
        );
        Vector3d bPos = new Vector3d(
                +w,
                0,
                +l / 2
        );

        aPos = renderVehicleTransformationHelperAbsolute.transformForwards(aPos, Vector3d::rotateX, Vector3d::rotateY, Vector3d::add);
        bPos = renderVehicleTransformationHelperAbsolute.transformForwards(bPos, Vector3d::rotateX, Vector3d::rotateY, Vector3d::add);
        return new Box(aPos, bPos);
    }
}