package com.lx862.jcm.mixin;

import org.mtr.mod.block.BlockPSDAPGDoorBase;
import org.mtr.mod.block.PlatformHelper;

/**
 * Loader-agnostic block test shared by the two {@code DoorBlockChangeMixin}
 * variants: the blocks whose presence or absence can change whether trains
 * can open their doors — platform blocks (any {@link PlatformHelper}) and
 * PSD/APG doors. Deliberately NOT an inner utility of the manager: the
 * mixins compile against raw loader-mapped types and only need this tiny
 * raw-level predicate, kept next to them.
 */
final class DoorBlockChangeHelper {

    private DoorBlockChangeHelper() {
    }

    static boolean isDoorBlock(Object block) {
        return block instanceof PlatformHelper || block instanceof BlockPSDAPGDoorBase;
    }
}
