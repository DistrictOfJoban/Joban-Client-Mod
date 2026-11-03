package com.lx862.jcm.mod.scripting.pids;

import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.annotation.ValueNullable;
import com.lx862.mtrscripting.core.util.ScriptVector3f;
import com.lx862.mtrscripting.mod.impl.mtr.pids.ArrivalsWrapper;
import org.mtr.core.data.Station;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongImmutableList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mod.InitClient;

public class PIDSWrapper {
    private final boolean[] rowHidden;
    private final String[] customMessages;
    private final boolean keyBlock;
    private final ArrivalsWrapper arrivalsWrapper;
    private final LongImmutableList targetPlatformIds;

    private final BlockPos blockPos;
    private final boolean isPlatformNumberHidden;
    private final boolean isAutoDetectPlatform;

    public final String type;
    public final int rows;
    public final int width;
    public final int height;

    @ApiInternal
    public PIDSWrapper(PIDSBlockEntity be, LongImmutableList targetPlatformIds, ObjectArrayList<ArrivalResponse> arrivalsResponse, int width, int height) {
        this(
                be.getPIDSType(), be.getRowAmount(), be.getPos2(), targetPlatformIds, arrivalsResponse, width, height,
                be.platformNumberHidden(), be.getPlatformIds().isEmpty(), be.isKeyBlock(), be.getRowHidden(), be.getCustomMessages()
        );
        /* The BE-derived constructor keeps its own reference so future getters
           added against `be` still work for real PIDS blocks. */
    }

    /**
     * Yanyang/PSD: BE-less constructor — used when a scripted PIDS preset is
     * bound to a platform screen door glass/top panel, where no
     * {@link PIDSBlockEntity} exists. The synthesized wrapper presents a
     * neutral PIDS identity (no custom messages, no hidden rows, no key
     * block) with the PSD's own canvas size and platform/arrival data.
     */
    @ApiInternal
    public PIDSWrapper(String type, int rows, BlockPos blockPos, LongImmutableList targetPlatformIds, ObjectArrayList<ArrivalResponse> arrivalsResponse, int width, int height, boolean platformNumberHidden, boolean platformAutoDetected, boolean keyBlock, boolean[] rowHidden, String[] customMessages) {
        this.type = type;
        this.width = width;
        this.height = height;
        this.rows = rows;
        this.targetPlatformIds = targetPlatformIds;
        this.blockPos = blockPos;
        this.isPlatformNumberHidden = platformNumberHidden;
        this.isAutoDetectPlatform = platformAutoDetected;
        this.keyBlock = keyBlock;
        this.rowHidden = rowHidden;
        this.customMessages = customMessages;
        this.arrivalsWrapper = new ArrivalsWrapper(arrivalsResponse);
    }

    public LongImmutableList getTargetPlatformIds() {
        return this.targetPlatformIds;
    }

    public ScriptVector3f blockPos() {
        return new ScriptVector3f(this.blockPos);
    }

    public boolean isKeyBlock() {
        return this.keyBlock;
    }

    public boolean isRowHidden(int i) {
        if(i >= rowHidden.length) {
            return false;
        } else {
            return rowHidden[i];
        }
    }

    public @ValueNullable String getCustomMessage(int i) {
        return i >= customMessages.length ? null : customMessages[i];
    }

    public boolean isPlatformNumberHidden() {
        return this.isPlatformNumberHidden;
    }

    public boolean isPlatformAutoDetected() {
        return this.isAutoDetectPlatform;
    }

    public @ValueNullable Station station() {
        return InitClient.findStation(this.blockPos);
    }

    public ArrivalsWrapper arrivals() {
        return this.arrivalsWrapper;
    }
}
