package com.lx862.jcm.mod.block.behavior;

import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.mapping.holder.PlayerEntity;
import org.mtr.mapping.holder.SoundCategory;
import org.mtr.mapping.holder.Text;
import org.mtr.mapping.holder.World;
import org.mtr.mod.SoundEvents;
import org.mtr.mod.data.TicketSystem;

public interface EnquiryMachineBehavior {
    default void enquiry(World world, PlayerEntity player) {
        int score = TicketSystem.getBalance(world, player);
        player.sendMessage(Text.cast(TextUtil.translatable("gui.mtr.balance", String.valueOf(score))), true);
        world.playSound((PlayerEntity) null, player.getBlockPos(), SoundEvents.TICKET_PROCESSOR_ENTRY.get(), SoundCategory.BLOCKS, 1, 1);
    }
}
