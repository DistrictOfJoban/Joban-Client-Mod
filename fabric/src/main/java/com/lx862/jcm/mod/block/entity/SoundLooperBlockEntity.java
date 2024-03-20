package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.data.JCMServerStats;
import com.lx862.jcm.mod.registry.BlockEntities;
import org.mtr.mapping.holder.*;

public class SoundLooperBlockEntity extends JCMBlockEntityBase {
    private String soundID = "";
    private BlockPos pos1 = new BlockPos(0, 0, 0);
    private BlockPos pos2 = new BlockPos(0, 0, 0);
    private int repeatTick = 20;
    private float volume = 1;
    private int soundCategory = 0;
    private boolean needRedstone = false;
    private boolean limitRange = false;
    private static final SoundCategory[] SOURCE_LIST = {SoundCategory.MASTER, SoundCategory.MUSIC, SoundCategory.WEATHER, SoundCategory.AMBIENT, SoundCategory.PLAYERS, SoundCategory.BLOCKS, SoundCategory.VOICE};

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
        this.pos1 = BlockPos.fromLong(compoundTag.getLong("pos_1"));
        this.pos2 = BlockPos.fromLong(compoundTag.getLong("pos_2"));
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
        compoundTag.putLong("pos_1", pos1.asLong());
        compoundTag.putLong("pos_2", pos2.asLong());
    }

    @Override
    public void blockEntityTick() {
        if(repeatTick > 0 && !soundID.isEmpty() && world != null && !world.isClient() && JCMServerStats.getGameTick() % repeatTick == 0) {
            if(needRedstone && !world.isReceivingRedstonePower(pos)) return;

            if(!limitRange) {
                world.getPlayers().forEach(player -> {
                    Identifier identifier = null;
                    try {
                         identifier = new Identifier(soundID);
                    } catch (Exception ignored) {
                    }

                    if(identifier == null) return;
                    final SoundCategory category = SOURCE_LIST[soundCategory];
                    final long seed = world.getRandom().nextLong();
                    // TODO: Implement the rest, need SoundEvent.of
                });
            } else {

            }
        }
    }

    public void setData(String soundId, int soundCategory, int interval, float volume, boolean needRedstone, boolean limitRange, BlockPos pos1, BlockPos pos2) {
        this.soundID = soundId;
        this.repeatTick = interval;
        this.soundCategory = soundCategory;
        this.volume = volume;
        this.needRedstone = needRedstone;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.limitRange = limitRange;
        markDirty2();
    }

    public String getSoundId() {
        return soundID == null ? "" : soundID;
    }

    public int getLoopInterval() {
        return repeatTick;
    }

    public int getSoundCategory() {
        if (soundCategory > SOURCE_LIST.length) {
            soundCategory = 0;
        }
        return soundCategory;
    }

    public float getVolume() {
        return volume;
    }

    public boolean needRedstone() {
        return needRedstone;
    }

    public boolean getLimitRange() {
        return limitRange;
    }

    public BlockPos getPos1() {
        return pos1;
    }

    public BlockPos getPos2() {
        return pos2;
    }
}
