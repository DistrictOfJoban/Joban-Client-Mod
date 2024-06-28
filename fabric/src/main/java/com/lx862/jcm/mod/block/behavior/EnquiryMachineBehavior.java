package com.lx862.jcm.mod.block.behavior;

import com.lx862.jcm.mod.data.EnquiryScreenType;
import com.lx862.jcm.mod.data.TransactionEntry;
import com.lx862.jcm.mod.data.TransactionLog;
import com.lx862.jcm.mod.network.gui.EnquiryUpdateGUIPacket;
import com.lx862.jcm.mod.registry.Networking;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mod.SoundEvents;
import org.mtr.mod.data.TicketSystem;

import java.util.List;

public interface EnquiryMachineBehavior {
    default void enquiry(EnquiryScreenType type, BlockPos pos, World world, PlayerEntity player) {
        world.playSound(null, player.getBlockPos(), SoundEvents.TICKET_PROCESSOR_ENTRY.get(), SoundCategory.BLOCKS, 1, 1);

        if(type == EnquiryScreenType.NONE) {
            int score = TicketSystem.getBalance(world, player);
            player.sendMessage(Text.cast(TextUtil.translatable("gui.mtr.balance", String.valueOf(score))), true);
        } else {
            List<TransactionEntry> entries = TransactionLog.readLog(player, player.getUuidAsString());
            Networking.sendPacketToClient(player, new EnquiryUpdateGUIPacket(type, pos, entries, TicketSystem.getBalance(world, player)));
        }
    }
}
