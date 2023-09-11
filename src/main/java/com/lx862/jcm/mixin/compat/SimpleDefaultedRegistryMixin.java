package com.lx862.jcm.mixin.compat;

import net.minecraft.registry.SimpleDefaultedRegistry;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Block renamed in JCM 2.0.0, unfortunately there's no easy way to migrate ids.
 * This class helps migrate these ids
 * See <a href="https://github.com/Noaaan/MythicMetals/blob/1.20/src/main/java/nourl/mythicmetals/mixin/DefaultedRegistryMixin.java">here</a>
 * And <a href="https://github.com/orgs/FabricMC/discussions/2361">https://github.com/orgs/FabricMC/discussions/2361</a>
 */
@Mixin(SimpleDefaultedRegistry.class)
public class SimpleDefaultedRegistryMixin {

    @ModifyVariable(at = @At("HEAD"), method = "get(Lnet/minecraft/util/Identifier;)Ljava/lang/Object;", ordinal = 0, argsOnly = true)
    Identifier dataFixerRegistry(@Nullable Identifier id) {
        if (id != null && id.getNamespace().equals("jsblock")) {
            switch (id.getPath()) {
                case "ceiling_1":
                    return new Identifier(id.getNamespace(), "ceiling_slanted");
                case "exit_sign_1":
                    return new Identifier(id.getNamespace(), "hk_exit_sign_odd");
                case "helpline_3":
                    return new Identifier(id.getNamespace(), "helpline_standing_eal");
                case "helpline_4":
                    return new Identifier(id.getNamespace(), "helpline_standing");
                case "enquiry_machine_1":
                    return new Identifier(id.getNamespace(), "mtr_enquiry_machine");
                case "enquiry_machine_2":
                    return new Identifier(id.getNamespace(), "rv_enquiry_machine");
                case "enquiry_machine_3":
                    return new Identifier(id.getNamespace(), "mtr_enquiry_machine_wall");
                case "enquiry_machine_4":
                    return new Identifier(id.getNamespace(), "kcr_enquiry_machine");
                case "light_1":
                    return new Identifier(id.getNamespace(), "light_lantern");
                case "light_2":
                    return new Identifier(id.getNamespace(), "spot_lamp");
                case "emg_stop_1":
                    return new Identifier(id.getNamespace(), "tcl_emg_stop_button");
                case "helpline_5":
                    return new Identifier(id.getNamespace(), "tml_emg_stop_button");
                case "helpline_6":
                    return new Identifier(id.getNamespace(), "sil_emg_stop_button");
                case "mtr_stairs_1":
                    return new Identifier(id.getNamespace(), "mtr_stairs");
                case "op_button":
                    return new Identifier(id.getNamespace(), "operator_button");
                case "station_ceiling_1":
                    return new Identifier(id.getNamespace(), "wrl_station_ceiling");
                case "station_ceiling_1_pole":
                    return new Identifier(id.getNamespace(), "wrl_station_ceiling_pole");
                case "inter_car_barrier_1_left":
                    return new Identifier(id.getNamespace(), "lrt_inter_car_barrier_left");
                case "inter_car_barrier_1_middle":
                    return new Identifier(id.getNamespace(), "lrt_inter_car_barrier_middle");
                case "inter_car_barrier_1_right":
                    return new Identifier(id.getNamespace(), "lrt_inter_car_barrier_right");
                case "subsidy_machine_1":
                    return new Identifier(id.getNamespace(), "subsidy_machine");
                case "trespass_sign_1":
                    return new Identifier(id.getNamespace(), "mtr_trespass_sign");
                case "trespass_sign_2":
                    return new Identifier(id.getNamespace(), "kcr_trespass_sign");
                case "trespass_sign_3":
                    return new Identifier(id.getNamespace(), "lrt_trespass_sign");
                case "water_machine_1":
                    return new Identifier(id.getNamespace(), "water_machine");
            }
        }
        return id;
    }
}