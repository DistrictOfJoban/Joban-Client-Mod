package com.lx862.jcm.mod.block.behavior;

import com.lx862.jcm.mod.data.TransactionLog;
import com.lx862.jcm.mod.data.EnquiryScreenType;
import com.lx862.jcm.mod.data.TransactionEntry;
import com.lx862.jcm.mod.network.gui.EnquiryUpdateGUIPacket;
import com.lx862.jcm.mod.registry.Networking;
import org.mtr.mapping.holder.*;
import org.mtr.mod.SoundEvents;
import org.mtr.mod.data.TicketSystem;

import java.util.List;

public interface EnquiryMachineBehavior {
    default void enquiry(EnquiryScreenType type, World world, PlayerEntity player) {
        world.playSound((PlayerEntity) null, player.getBlockPos(), SoundEvents.TICKET_PROCESSOR_ENTRY.get(), SoundCategory.BLOCKS, 1, 1);

        List<TransactionEntry> entries = TransactionLog.readLog(player, player.getUuidAsString());
        Networking.sendPacketToClient(player, new EnquiryUpdateGUIPacket(type, entries, TicketSystem.getBalance(world, player)));
    }
}
