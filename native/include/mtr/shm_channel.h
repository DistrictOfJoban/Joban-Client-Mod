/**
 * shm_channel.h — Yanyang "shared memory" transport layout (v1).
 *
 * ONE memory-mapped file, two participants in the SAME process:
 *
 *   - Java maps it with FileChannel#map (a MappedByteBuffer) and writes
 *     per-frame snapshot blobs into bump-allocated "windows" inside the
 *     WINDOWS area (see NativeShm.java on the JVM side);
 *   - C++ reaches the very same pages through the mapping's base address,
 *     which Java hands over exactly once via nativeShmInit().
 *
 * The per-frame exchange is fully SEQUENTIAL on the render thread:
 *
 *     Java: write request  -> control slot
 *     Java: nativeShmKick()          (an empty-argument JNI call: no data,
 *                                     no jstring, no returned object)
 *     C++:  read request  <- control slot
 *     C++:  mtrRender (snapshot pointer points INTO this mapping for
 *           driver-owned buffers, or at any plain direct buffer the
 *           historical fallbacks produce — the address travels per frame)
 *     C++:  write result  -> result slot (arena pointers + lengths)
 *     Java: read result   <- result slot, wrap arenas as NIO ByteBuffers
 *           (reused wrapper objects; see NativeArenaView.java)
 *
 * Because writer and reader are the same thread, no locks or atomics are
 * REQUIRED; the magic/seq/echo fields exist so that accidental misuse
 * (a stray kick from another thread, a stale bridge, jar/bridge skew)
 * degrades into a logged no-op frame instead of reading garbage.
 *
 * JDK note: on the JVM side the control/result fields are accessed via
 * sun.misc.Unsafe volatile operations on Java 16..21 and via
 * Foreign Function & Memory API VarHandles (MemorySegment.ofBuffer) on
 * JDK 22+, selected automatically at runtime.
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

/* Bytes "JCMYSHM1" read as a little-endian u64. */
#define JCM_SHM_MAGIC        UINT64_C(0x314D48534D59434A)
#define JCM_SHM_LAYOUT_VERSION 1

/* Fixed region geometry (bytes). The windows area starts at 16 KiB; the
   control/result slots live in the first 16 KiB. */
#define JCM_SHM_CONTROL_OFFSET 8192
#define JCM_SHM_CONTROL_SIZE   128
#define JCM_SHM_RESULT_OFFSET 8320
#define JCM_SHM_RESULT_SIZE   256
#define JCM_SHM_WINDOWS_OFFSET 16384

/* Header field offsets (Java writes these once at region creation). */
#define JCM_SHM_HDR_MAGIC     0   /* u64 = JCM_SHM_MAGIC            */
#define JCM_SHM_HDR_VERSION   8   /* u32 = JCM_SHM_LAYOUT_VERSION   */
#define JCM_SHM_HDR_RESERVED  12  /* u32, zero                      */
#define JCM_SHM_HDR_SIZE      16  /* u64 = total region size        */
#define JCM_SHM_HDR_CONTROL   24  /* u64 = JCM_SHM_CONTROL_OFFSET   */
#define JCM_SHM_HDR_RESULT    32  /* u64 = JCM_SHM_RESULT_OFFSET    */
#define JCM_SHM_HDR_WINDOWS   40  /* u64 = JCM_SHM_WINDOWS_OFFSET   */

/* Control slot (Java writes, C++ reads inside nativeShmKick). */
#define JCM_SHM_CTL_SEQ       0   /* u64: bumped by Java every kick */
#define JCM_SHM_CTL_MODULE    8   /* u64: BridgeModule handle       */
#define JCM_SHM_CTL_INSTANCE  16  /* u64: per-module instance id    */
#define JCM_SHM_CTL_KIND      24  /* u32: MtrResourceKind           */
#define JCM_SHM_CTL_SNAP_LEN  28  /* u32: written snapshot bytes    */
#define JCM_SHM_CTL_SNAP_ADDR 32  /* u64: snapshot address          */

/* Result slot (C++ writes before nativeShmKick returns). */
#define JCM_SHM_RES_SEQ       0   /* u64: echo of the request seq   */
#define JCM_SHM_RES_STATUS    8   /* u32: JCM_SHM_* status          */
#define JCM_SHM_RES_COUNT     12  /* u32: record count              */
#define JCM_SHM_RES_RECORDS   16  /* u64 addr, u64 len, u64 cap     */
#define JCM_SHM_RES_STRINGS   40
#define JCM_SHM_RES_PIXELS    64
#define JCM_SHM_RES_MATRICES  88
#define JCM_SHM_RES_FLOATS     112
/* each arena entry: +0 address, +8 length (this frame), +16 capacity. */

/* Status codes shared with NativeChannel.java. 0 = success. */
#define JCM_SHM_OK              0
#define JCM_SHM_ERR_NOT_INIT    1   /* nativeShmInit not called/failed */
#define JCM_SHM_ERR_BAD_LAYOUT  2   /* magic/version mismatch          */
#define JCM_SHM_ERR_MODULE      3   /* module handle not registered    */
#define JCM_SHM_ERR_INSTANCE    4   /* instance id never registered    */
#define JCM_SHM_ERR_CREATE      5   /* mtrCreate failed                 */
#define JCM_SHM_ERR_RENDER      6   /* mtrRender failed                 */
#define JCM_SHM_ERR_RESOURCES   7   /* host createResources failed     */
#define JCM_SHM_ERR_REQUEST     8   /* bad seq/magic/snapshot          */
#define JCM_SHM_ERR_SNAPSHOT    9   /* null/empty snapshot address     */

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* MTR_SHM_CHANNEL_H */
