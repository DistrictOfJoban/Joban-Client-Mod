package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.data.JCMServerStats;
import com.lx862.jcm.mod.registry.BlockEntities;
import org.mtr.mapping.holder.*;

public class SoundLooperBlockEntity extends JCMBlockEntityBase {
    public static final SoundCategory[] SOURCE_LIST = {SoundCategory.MASTER, SoundCategory.MUSIC, SoundCategory.WEATHER, SoundCategory.AMBIENT, SoundCategory.PLAYERS, SoundCategory.BLOCKS, SoundCategory.VOICE};
    private String soundID = "";
    private BlockPos corner1 = new BlockPos(0, 0, 0);
    private BlockPos corner2 = new BlockPos(0, 0, 0);
    private int repeatTick = 20;
    private float volume = 1;
    private int soundCategory = 0;
    private boolean needRedstone = false;
    private boolean limitRange = false;
    public SoundLooperBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockEntities.SOUND_LOOPER.get(), blockPos, blockState);
    }

    @Override
    public void readCompoundTag(CompoundTag compoundTag) {
        super.readCompoundTag(compoundTag);
        this.repeatTick = compoundTag.getInt("repeat_tick");
        this.soundID = compoundTag.getString("sound_id");
        this.soundCategory = compoundTag.getInt("sound_category");
        this.volume = compoundTag.getFloat("volume");
        this.needRedstone = compoundTag.getBoolean("need_redstone");
        this.limitRange = compoundTag.getBoolean("limit_range");
        this.corner1 = BlockPos.fromLong(compoundTag.getLong("pos_1"));
        this.corner2 = BlockPos.fromLong(compoundTag.getLong("pos_2"));
    }

    @Override
    public void writeCompoundTag(CompoundTag compoundTag) {
        super.writeCompoundTag(compoundTag);
        compoundTag.putInt("repeat_tick", repeatTick);
        compoundTag.putString("sound_id", soundID);
        compoundTag.putInt("sound_category", soundCategory);
        compoundTag.putFloat("volume", volume);
        compoundTag.putBoolean("need_redstone", needRedstone);
        compoundTag.putBoolean("limit_range", limitRange);
        compoundTag.putLong("pos_1", corner1.asLong());
        compoundTag.putLong("pos_2", corner2.asLong());
    }

    @Override
    public void blockEntityTick() {
        World world = getWorld2();

        if(repeatTick > 0 && !soundID.isEmpty() && world != null && !world.isClient() && JCMServerStats.getGameTick() % repeatTick == 0) {
            boolean bl1 = world.isEmittingRedstonePower(getPos2(), Direction.NORTH);
            boolean bl2 = world.isEmittingRedstonePower(getPos2(), Direction.EAST);
            boolean bl3 = world.isEmittingRedstonePower(getPos2(), Direction.SOUTH);
            boolean bl4 = world.isEmittingRedstonePower(getPos2(), Direction.WEST);
            boolean bl5 = world.isEmittingRedstonePower(getPos2(), Direction.UP);
            boolean bl6 = world.isEmittingRedstonePower(getPos2(), Direction.DOWN);
            boolean emittingRedstonePower = bl1 || bl2 || bl3 || bl4 || bl5 || bl6;
            if(needRedstone && !emittingRedstonePower) return;

            if(!limitRange) {
//                world.getPlayers().forEach(player -> {
//                    Identifier identifier = null;
//                    try {
//                        identifier = new Identifier(soundID);
//                    } catch (Exception ignored) {
//                    }
//
//                    if(identifier == null) return;
//                    final SoundCategory category = SOURCE_LIST[soundCategory];
//                    final long seed = world.getRandom().nextLong();
//                    // TODO: Implement the rest, need SoundEvent.of
//                });
            } else {

            }
        }
    }

    public String getSoundId() {
        return soundID == null ? "" : soundID;
    }

    public int getLoopInterval() {
        return repeatTick;
    }

    public int getSoundCategory() {
        if (soundCategory > SoundLooperBlockEntity.SOURCE_LIST.length) {
            soundCategory = 0;
        }
        return soundCategory;
    }

    public float getSoundVolume() {
        return volume;
    }

    public boolean rangeLimited() {
        return limitRange;
    }

    public boolean needRedstone() {
        return needRedstone;
    }

    public BlockPos getCorner1() {
        return corner1;
    }

    public BlockPos getCorner2() { // This used to be called getPos2, which happens to override MC Mappings method, causing the world to be null. Ended up spending an hour debugging it, horrifying.
        return corner2;
    }

    public void setData(String soundId, int soundCategory, int interval, float volume, boolean needRedstone, boolean limitRange, BlockPos corner1, BlockPos corner2) {
        this.soundID = soundId;
        this.repeatTick = interval;
        this.soundCategory = soundCategory;
        this.volume = volume;
        this.needRedstone = needRedstone;
        this.corner1 = corner1;
        this.corner2 = corner2;
        this.limitRange = limitRange;
        this.markDirty2();
    }
}