cmake_minimum_required(VERSION 3.16)
project(jcm-native-scripting LANGUAGES CXX)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
if(MSVC)
  set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")
  add_compile_options(/utf-8 /O2 /EHsc /W3)
else()
  add_compile_options(-O2)
  # Warnings (not errors) for GCC/Clang — MSVC had /W3, the POSIX side had
  # nothing, so JNI-boundary and bounds-check regressions compiled silently.
  add_compile_options(-Wall -Wextra)
  if(CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
    add_compile_options(-fno-gnu-unique)
  endif()
endif()
add_library(mtr_script_sdk INTERFACE)
target_include_directories(mtr_script_sdk INTERFACE ${CMAKE_CURRENT_SOURCE_DIR}/include)
function(mtr_add_script name)
  add_library(${name} MODULE ${ARGN})
  target_link_libraries(${name} PRIVATE mtr_script_sdk)
  set_target_properties(${name} PROPERTIES CXX_VISIBILITY_PRESET hidden)
  if(WIN32)
    set_target_properties(${name} PROPERTIES PREFIX "" SUFFIX ".dll")
  elseif(APPLE)
    set_target_properties(${name} PROPERTIES PREFIX "lib" SUFFIX ".dylib")
    # Modules resolve JNI symbols against the embedding JVM at load time.
    # CMake's Darwin MODULE create flags have historically included
    # -undefined dynamic_lookup, but some CMAKE_SYSTEM_NAME=iOS combos drop
    # it — make it explicit so cross builds link the same way as macOS.
    target_link_options(${name} PRIVATE "-Wl,-undefined,dynamic_lookup")
  elseif(ANDROID)
    # 16 KB page alignment: required in Android 15's 16 KB mode and
    # harmless everywhere else (lld has understood this since NDK r22).
    target_link_options(${name} PRIVATE "-Wl,-z,max-page-size=16384")
  endif()
endfunction()
# Generic SDK examples remain separate from mod resources and jar contents.
mtr_add_script(pids_arrivals examples/pids_arrivals.cpp)
mtr_add_script(eyecandy_signal examples/eyecandy_signal.cpp)
mtr_add_script(lift_indicator examples/lift_indicator.cpp)
find_package(JNI QUIET)
if(ANDROID)
  # UNCONDITIONALLY override whatever FindJNI picked under the NDK
  # toolchain: FindJNI resolves against the NDK sysroot and reports
  # success, but that jni.h is a divergent variant (no JNI_VERSION_1_8,
  # C++ AttachCurrentThread takes JNIEnv** where OpenJDK's takes void** —
  # the form the bridge and every desktop build use). The JNI interface
  # declarations themselves are platform-independent (LP64 widths match),
  # so compile against the HOST JDK headers passed via
  # -DJCM_JNI_INCLUDE_DIR=$JAVA_HOME/include; -I comes before the sysroot,
  # and module JNI symbols still resolve against the embedding JVM at
  # load time — nothing from the JDK is linked into the image.
  if(DEFINED JCM_JNI_INCLUDE_DIR AND EXISTS "${JCM_JNI_INCLUDE_DIR}/jni.h")
    set(JNI_FOUND ON)
    set(JNI_INCLUDE_DIRS "${JCM_JNI_INCLUDE_DIR}" "${JCM_JNI_INCLUDE_DIR}/linux")
  endif()
endif()
if(NOT JNI_FOUND AND CMAKE_SYSTEM_NAME STREQUAL "iOS")
  # Cross-compile for iOS: no JVM on the target, but the JNI surface is a
  # stable interface — compile against the HOST JDK's headers (identical
  # declarations). Pass -DJCM_JNI_INCLUDE_DIR=$JAVA_HOME/include.
  if(DEFINED JCM_JNI_INCLUDE_DIR AND EXISTS "${JCM_JNI_INCLUDE_DIR}/jni.h")
    set(JNI_FOUND ON)
    set(JNI_INCLUDE_DIRS "${JCM_JNI_INCLUDE_DIR}" "${JCM_JNI_INCLUDE_DIR}/darwin")
  endif()
endif()
if(JNI_FOUND)
  mtr_add_script(jcm_native_bridge jni/jni_bridge.cpp)
  target_include_directories(jcm_native_bridge PRIVATE ${JNI_INCLUDE_DIRS})
  target_link_libraries(jcm_native_bridge PRIVATE ${CMAKE_DL_LIBS})
endif()
include(CTest)
if(BUILD_TESTING)
  add_executable(native_regression bench/native_regression.cpp)
  target_link_libraries(native_regression PRIVATE mtr_script_sdk)
  add_executable(native_abi_layout bench/native_abi_layout.cpp)
  target_link_libraries(native_abi_layout PRIVATE mtr_script_sdk)
  add_executable(native_sdk_test bench/native_sdk_test.cpp)
  target_link_libraries(native_sdk_test PRIVATE mtr_script_sdk)
  add_test(NAME native_regression COMMAND native_regression)
  add_test(NAME native_sdk_test COMMAND native_sdk_test)
  mtr_add_script(api_probe bench/api_probe.cpp)
  foreach(kind RANGE 1 3)
    mtr_add_script(content_probe_${kind} bench/content_probe.cpp)
    target_compile_definitions(content_probe_${kind} PRIVATE CONTENT_KIND=${kind})
  endforeach()
  foreach(abi RANGE 6 7)
    mtr_add_script(legacy_probe_${abi} bench/legacy_probe.cpp)
    target_compile_definitions(legacy_probe_${abi} PRIVATE LEGACY_ABI=${abi})
  endforeach()
  find_package(Python3 COMPONENTS Interpreter QUIET)
  if(JNI_FOUND AND Python3_FOUND)
    add_test(NAME native_jvm_integration COMMAND ${Python3_EXECUTABLE} ${CMAKE_CURRENT_SOURCE_DIR}/bench/jvm_test.py --build ${CMAKE_CURRENT_BINARY_DIR})
  endif()
endif()
install(DIRECTORY include/ DESTINATION include)
