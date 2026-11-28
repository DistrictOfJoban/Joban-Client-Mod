# JCM native script scaffold — copy this folder, run cmake with your SDK,
# and you have a buildable C++ script module in one command.
#
#   cp -r <sdk>/template my_crossing_script
#   cd my_crossing_script
#   cmake -B build -DJCM_SDK=<path-to-unzipped-sdk> -DCMAKE_BUILD_TYPE=Release
#   cmake --build build
#
# The SDK zip ships include/ (the mtr/ headers) and prebuilt example
# modules; JCM_SDK must point at its extracted root. CMAKE_OSX_* /
# toolchain files work as usual for cross builds — see the SDK README
# for the per-platform output naming conventions JCM expects
# (natives/<platform>/lib<stem>.so / <stem>.dll / lib<stem>.dylib).
#
# This template builds THREE flavours of the one source file so you can
# see the registration macros in context; delete the ones you don't
# need (and their sources) for a real project.

cmake_minimum_required(VERSION 3.16)
project(jcm_native_scaffold CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# The SDK root: include/ plus this template's build script expect it.
set(JCM_SDK "" CACHE PATH "Path to the extracted jcm-native-sdk zip")
if(NOT JCM_SDK)
    message(FATAL_ERROR
        "Point JCM_SDK at the extracted SDK directory, e.g.:\n"
        "  cmake -B build -DJCM_SDK=/path/to/jcm-native-sdk-x.y.z")
endif()
if(NOT EXISTS "${JCM_SDK}/include/mtr/mtr_native.h")
    message(FATAL_ERROR "JCM_SDK=${JCM_SDK} has no include/mtr/mtr_native.h — wrong directory?")
endif()

# Every script module is a MODULE library (a plugin .so/.dll/.dylib).
function(jcm_add_script target)
    add_library(${target} MODULE ${ARGN})
    target_include_directories(${target} PRIVATE "${JCM_SDK}/include")
    set_target_properties(${target} PROPERTIES
        PREFIX ""                                   # Windows: script.dll, not libscript.dll
        SUFFIX "${CMAKE_SHARED_LIBRARY_SUFFIX}")    # macOS .dylib keeps the lib prefix below
    if(APPLE)
        set_target_properties(${target} PROPERTIES PREFIX "lib"
            LINK_FLAGS "-undefined dynamic_lookup") # symbols resolve from the bridge at load
    endif()
    if(ANDROID)
        set_target_properties(${target} PROPERTIES LINK_FLAGS "-Wl,-z,max-page-size=16384")
    endif()
endfunction()

# The three example flavours — pick the kind you need.
jcm_add_script(scaffold_pids        scaffold_pids.cpp)
jcm_add_script(scaffold_vehicle     scaffold_vehicle.cpp)
jcm_add_script(scaffold_crossing    scaffold_crossing.cpp)

message(STATUS "JCM SDK: ${JCM_SDK}")
message(STATUS "Build the pack layout the loader expects:")
message(STATUS "  mypack/assets/mypack/natives/<platform>/<module file>")
message(STATUS "and declare it in mtr:mtr_custom_resources.json (see the README).")
