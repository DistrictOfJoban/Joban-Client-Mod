/**
 * shm_channel.h — Yanyang "shared memory" transport layout (v2).
 *
 * ONE memory-mapped file, two participants in the SAME process:
 *
 *   - Java maps it with FileChannel#map (a MappedByteBuffer) and writes
 *     per-frame snapshot blobs into bump-allocated "windows" inside the
 *     WINDOWS area (see NativeShm.java on the JVM side);
 *   - C++ reaches the very same pages either through the mapping's base
 *     address, handed over once via jcm_shm_init (the Panama FFM plane on
 *     JDK 22+), or — new in v2 — through ITS OWN mapping of the same
 *     file, which the background POLLER thread establishes from a
 *     filesystem handshake. Two MAP_SHARED views of one file are
 *     coherent on every supported OS, which makes the v2 plane the
 *     first invocation path that crosses NO native-call API at all:
 *     Java publishes requests, C++ serves them from the poller thread,
 *     and the script->Java host services travel back as SERVICE
 *     requests that Java answers from its wait loop. No JNI, no FFM,
 *     no upcalls — plain shared pages on any JDK back to 16.
 *
 * <h2>Request paths (v2)</h2>
 * The CONTROL slot carries TWO request families, discriminated by CTL_KIND:
 *
 *   kind <  JCM_CMD_BASE       per-frame render (MtrResourceKind, snapshot
 *                              address + written length in the request)
 *   kind >= JCM_CMD_BASE       lifecycle/control command (open/close/meta/
 *                              dispose/register/shutdown); arguments ride the
 *                              ARG window (a Java-owned raw address) and
 *                              string replies land in the REPLY window.
 *
 * Whoever serves a request publishes the RESULT slot with the request's
 * seq LAST. A release-store of the request seq by Java pairs with the
 * acquire-load in the server, and the result publish pairs back the same
 * way, so no lock is needed on the exchange itself.
 *
 * <h2>Service requests (script -> Java, v2)</h2>
 * While a command/render is being served, the C++ side may need Java work
 * (font rasterize, model acquire, resource creation, API callbacks). It
 * publishes one SERVICE request into the SVC block (a 32-word argument
 * area whose meaning is keyed by SVC_ID; pointers travel as raw addresses
 * because both sides share one address space) and bumps SVC_SEQ; the Java
 * thread waiting for the command reply observes the request, services it
 * — writing out-parameters DIRECTLY at the raw addresses the request
 * carried, exactly like the FFM vtable contract — and bumps SVC_RSP.
 * The single in-flight slot is sufficient: the poller is the only C++
 * requester and each Java waiter services requests while it waits.
 *
 * <h2>Callback requests (Java -> C++, v2)</h2>
 * The reverse cold path — a JS-side proxy method re-entering a module C++
 * API callback — travels the CBQ block. It MUST NOT use the control slot:
 * the poller can be mid-command (blocked waiting for an SVC reply) when a
 * Java thread makes such a call, so the CBQ is drained by the poller in
 * BOTH of its wait loops (outer command scan and SVC spin). This is the
 * structural fix for the reentrancy deadlock: on the FFM plane the nested
 * downcall is direct, on the poller plane it is this queue.
 *
 * <h2>Bootstrap handshake (poller plane only)</h2>
 * The bridge's constructor (POSIX) / DllMain (Windows) starts the poller
 * BEFORE Java has created any region, so the poller scans well-known temp
 * directories for the handshake file "jcm_shm_hs_<pid>" (pid = THIS
 * process, the JVM). Its first line is the absolute path of the region
 * file, the second the region size, the third the region EPOCH — a fresh
 * value for every region creation, so a reload creates a new region and
 * the poller re-attaches even though the handshake name is stable. On
 * attach the poller validates magic/layout/epoch and publishes the epoch
 * into HDR_READY_EPOCH; Java acquire-polls that field under its own
 * mapping and proceeds only once it matches. The handshake files and the
 * region file are Java-owned and deleted on region close.
 *
 * Layout version history:
 *   1 — control/result slots, windows area (FFM kick + JNI-era bridge)
 *   2 — SVC + CBQ blocks, extended control/result fields, command ids,
 *       poller handshake header fields, claim-CAS request discipline
 *
 * License: MIT (aligned with Joban Client Mod's LICENSE)
 */
#ifndef MTR_SHM_CHANNEL_H
#define MTR_SHM_CHANNEL_H

#include <stdint.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

/* Bytes "JCMYSHM2" read as a little-endian u64. */
#define JCM_SHM_MAGIC        UINT64_C(0x324D48534D59434A)
#define JCM_SHM_LAYOUT_VERSION 2

/* Fixed region geometry (bytes). The windows area starts at 16 KiB; the
   control/result/svc/cbq slots live in the first 16 KiB. */
#define JCM_SHM_CONTROL_OFFSET 8192
#define JCM_SHM_CONTROL_SIZE   128
#define JCM_SHM_RESULT_OFFSET  8320
#define JCM_SHM_RESULT_SIZE    256
/* The SVC ring (4 x 320 B, see below) occupies 8576..9856; the CBQ ring
   (8 x 128 B) follows at 9856..10880. BOTH stay inside the first 16 KiB,
   well clear of the windows area — the first bring-up overlapped the CBQ
   at the single-slot SVC end (8896) and the slot scanner happily read
   callback-queue fields as service requests. */
#define JCM_SHM_SVC_OFFSET     8576
#define JCM_SHM_CBQ_OFFSET     9856
#define JCM_SHM_WINDOWS_OFFSET 16384

/* Header field offsets (Java writes these once at region creation). */
#define JCM_SHM_HDR_MAGIC     0   /* u64 = JCM_SHM_MAGIC                    */
#define JCM_SHM_HDR_VERSION   8   /* u32 = JCM_SHM_LAYOUT_VERSION          */
#define JCM_SHM_HDR_RESERVED  12  /* u32, zero                             */
#define JCM_SHM_HDR_SIZE      16  /* u64 = total region size               */
#define JCM_SHM_HDR_CONTROL   24  /* u64 = JCM_SHM_CONTROL_OFFSET          */
#define JCM_SHM_HDR_RESULT    32  /* u64 = JCM_SHM_RESULT_OFFSET           */
#define JCM_SHM_HDR_WINDOWS   40  /* u64: LOW u32 = JCM_SHM_WINDOWS_OFFSET;
                                     the HIGH u32 aliases HDR_EPOCH below
                                     (only the low half is meaningful — the
                                     geometry constants are compile-time) */
#define JCM_SHM_HDR_EPOCH     44  /* u32 = region epoch (fresh per create;
                                     overlaps the HIGH half of HDR_WINDOWS) */
#define JCM_SHM_HDR_READY     48  /* u64 = poller publish: attached epoch  */
#define JCM_SHM_HDR_MODE      56  /* u64 = region liveness bit:
                                     1 = poller-plane region, live and
                                         servable (REQUIRED for attach);
                                     0 = FFM-kick plane (never attached)
                                         OR POISONED — set to 0 by the
                                         CMD_SHUTDOWN handler and by any
                                         Java waiter that gives up on the
                                         region (timeout), so both servers
                                         stop serving an abandoned region
                                         and the poller detaches + rescans */
#define JCM_SHM_HDR_SVC       64  /* u64 = JCM_SHM_SVC_OFFSET              */
#define JCM_SHM_HDR_CBQ       72  /* u64 = JCM_SHM_CBQ_OFFSET              */

/* Control slot (Java writes the request, server reads + claims it). */
#define JCM_SHM_CTL_SEQ       0   /* u64: bumped by Java every request     */
#define JCM_SHM_CTL_MODULE    8   /* u64: BridgeModule handle              */
#define JCM_SHM_CTL_INSTANCE  16  /* u64: per-module instance id            */
#define JCM_SHM_CTL_KIND      24  /* u32: MtrResourceKind or 0x8000+ cmd   */
#define JCM_SHM_CTL_SNAP_LEN  28  /* u32: written snapshot bytes           */
#define JCM_SHM_CTL_SNAP_ADDR 32  /* u64: snapshot address                 */
#define JCM_SHM_CTL_ARG_ADDR  40  /* u64: command argument block address   */
#define JCM_SHM_CTL_ARG_LEN   48  /* u64: command argument block bytes     */
#define JCM_SHM_CTL_RPLY_ADDR 56  /* u64: string reply window address      */
#define JCM_SHM_CTL_RPLY_LEN  64  /* u64: reply window capacity            */

/* Command ids (CTL_KIND >= JCM_CMD_BASE). Argument block layouts are
   documented per command in the bridge implementation. */
#define JCM_CMD_BASE          0x8000u
#define JCM_CMD_OPEN          0x8001u  /* args: module path utf8 ->
                                          RES_EXTRA = handle                */
#define JCM_CMD_CLOSE         0x8002u  /* MODULE = handle                   */
#define JCM_CMD_ABI           0x8003u  /* MODULE -> RES_EXTRA = abi          */
#define JCM_CMD_SCRIPT_TYPE   0x8004u  /* MODULE -> reply utf8, len EXTRA   */
#define JCM_CMD_SCRIPT_ID     0x8005u  /* MODULE -> reply utf8, len EXTRA   */
#define JCM_CMD_STATE_SIZE    0x8006u  /* MODULE -> RES_EXTRA = state bytes */
#define JCM_CMD_DISPOSE       0x8007u  /* MODULE + args: key utf8, then
                                          u64 live addr + u64 live len      */
#define JCM_CMD_REGISTER      0x8008u  /* MODULE + args: u64 instance id
                                          then key utf8                     */
#define JCM_CMD_SHUTDOWN      0x800Au  /* poller detaches, back to scanning  */
#define JCM_CMD_RECLAIMER     0x800Bu  /* quiesce; RES_EXTRA = pending (diagnostics) */

/* Result slot (the request's server publishes it, seq echo LAST). */
#define JCM_SHM_RES_SEQ       0   /* u64: echo of the request seq           */
#define JCM_SHM_RES_STATUS    8   /* u32: JCM_SHM_* status                  */
#define JCM_SHM_RES_COUNT     12  /* u32: record count                     */
#define JCM_SHM_RES_RECORDS   16  /* u64 addr, u64 len, u64 cap             */
#define JCM_SHM_RES_STRINGS   40
#define JCM_SHM_RES_PIXELS    64
#define JCM_SHM_RES_MATRICES  88
#define JCM_SHM_RES_FLOATS    112
#define JCM_SHM_RES_EXTRA     128 /* u64: command return value (handle,
                                      abi, state size, reply length, ...) */
/* each arena entry: +0 address, +8 length (this frame), +16 capacity. */

/* Service block (C++ publishes a request, Java services it): a small
   RING of slots so a NESTED service (a CBQ module callback inside a
   service wait posting another service, which a deeper Java proxy may
   nest again) can publish while an outer slot is still unanswered — the
   single-slot design deadlocked exactly there. The poller is the only
   requester and takes slots by nesting depth (see bridge.cpp); the
   Java side scans every slot and answers pending requests with a
   per-slot exactly-once claim. The 32 argument words carry the
   vtable-shaped arguments of the service: every pointer as a raw
   address, every scalar widened/bit-cast to u64. */
#define JCM_SVC_SLOT_BYTES   320
#define JCM_SVC_SLOTS        4
/* per-slot fields (base = JCM_SHM_SVC_OFFSET + slot * 320): */
#define JCM_SHM_SVC_SEQ       0   /* u64: C++ bumps to publish (release)  */
#define JCM_SHM_SVC_RSP       8   /* u64: Java writes back the served seq */
#define JCM_SHM_SVC_ID        16  /* u32: service id                      */
#define JCM_SHM_SVC_RSV       20  /* u32, zero                             */
#define JCM_SHM_SVC_ARGS      24  /* 32 x u64 argument words               */
#define JCM_SHM_SVC_RET       280 /* u64: return value / reply address     */
#define JCM_SHM_SVC_RET2      288 /* u64: spare return word (unused)       */

/* Service ids — one per JcmFfmHostVTable entry (see ffm_host.h; the same
   adapters on the Java side serve both planes, so the semantics and the
   memory contracts are deliberately identical).
     0 acquire_model:    [0]=path addr [1]=path len
     1 release_model:    [0]=host handle
     2 rasterize_text:   [0]=utf8 [1]=len [2]=x [3]=y [4]=max_w [5]=r
                          [6]=g [7]=b [8]=pixels [9]=out_w [10]=out_h
     3 measure_font_text:[0]=font [1]=font_len [2]=utf8 [3]=len
                          [4]=size(bits) [5]=sx(bits) [6]=sy(bits)
                          [7]=out_metrics
     4 draw_font_text:   [0]=font [1]=font_len [2]=utf8 [3]=len
                          [4]=size(bits) [5]=x(bits) [6]=baseline(bits)
                          [7]=sx(bits) [8]=sy(bits) [9]=compression(bits)
                          [10]=argb [11]=clip_x0 [12]=clip_y0
                          [13]=clip_x1 [14]=clip_y1 [15]=pixels
                          [16]=width [17]=height [18]=out_bounds
     5 create_resources: [0]=tex_count [1]=quad_count [2]=widths
                          [3]=heights [4]=verts [5]=uv [6]=slots
                          [7]=stages [8]=out_handles
     6 invoke_api:       [0]=key [1]=key_len [2]=request [3]=request_len
                          [4]=out_len addr; RET = reply address           */
#define JCM_SVC_ACQUIRE_MODEL       0
#define JCM_SVC_RELEASE_MODEL       1
#define JCM_SVC_RASTERIZE_TEXT      2
#define JCM_SVC_MEASURE_FONT_TEXT   3
#define JCM_SVC_DRAW_FONT_TEXT      4
#define JCM_SVC_CREATE_RESOURCES    5
#define JCM_SVC_INVOKE_API          6

/* Callback queue (Java publishes, the poller drains): a small RING of
   slots so a NESTED Java->C++ re-entry (proxy inside a service handler
   whose module fn calls another Java api with another proxy...) can
   publish while an earlier slot is still in flight. Publishing is
   serialized by a re-entrant Java-side monitor (same-thread nesting
   takes a fresh slot; other threads queue), and the poller drains every
   ring slot in both of its wait loops (outer scan and SVC spin) so no
   reentrancy combination can deadlock. Each slot is 128 bytes: */
#define JCM_CBQ_SLOT_BYTES    128
#define JCM_CBQ_SLOTS         8
/* per-slot fields: */
#define JCM_SHM_CBQ_SEQ       0   /* u64: Java bumps to publish            */
#define JCM_SHM_CBQ_RSP       8   /* u64: poller writes the served seq    */
#define JCM_SHM_CBQ_FN        16  /* u64: module C function pointer        */
#define JCM_SHM_CBQ_USER      24  /* u64: module user payload              */
#define JCM_SHM_CBQ_REQ_ADDR  32  /* u64: request bytes (Java scratch)     */
#define JCM_SHM_CBQ_REQ_LEN   40  /* u64                                    */
#define JCM_SHM_CBQ_RPLY_ADDR 48  /* u64: reply scratch (Java owned)       */
#define JCM_SHM_CBQ_RPLY_CAP  56  /* u64                                    */
#define JCM_SHM_CBQ_RPLY_LEN  64  /* u64: written by the poller            */

/* Handshake (poller discovery): "jcm_shm_hs_<pid>", three lines —
   region file path, region size, region epoch. Written by Java into
   every directory of {java.io.tmpdir, TMPDIR, /tmp, /var/tmp} on POSIX
   (Temp/%TEMP%/%TMP% on Windows); the poller scans {TMPDIR, /tmp,
   /var/tmp} / {GetTempPath(), %TEMP%, %TMP%}. The overlap is total on
   every supported platform; a user-overridden java.io.tmpdir still
   matches through the TMPDIR/%TEMP% copy. */
#define JCM_SHM_HS_NAME_MAX   48

/* Status codes shared with NativeChannel.java. 0 = success. */
#define JCM_SHM_OK              0
#define JCM_SHM_ERR_NOT_INIT    1   /* region not attached/initialized     */
#define JCM_SHM_ERR_BAD_LAYOUT  2   /* magic/version/epoch mismatch        */
#define JCM_SHM_ERR_MODULE      3   /* module handle not registered        */
#define JCM_SHM_ERR_INSTANCE    4   /* instance id never registered        */
#define JCM_SHM_ERR_CREATE      5   /* mtrCreate failed                     */
#define JCM_SHM_ERR_RENDER      6   /* mtrRender failed                      */
#define JCM_SHM_ERR_RESOURCES   7   /* host createResources failed          */
#define JCM_SHM_ERR_REQUEST     8   /* bad seq/magic/snapshot/request       */
#define JCM_SHM_ERR_SNAPSHOT    9   /* null/empty snapshot address          */
#define JCM_SHM_ERR_OVERFLOW    10  /* reply window too small               */
#define JCM_SHM_ERR_DETACHED    11  /* poller lost the region (epoch)       */

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* MTR_SHM_CHANNEL_H */
