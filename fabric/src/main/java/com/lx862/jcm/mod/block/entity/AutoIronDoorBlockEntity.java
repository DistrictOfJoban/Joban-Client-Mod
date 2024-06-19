package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.data.JCMServerStats;
import com.lx862.jcm.mod.registry.BlockEntities;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.MinecraftServerHelper;
import org.mtr.mapping.mapper.SoundHelper;

import java.util.concurrent.atomic.AtomicBoolean;

public class AutoIronDoorBlockEntity extends JCMBlockEntityBase {
    public static final int DETECT_RADIUS = 3;
    public AutoIronDoorBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockEntities.AUTO_IRON_DOOR.get(), blockPos, blockState);
    }

    @Override
    public void blockEntityTick() {
        World world = getWorld2();

        if(world != null && !world.isClient() && JCMServerStats.getGameTick() % 5 == 0) {
            BlockPos pos = getPos2();
            BlockState state = world.getBlockState(pos);
            if(state == null) return;

            Box box = new Box(pos.getX() - DETECT_RADIUS, pos.getY() - DETECT_RADIUS, pos.getZ() - DETECT_RADIUS, pos.getX() + DETECT_RADIUS, pos.getY() + DETECT_RADIUS, pos.getZ() + DETECT_RADIUS);
            AtomicBoolean haveNearbyPlayer = new AtomicBoolean(false);

            MinecraftServerHelper.iteratePlayers(ServerWorld.cast(getWorld2()), (player) -> {
                if(box.contains(player.getPos())) {
                    boolean alreadyOpened = BlockUtil.getProperty(state, new Property<>(DoorBlockAbstractMapping.getOpenMapped().data));

                    if(!alreadyOpened) {
                        world.playSound((PlayerEntity) null, pos, SoundHelper.createSoundEvent(new Identifier("minecraft:block.iron_door.open")), SoundCategory.BLOCKS, 1, 1);
                        world.setBlockState(pos, state.with(new Property<>(DoorBlockAbstractMapping.getOpenMapped().data), true));
                    }
                    haveNearbyPlayer.set(true);
                }
            });

            if(!haveNearbyPlayer.get() && BlockUtil.getProperty(state, new Property<>(DoorBlockAbstractMapping.getOpenMapped().data))) {
                world.playSound((PlayerEntity) null, getPos2(), SoundHelper.createSoundEvent(new Identifier("minecraft:block.iron_door.close")), SoundCategory.BLOCKS, 1, 1);
                world.setBlockState(getPos2(), state.with(new Property<>(DoorBlockAbstractMapping.getOpenMapped().data), false));
            }
        }
    }
}