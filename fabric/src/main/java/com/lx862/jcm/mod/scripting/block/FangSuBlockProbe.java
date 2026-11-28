package com.lx862.jcm.mod.scripting.block;

import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.BlockState;
import org.mtr.mapping.holder.MinecraftClient;
import org.mtr.mapping.holder.World;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Function;

/**
 * FangSuBlockProbe — the single soft-dependency surface between the ABI
 * 16 block-script kinds and the FangSu (方速) addon's blocks. All four
 * anchor blocks are resolved reflectively:
 *
 * <ul>
 *   <li>{@code com.fangsu.blockEntities.BlockEntityLevelCrossing} — the
 *       geometry snapshot (track yaw for barrier alignment);</li>
 *   <li>{@code com.fangsu.blockEntities.BlockEntityTicketBarrier} — the
 *       extraConfig string table (isOpen / fareType / fareVal / isExit)
 *       via the public getters on its FunctionalObjBlockEntity base;</li>
 *   <li>{@code com.fangsu.blocks.BlockTicketMachine} — block-level
 *       detection only (no block entity);</li>
 *   <li>{@code com.fangsu.blockEntities.BlockEntityAnnouncementSpeaker} —
 *       bindings() / leadSeconds() / allStops() / template() / variant().</li>
 * </ul>
 *
 * <p>No FangSu class is referenced at compile time. When the addon is
 * absent every probe reports "absent" and the drivers skip the binding —
 * the kinds are simply inert, exactly like the psd kind before a binding
 * exists. Class probes are rate-limited (1/s) so a late-loading FangSu
 * is picked up, mirroring {@code FangSuStopStatus}.</p>
 */
final class FangSuBlockProbe {

    private static final long PROBE_RETRY_NANOS = 1_000_000_000L;
    private static volatile long nextProbeNanos;

    private static volatile boolean probed;

    private static volatile Class<?> levelCrossingBe;
    private static volatile Class<?> ticketBarrierBe;
    private static volatile Class<?> ticketMachineBlock;
    private static volatile Class<?> announcementSpeakerBe;

    /* Resolved accessors (null while FangSu is absent). Records expose
       accessor methods, but public FIELD reads are kept as a defensive
       fallback for older FangSu shapes — both funnel into Functions. */
    private static volatile Function<Object, List<?>> snapshotTracks;  /* CellSnapshot -> tracks */
    private static volatile Function<Object, Double> trackYaw;         /* TrackSlice -> yawDeg */
    private static volatile Function<Object, Boolean> trackPrimary;    /* TrackSlice -> primary */
    private static volatile Method barrierGetExtraBool;                /* getExtraConfigBool(String, boolean) */
    private static volatile Method barrierGetExtraInt;                 /* getExtraConfigInt(String, int) */
    private static volatile Method speakerBindings;                    /* bindings() -> List<Binding> */
    private static volatile Method speakerLeadSeconds;                 /* leadSeconds() -> int */
    private static volatile Method speakerAllStops;                    /* allStops() -> boolean */
    private static volatile Method speakerTemplate;                    /* template() -> String */
    private static volatile Method speakerVariant;                     /* variant() -> String */
    private static volatile Method bindingRoute;                       /* Binding#routeId() -> long */
    private static volatile Method bindingPlatform;                    /* Binding#platformId() -> long */

    private FangSuBlockProbe() {
    }

    /* ------------------------------------------------------------------ */
    /* Anchor kinds                                                        */
    /* ------------------------------------------------------------------ */

    /** 0 = not a known anchor, 1 = level crossing, 2 = fare machine,
     *  3 = announcement speaker. */
    static int anchorKindOf(BlockPos pos) {
        probe();
        final org.mtr.mapping.holder.ClientWorld clientWorld = MinecraftClient.getInstance().getWorldMapped();
        if (clientWorld == null) return 0;
        final World world = World.cast(clientWorld);
        final Object blockEntity = rawBlockEntity(world, pos);
        if (blockEntity != null) {
            if (levelCrossingBe != null && levelCrossingBe.isInstance(blockEntity)) return 1;
            if (ticketBarrierBe != null && ticketBarrierBe.isInstance(blockEntity)) return 2;
            if (announcementSpeakerBe != null && announcementSpeakerBe.isInstance(blockEntity)) return 3;
        } else {
            final BlockState state = world.getBlockState(pos);
            final Object block = state.getBlock().data;
            if (ticketMachineBlock != null && ticketMachineBlock.isInstance(block)) return 2;
        }
        return 0;
    }

    static boolean present() {
        return levelCrossingBe != null || ticketBarrierBe != null
                || ticketMachineBlock != null || announcementSpeakerBe != null;
    }

    /* ------------------------------------------------------------------ */
    /* Level crossing geometry                                             */
    /* ------------------------------------------------------------------ */

    /** The primary track's yaw in degrees, -1 when unknown. */
    static double crossingTrackYaw(BlockPos pos) {
        if (levelCrossingBe == null) return -1;
        final org.mtr.mapping.holder.ClientWorld clientWorld = MinecraftClient.getInstance().getWorldMapped();
        final World world = clientWorld == null ? null : World.cast(clientWorld);
        final Object blockEntity = world == null ? null : rawBlockEntity(world, pos);
        if (blockEntity == null || !levelCrossingBe.isInstance(blockEntity)) return -1;
        try {
            final Object snapshot = invoke(method(levelCrossingBe, "getSnapshot"), blockEntity);
            if (snapshot == null) {
                /* Public field fallback: `public CellSnapshot snapshot`. */
                final Field field = field(levelCrossingBe, "snapshot");
                if (field != null) {
                    final Object value = field.get(blockEntity);
                    return primaryYawOf(value);
                }
                return -1;
            }
            return primaryYawOf(snapshot);
        } catch (Throwable t) {
            return -1;
        }
    }

    private static double primaryYawOf(Object snapshot) {
        final Function<Object, List<?>> tracks = snapshotTracks;
        final Function<Object, Double> yaw = trackYaw;
        if (snapshot == null || tracks == null || yaw == null) return -1;
        final List<?> list = tracks.apply(snapshot);
        if (list == null) return -1;
        Double primaryYaw = null, anyYaw = null;
        for (Object track : list) {
            if (track == null) continue;
            final Double value = yaw.apply(track);
            if (value == null) continue;
            if (anyYaw == null) anyYaw = value;
            final Function<Object, Boolean> isPrimary = trackPrimary;
            if (isPrimary != null && Boolean.TRUE.equals(isPrimary.apply(track))) {
                primaryYaw = value;
                break;
            }
        }
        return primaryYaw != null ? primaryYaw : anyYaw != null ? anyYaw : -1;
    }

    /* ------------------------------------------------------------------ */
    /* Ticket barrier state                                                */
    /* ------------------------------------------------------------------ */

    static final class BarrierState {
        boolean open;
        boolean exitSide;
        int fareType;
        int fareValue;
    }

    static BarrierState barrierState(BlockPos pos) {
        final BarrierState state = new BarrierState();
        if (ticketBarrierBe == null || barrierGetExtraBool == null) return state;
        final org.mtr.mapping.holder.ClientWorld clientWorld = MinecraftClient.getInstance().getWorldMapped();
        final World world = clientWorld == null ? null : World.cast(clientWorld);
        final Object blockEntity = world == null ? null : rawBlockEntity(world, pos);
        if (blockEntity == null || !ticketBarrierBe.isInstance(blockEntity)) return state;
        try {
            state.open = Boolean.TRUE.equals(barrierGetExtraBool.invoke(blockEntity, "isOpen", Boolean.FALSE));
            state.exitSide = Boolean.TRUE.equals(barrierGetExtraBool.invoke(blockEntity, "isExit", Boolean.FALSE));
            state.fareType = intValue(invokeCatching(barrierGetExtraInt, blockEntity, "fareType", 0));
            state.fareValue = intValue(invokeCatching(barrierGetExtraInt, blockEntity, "fareVal", 10));
        } catch (Throwable ignored) {
            /* FangSu changed shape — defaults stay. */
        }
        return state;
    }

    private static Object invokeCatching(Method m, Object target, Object a, Object b) {
        try {
            return m.invoke(target, a, b);
        } catch (Throwable t) {
            return null;
        }
    }

    /* ------------------------------------------------------------------ */
    /* Announcement speaker state                                          */
    /* ------------------------------------------------------------------ */

    static final class SpeakerState {
        final java.util.ArrayList<long[]> bindings = new java.util.ArrayList<>();
        int leadSeconds = 30;
        boolean allStops;
        String template = "";
        String variant = "";
    }

    static SpeakerState speakerState(BlockPos pos) {
        final SpeakerState state = new SpeakerState();
        if (announcementSpeakerBe == null || speakerBindings == null) return state;
        final org.mtr.mapping.holder.ClientWorld clientWorld = MinecraftClient.getInstance().getWorldMapped();
        final World world = clientWorld == null ? null : World.cast(clientWorld);
        final Object blockEntity = world == null ? null : rawBlockEntity(world, pos);
        if (blockEntity == null || !announcementSpeakerBe.isInstance(blockEntity)) return state;
        try {
            final List<?> bindings = (List<?>) speakerBindings.invoke(blockEntity);
            if (bindings != null) {
                for (Object binding : bindings) {
                    if (binding == null) continue;
                    state.bindings.add(new long[]{
                            longValue(invoke(bindingRoute, binding)),
                            longValue(invoke(bindingPlatform, binding))});
                }
            }
            state.leadSeconds = intValue(invoke(speakerLeadSeconds, blockEntity));
            state.allStops = Boolean.TRUE.equals(invoke(speakerAllStops, blockEntity));
            final String template = (String) invoke(speakerTemplate, blockEntity);
            final String variant = (String) invoke(speakerVariant, blockEntity);
            state.template = template == null ? "" : template;
            state.variant = variant == null ? "" : variant;
        } catch (Throwable ignored) {
            /* FangSu changed shape — defaults stay. */
        }
        return state;
    }

    /* ------------------------------------------------------------------ */

    /**
     * The block's facing (a "facing"-named Direction property), 0..5 MC
     * Direction ordinal, 2 (NORTH) as the fallback.
     *
     * <p>Deliberately reflection-only and free of any compile-time
     * {@code net.minecraft} reference: the source set is shared with the
     * Forge build (mojmap), where the yarn class names do not exist. The
     * reflective getters try both mapping dialects — {@code getProperties()}
     * is named identically everywhere, the state's value accessor is
     * {@code get} (yarn) or {@code getValue} (mojmap), and the returned
     * Direction is an enum on every mapping, so {@code Enum.ordinal()}
     * is universal.</p>
     */
    static int facingOf(BlockPos pos) {
        final org.mtr.mapping.holder.ClientWorld clientWorld = MinecraftClient.getInstance().getWorldMapped();
        if (clientWorld == null) return 2;
        final World world = World.cast(clientWorld);
        try {
            final BlockState state = world.getBlockState(pos);
            final Object rawState = state.data;
            final Object properties = rawState.getClass().getMethod("getProperties").invoke(rawState);
            for (Object property : (java.util.Collection<?>) properties) {
                if (!"DirectionProperty".equals(property.getClass().getSimpleName())) continue;
                final Object name = property.getClass().getMethod("getName").invoke(property);
                if (!"facing".equals(name)) continue;
                final Class<?> propertyBase = property.getClass().getSuperclass();
                Object value = null;
                for (String getter : new String[]{"get", "getValue"}) {
                    try {
                        value = rawState.getClass().getMethod(getter, propertyBase).invoke(rawState, property);
                        break;
                    } catch (NoSuchMethodException nextMapping) {
                        /* try the other dialect */
                    }
                }
                if (value instanceof Enum) {
                    return ((Enum<?>) value).ordinal();
                }
            }
        } catch (Throwable ignored) {
        }
        return 2;
    }

    private static Object rawBlockEntity(World world, BlockPos pos) {
        try {
            final org.mtr.mapping.holder.BlockEntity blockEntity = world.getBlockEntity(pos);
            return blockEntity == null ? null : blockEntity.data;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object invoke(Method method, Object target) {
        if (method == null) return null;
        try {
            return method.invoke(target);
        } catch (Throwable t) {
            return null;
        }
    }

    private static int intValue(Object value) {
        return value instanceof Integer ? (Integer) value : 0;
    }

    private static long longValue(Object value) {
        return value instanceof Long ? (Long) value : 0;
    }

    /* ------------------------------------------------------------------ */

    private static synchronized boolean probe() {
        final long now = System.nanoTime();
        if (probed && now < nextProbeNanos) return false;
        nextProbeNanos = now + PROBE_RETRY_NANOS;
        final ClassLoader loader = FangSuBlockProbe.class.getClassLoader();
        try {
            levelCrossingBe = classFor(loader, "com.fangsu.blockEntities.BlockEntityLevelCrossing");
            ticketBarrierBe = classFor(loader, "com.fangsu.blockEntities.BlockEntityTicketBarrier");
            ticketMachineBlock = classFor(loader, "com.fangsu.blocks.BlockTicketMachine");
            announcementSpeakerBe = classFor(loader, "com.fangsu.blockEntities.BlockEntityAnnouncementSpeaker");
            if (levelCrossingBe != null) {
                final Class<?> snapshotClass = classFor(loader, "com.fangsu.levelcrossing.CellSnapshot");
                if (snapshotClass != null) {
                    snapshotTracks = accessor(snapshotClass, "getTracks", "tracks");
                }
                final Class<?> trackClass = classFor(loader, "com.fangsu.levelcrossing.TrackSlice");
                if (trackClass != null) {
                    final Function<Object, Double> yaw = accessor(trackClass, "getYawDeg", "yawDeg");
                    trackYaw = yaw;
                    trackPrimary = accessor(trackClass, "isPrimary", "primary");
                }
            }
            if (ticketBarrierBe != null) {
                final Class<?> functionalBase = classFor(loader, "com.fangsu.blockEntities.FunctionalObjBlockEntity");
                final Class<?> owner = functionalBase != null ? functionalBase : ticketBarrierBe;
                barrierGetExtraBool = method(owner, "getExtraConfigBool", String.class, boolean.class);
                barrierGetExtraInt = method(owner, "getExtraConfigInt", String.class, int.class);
            }
            if (announcementSpeakerBe != null) {
                speakerBindings = method(announcementSpeakerBe, "bindings");
                speakerLeadSeconds = method(announcementSpeakerBe, "leadSeconds");
                speakerAllStops = method(announcementSpeakerBe, "allStops");
                speakerTemplate = method(announcementSpeakerBe, "template");
                speakerVariant = method(announcementSpeakerBe, "variant");
                final Class<?> bindingClass = classFor(loader,
                        "com.fangsu.blockEntities.BlockEntityAnnouncementSpeaker$Binding");
                if (bindingClass != null) {
                    bindingRoute = method(bindingClass, "routeId");
                    bindingPlatform = method(bindingClass, "platformId");
                }
            }
            probed = true;
            return present();
        } catch (Throwable t) {
            probed = true;
            return false;
        }
    }

    /** A method accessor with a public-field fallback, widened to a
     *  Function — records expose accessor methods; older shapes exposed
     *  plain public fields. Returns null when neither resolves. */
    @SuppressWarnings("unchecked")
    private static <R> Function<Object, R> accessor(Class<?> owner, String methodName, String fieldName) {
        final Method method = method(owner, methodName);
        if (method != null) {
            return target -> {
                try {
                    return (R) method.invoke(target);
                } catch (Throwable t) {
                    return null;
                }
            };
        }
        try {
            final Field field = owner.getField(fieldName);
            return target -> {
                try {
                    return (R) field.get(target);
                } catch (Throwable t) {
                    return null;
                }
            };
        } catch (Throwable t) {
            return null;
        }
    }

    private static Class<?> classFor(ClassLoader loader, String name) {
        try {
            return Class.forName(name, false, loader);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Method method(Class<?> owner, String name, Class<?>... params) {
        try {
            final Method m = owner.getMethod(name, params);
            m.setAccessible(true);
            return m;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Field field(Class<?> owner, String name) {
        try {
            final Field f = owner.getField(name);
            f.setAccessible(true);
            return f;
        } catch (Throwable t) {
            return null;
        }
    }
}
