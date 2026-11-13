package com.lx862.jcm.test;

import com.lx862.jcm.mod.scripting.psd.*;
import org.mtr.mapping.holder.*;
import org.mtr.mod.block.*;

public final class PSDBindingsTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        final MinecraftClient client = MinecraftClient.getInstance();
        client.world = new ClientWorld();
        client.world.block = new BlockPSDAPGGlassBase();
        final BlockPos pos = new BlockPos(7, 12, 5);
        PSDClientBindings.reset();
        PSDClientBindings.tick();
        PSDClientBindings.acceptLocalBind(pos, "test:same_id", false, PSDWrapper.PSDType.GLASS);
        final PSDScriptInstance psd = new PSDScriptInstance(PSDClientBindings.get(pos), null);
        check(!psd.shouldInvalidate(), "unchanged PSD binding must stay live");

        PSDClientBindings.acceptLocalBind(pos, "test:same_id", true, PSDWrapper.PSDType.GLASS);
        check(psd.shouldInvalidate(), "same id in PIDS namespace must invalidate PSD instance");
        final PSDPidsScriptInstance pids = new PSDPidsScriptInstance(PSDClientBindings.get(pos), null);
        check(!pids.shouldInvalidate(), "PIDS preset must use its own binding identity");

        PSDClientBindings.acceptLocalBind(pos, "test:same_id", true, PSDWrapper.PSDType.TOP);
        check(pids.shouldInvalidate(), "same id on another surface needs a fresh instance");
        client.world.block = new BlockPSDTop();
        final PSDPidsScriptInstance top = new PSDPidsScriptInstance(PSDClientBindings.get(pos), null);
        check(!top.shouldInvalidate(), "top instance lives on matching top block");
        client.world.block = new BlockPSDAPGGlassBase();
        check(top.shouldInvalidate(), "physical surface replacement must invalidate top instance");

        PSDClientBindings.acceptLocalBind(pos, "test:same_id", false, PSDWrapper.PSDType.GLASS);
        final PSDScriptInstance beforeWorldChange = new PSDScriptInstance(PSDClientBindings.get(pos), null);
        client.world = new ClientWorld();
        client.world.block = new BlockPSDAPGGlassBase();
        check(beforeWorldChange.shouldInvalidate(), "same coordinates and binding in another world cannot reuse state");
        final PSDScriptInstance unbound = new PSDScriptInstance(PSDClientBindings.get(pos), null);
        PSDClientBindings.acceptLocalBind(pos, "", false, PSDWrapper.PSDType.GLASS);
        check(unbound.shouldInvalidate(), "unbinding must dispose the instance");

        PSDClientBindings.acceptLocalBind(pos, "test:same_id", false, PSDWrapper.PSDType.GLASS);
        final PSDScriptInstance disconnected = new PSDScriptInstance(PSDClientBindings.get(pos), null);
        client.world = null;
        check(disconnected.shouldInvalidate(), "disconnect must invalidate without reading a missing world");
        System.out.println("PSDBindingsTest: all source, surface, world and unbind assertions passed");
    }
}
