package com.lx862.jcm.mod.util;

import com.lx862.jcm.mod.Constants;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.holder.PlayerEntity;

public class JCMUtil {
    /**
     * Whether the player is holding an MTR Brush / An item used for configuring blocks
     */
    public static boolean playerHoldingBrush(PlayerEntity player) {
        return player.isHolding(org.mtr.mod.Items.BRUSH.get());
    }

    public static boolean playerHoldingDriverKey(PlayerEntity player) {
        return player.isHolding(org.mtr.mod.Items.DRIVER_KEY.get());
    }

    /**
     * For block ID migration only, returns an identifier with the latest block id.
     */
    public static Identifier getMigrationId(Identifier checkId) {
        if (checkId.getNamespace().equals("jsblock")) {
            switch (checkId.getPath()) {
                case "bufferstop_1":
                    return new Identifier(Constants.MOD_ID, "buffer_stop");
                case "ceiling_1":
                    return new Identifier(Constants.MOD_ID, "ceiling_slanted");
                case "exit_sign_1":
                    return new Identifier(Constants.MOD_ID, "exit_sign_odd");
                case "faresaver_1":
                    return new Identifier(Constants.MOD_ID, "faresaver");
                case "helpline_3":
                    return new Identifier(Constants.MOD_ID, "helpline_standing_eal");
                case "helpline_4":
                    return new Identifier(Constants.MOD_ID, "helpline_standing");
                case "enquiry_machine_1":
                    return new Identifier(Constants.MOD_ID, "mtr_enquiry_machine");
                case "enquiry_machine_2":
                    return new Identifier(Constants.MOD_ID, "rv_enquiry_machine");
                case "enquiry_machine_3":
                    return new Identifier(Constants.MOD_ID, "mtr_enquiry_machine_wall");
                case "enquiry_machine_4":
                    return new Identifier(Constants.MOD_ID, "kcr_enquiry_machine");
                case "emg_stop_1":
                    return new Identifier(Constants.MOD_ID, "tcl_emg_stop_button");
                case "helpline_5":
                    return new Identifier(Constants.MOD_ID, "tml_emg_stop_button");
                case "helpline_6":
                    return new Identifier(Constants.MOD_ID, "sil_emg_stop_button");
                case "light_1":
                    return new Identifier(Constants.MOD_ID, "light_lantern");
                case "light_2":
                    return new Identifier(Constants.MOD_ID, "spot_lamp");
                case "mtr_stairs_1":
                    return new Identifier(Constants.MOD_ID, "mtr_stairs");
                case "op_button":
                    return new Identifier(Constants.MOD_ID, "operator_button");
                case "pids_4":
                    return new Identifier(Constants.MOD_ID, "pids_lcd");
                case "station_ceiling_1":
                    return new Identifier(Constants.MOD_ID, "station_ceiling_wrl");
                case "station_ceiling_1_station_color":
                    return new Identifier(Constants.MOD_ID, "station_ceiling_wrl_station_colored");
                case "station_ceiling_pole":
                    return new Identifier(Constants.MOD_ID, "station_ceiling_wrl_pole");
                case "station_name_tall_stand":
                    return new Identifier(Constants.MOD_ID, "station_name_standing");
                case "ticket_barrier_1_entrance":
                    return new Identifier(Constants.MOD_ID, "thales_ticket_barrier_entrance");
                case "ticket_barrier_1_exit":
                    return new Identifier(Constants.MOD_ID, "thales_ticket_barrier_exit");
                case "ticket_barrier_1_bare":
                    return new Identifier(Constants.MOD_ID, "thales_ticket_barrier_bare");
                case "inter_car_barrier_1_left":
                    return new Identifier(Constants.MOD_ID, "lrt_inter_car_barrier_left");
                case "inter_car_barrier_1_middle":
                    return new Identifier(Constants.MOD_ID, "lrt_inter_car_barrier_middle");
                case "inter_car_barrier_1_right":
                    return new Identifier(Constants.MOD_ID, "lrt_inter_car_barrier_right");
                case "subsidy_machine_1":
                    return new Identifier(Constants.MOD_ID, "subsidy_machine");
                case "trespass_sign_1":
                    return new Identifier(Constants.MOD_ID, "mtr_trespass_sign");
                case "trespass_sign_2":
                    return new Identifier(Constants.MOD_ID, "kcr_trespass_sign");
                case "trespass_sign_3":
                    return new Identifier(Constants.MOD_ID, "lrt_trespass_sign");
                case "water_machine_1":
                    return new Identifier(Constants.MOD_ID, "water_machine");
            }
        }
        return checkId;
    }
}
