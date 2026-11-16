package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.data.JCMServerStats;
import com.lx862.jcm.mod.registry.BlockEntities;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.MinecraftServerHelper;
import org.mtr.mapping.mapper.SoundHelper;
import org.mtr.mod.block.IBlock;

import java.util.concurrent.atomic.AtomicBoolean;

public class AutoIronDoorBlockEntity extends JCMBlockEntityBase {
    public static final int DETECT_RADIUS = 3;
    private static final Identifier DOOR_OPEN_SOUND_ID = new Identifier("minecraft:block.iron_door.open");
    private static final Identifier DOOR_CLOSE_SOUND_ID = new Identifier("minecraft:block.iron_door.close");

    public AutoIronDoorBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockEntities.AUTO_IRON_DOOR.get(), blockPos, blockState);
    }

    @Override
    public void blockEntityTick() {
        World world = getWorld2();

        if(world != null && !world.isClient() && JCMServerStats.getGameTick() % 5 == 0) {
            BlockPos pos = getPos2();
            BlockState state = world.getBlockState(pos);

            Box box = new Box(pos.getX() - DETECT_RADIUS, pos.getY() - DETECT_RADIUS, pos.getZ() - DETECT_RADIUS, pos.getX() + DETECT_RADIUS, pos.getY() + DETECT_RADIUS, pos.getZ() + DETECT_RADIUS);
            AtomicBoolean playerDetected = new AtomicBoolean(false);

            boolean doorOpened = IBlock.getStatePropertySafe(state, new Property<>(DoorBlockAbstractMapping.getOpenMapped().data));

            MinecraftServerHelper.iteratePlayers(ServerWorld.cast(getWorld2()), (player) -> {
                if(playerDetected.get()) return;

                if(!player.isSpectator() && box.contains(player.getPos())) {
                    if(!doorOpened) {
                        world.playSound(null, pos, SoundHelper.createSoundEvent(DOOR_OPEN_SOUND_ID), SoundCategory.BLOCKS, 1, 1);
                        world.setBlockState(pos, state.with(new Property<>(DoorBlockAbstractMapping.getOpenMapped().data), true));
                    }
                    playerDetected.set(true);
                }
            });

            if(!playerDetected.get() && doorOpened) {
                world.playSound(null, getPos2(), SoundHelper.createSoundEvent(DOOR_CLOSE_SOUND_ID), SoundCategory.BLOCKS, 1, 1);
                world.setBlockState(getPos2(), state.with(new Property<>(DoorBlockAbstractMapping.getOpenMapped().data), false));
            }
        }
    }
}