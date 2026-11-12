#
# Copyright © 2022 Github Lzhiyong
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Toolchain independent platform settings.
#
#   SDK_STATIC  link fully static executables (default ON)
#   SDK_STRIP   strip symbols of the executables at link time (default ON)
#
# Every executable links `sdk_system_libs`, which carries the system / runtime
# libraries (zlib, threads, dl, m) for the current toolchain, instead of
# hard coding NDK specific names such as `c++_static`.

option(SDK_STATIC "Link fully static executables" ON)
option(SDK_STRIP "Strip the executables at link time" ON)

if(ANDROID)
    set(SDK_TARGET_ANDROID TRUE)
    set(SDK_TARGET_OS android)
else()
    set(SDK_TARGET_ANDROID FALSE)
    string(TOLOWER "${CMAKE_SYSTEM_NAME}" SDK_TARGET_OS)
endif()

# global flags for C/C++ (not for assembler sources)
add_compile_options(
    "$<$<COMPILE_LANGUAGE:C,CXX>:-fPIC>"
    "$<$<COMPILE_LANGUAGE:C,CXX>:-Wno-attributes>"
    "$<$<COMPILE_LANGUAGE:C,CXX>:-fdiagnostics-color=always>"
    )

if(SDK_STATIC)
    # prefer static archives for every find_library()
    set(CMAKE_FIND_LIBRARY_SUFFIXES .a)
    string(APPEND CMAKE_EXE_LINKER_FLAGS " -static")
    if(SDK_TARGET_ANDROID AND ANDROID_STL AND NOT ANDROID_STL STREQUAL "c++_static")
        message(FATAL_ERROR "static build requires ANDROID_STL=c++_static, current: ${ANDROID_STL}")
    endif()
endif()

if(SDK_STRIP)
    string(APPEND CMAKE_EXE_LINKER_FLAGS_RELEASE " -s")
    string(APPEND CMAKE_EXE_LINKER_FLAGS_MINSIZEREL " -s")
endif()

if(NOT SDK_TARGET_ANDROID)
    # libpng (and others) call find_library(M_LIBRARY m), which fails when
    # cross compiling without sysroot (zig), libm is always provided by libc
    set(M_LIBRARY m CACHE STRING "math library")
endif()

set(THREADS_PREFER_PTHREAD_FLAG ON)
find_package(Threads REQUIRED)

add_library(sdk_system_libs INTERFACE)
target_link_libraries(sdk_system_libs INTERFACE
    sdk_zlib
    Threads::Threads
    ${CMAKE_DL_LIBS}
    )
if(NOT SDK_TARGET_ANDROID)
    target_link_libraries(sdk_system_libs INTERFACE m)
endif()
