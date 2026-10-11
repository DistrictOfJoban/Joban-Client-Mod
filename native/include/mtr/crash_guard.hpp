/**
 * crash_guard.hpp — turn a hard crash inside a native script into a logged,
 * recoverable failure.
 *
 * Why: the field crash we are chasing surfaced as an ACCESS_VIOLATION inside
 * VCRUNTIME140.dll's memcpy with the calling frame already unwound, which made
 * it impossible to attribute from the hs_err log alone — and it took the whole
 * game down. A script module is optional content: if one throws a structured
 * exception, the right behaviour is to disable THAT module, log where it
 * happened, and keep the game running with the other modules intact.
 *
 * Windows: __try/__except around the entry point (SEH catches access
 * violations; C++ try/catch does not).
 * Other toolchains: a plain C++ try/catch, which at least handles C++
 * exceptions escaping the module (std::bad_alloc from a giant texture, etc).
 * POSIX signals are deliberately not trapped — longjmp-ing out of a SIGSEGV
 * is not something this SDK should attempt.
 */
#pragma once
#include <cstdio>
#include <exception>

#if defined(_MSC_VER)
   /* NOMINMAX is mandatory: this header is pulled in by script.hpp, which is
      included by the drawing code, and windows.h's min/max macros turn every
      std::min/std::max in gfx2d.hpp into "illegal token on right side of '::'".
      WIN32_LEAN_AND_MEAN keeps the compiler time down. */
#  ifndef NOMINMAX
#    define NOMINMAX
#  endif
#  ifndef WIN32_LEAN_AND_MEAN
#    define WIN32_LEAN_AND_MEAN
#  endif
#  include <windows.h>
#  include <cstdio>
#endif

namespace mtr {

/* Set to true by guard_entry() when the guarded call faulted, so the caller
   can mark the module dead and stop calling into it. */
inline bool& module_faulted() {
    static bool faulted = false;
    return faulted;
}

template <typename F> inline int guard_cpp(F&& function) noexcept {
    try { return function(); }
    catch (const std::exception& error) {
        std::fprintf(stderr, "[mtr] native script exception: %s\n", error.what());
    } catch (...) {
        std::fprintf(stderr, "[mtr] unknown native script exception\n");
    }
    module_faulted() = true;
    return -99;
}

#if defined(_MSC_VER)
#  define MTR_GUARDED_BODY(body)                                              \
    __try {                                                                   \
        return mtr::guard_cpp([&]() -> int { body; });                          \
    } __except (mtr::report_seh(GetExceptionInformation(), __FILE__, __LINE__)) { \
        return -99;                                                           \
    }
#else
#  define MTR_GUARDED_BODY(body)                                              \
    return mtr::guard_cpp([&]() -> int { body; })
#endif

#if defined(_MSC_VER)
/* Log the faulting address + exception code, then swallow it (EXCEPTION_EXECUTE_HANDLER).
   Returning 1 means "handle it". */
inline int report_seh(EXCEPTION_POINTERS* info, const char* file, int line) {
    const DWORD code = info && info->ExceptionRecord
                     ? info->ExceptionRecord->ExceptionCode : 0;
    const void* addr = info && info->ExceptionRecord
                     ? info->ExceptionRecord->ExceptionAddress : nullptr;
    std::fprintf(stderr,
        "[mtr] native script faulted: code=0x%08lX at %p (%s:%d) — module disabled\n",
        static_cast<unsigned long>(code), addr, file, line);
    std::fflush(stderr);
    module_faulted() = true;
    return 1; /* EXCEPTION_EXECUTE_HANDLER */
}
#endif

} /* namespace mtr */
