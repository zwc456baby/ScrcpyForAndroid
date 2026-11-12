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
# CMake toolchain file for cross compiling with `zig cc` / `zig c++`.
#
# Variables (pass with -D, or set as environment variables):
#   ZIG_TARGET      zig target triple, e.g. aarch64-linux-musl (required)
#   ZIG_CPU         optional -mcpu value, e.g. baseline / cortex_a53
#   ZIG_EXECUTABLE  path of the zig executable (default: zig in PATH)
#
# Example:
#   cmake -GNinja -B build \
#       -DCMAKE_TOOLCHAIN_FILE=cmake/toolchains/zig.cmake \
#       -DZIG_TARGET=aarch64-linux-musl
#

foreach(_var ZIG_TARGET ZIG_CPU ZIG_EXECUTABLE)
    if(NOT DEFINED ${_var} AND DEFINED ENV{${_var}})
        set(${_var} "$ENV{${_var}}")
    endif()
endforeach()

if(NOT ZIG_TARGET)
    message(FATAL_ERROR "ZIG_TARGET is not set, e.g. -DZIG_TARGET=aarch64-linux-musl")
endif()

if(NOT ZIG_EXECUTABLE)
    find_program(ZIG_EXECUTABLE NAMES zig REQUIRED)
endif()

# propagate the settings into try_compile() projects
list(APPEND CMAKE_TRY_COMPILE_PLATFORM_VARIABLES ZIG_TARGET ZIG_CPU ZIG_EXECUTABLE)

# target system
string(REGEX MATCH "^[^-]+" _zig_arch "${ZIG_TARGET}")
if(_zig_arch STREQUAL "x86" OR _zig_arch STREQUAL "i386")
    set(_zig_arch i686)
endif()

if(ZIG_TARGET MATCHES "-windows")
    set(CMAKE_SYSTEM_NAME Windows)
elseif(ZIG_TARGET MATCHES "-macos")
    set(CMAKE_SYSTEM_NAME Darwin)
else()
    set(CMAKE_SYSTEM_NAME Linux)
endif()
set(CMAKE_SYSTEM_PROCESSOR ${_zig_arch})

# zig needs extra sub-command arguments (cc / c++ / ar), while CMake wants a
# single executable for every tool, so generate tiny wrapper scripts
set(_zig_flags "-target ${ZIG_TARGET}")
if(ZIG_CPU)
    string(APPEND _zig_flags " -mcpu=${ZIG_CPU}")
endif()
# zig enables UBSan traps in non-release modes, AOSP code is not UBSan clean
string(APPEND _zig_flags " -fno-sanitize=undefined")

set(_zig_wrapper_dir "${CMAKE_BINARY_DIR}/zig-wrappers")
file(MAKE_DIRECTORY "${_zig_wrapper_dir}")

# zig's linker rejects `-Wl,-v` (used by CMake to detect the compiler ABI),
# drop it, otherwise CMAKE_SIZEOF_VOID_P etc. stay undefined
function(_zig_write_wrapper name command)
    set(_file "${_zig_wrapper_dir}/${name}")
    set(_content "#!/bin/sh\n")
    string(APPEND _content "for arg in \"$@\"; do\n")
    string(APPEND _content "    shift\n")
    string(APPEND _content "    [ \"$arg\" = \"-Wl,-v\" ] || set -- \"$@\" \"$arg\"\n")
    string(APPEND _content "done\n")
    string(APPEND _content "exec \"${ZIG_EXECUTABLE}\" ${command} \"$@\"\n")
    if(EXISTS "${_file}")
        file(READ "${_file}" _old)
    endif()
    if(NOT "${_old}" STREQUAL "${_content}")
        file(WRITE "${_file}" "${_content}")
        file(CHMOD "${_file}" PERMISSIONS
            OWNER_READ OWNER_WRITE OWNER_EXECUTE
            GROUP_READ GROUP_EXECUTE WORLD_READ WORLD_EXECUTE)
    endif()
endfunction()

_zig_write_wrapper(zig-cc     "cc ${_zig_flags}")
_zig_write_wrapper(zig-c++    "c++ ${_zig_flags}")
_zig_write_wrapper(zig-ar     "ar")
_zig_write_wrapper(zig-ranlib "ranlib")

set(CMAKE_C_COMPILER   "${_zig_wrapper_dir}/zig-cc")
set(CMAKE_CXX_COMPILER "${_zig_wrapper_dir}/zig-c++")
set(CMAKE_ASM_COMPILER "${_zig_wrapper_dir}/zig-cc")
set(CMAKE_AR           "${_zig_wrapper_dir}/zig-ar" CACHE FILEPATH "zig ar" FORCE)
set(CMAKE_RANLIB       "${_zig_wrapper_dir}/zig-ranlib" CACHE FILEPATH "zig ranlib" FORCE)

# never pick up libraries or headers of the build machine
set(CMAKE_FIND_ROOT_PATH "${CMAKE_BINARY_DIR}/zig-sysroot")
set(CMAKE_FIND_ROOT_PATH_MODE_PROGRAM NEVER)
set(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_INCLUDE ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_PACKAGE ONLY)
