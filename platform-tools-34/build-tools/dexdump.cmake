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

set(INCLUDES
    ${SRC}/art/libartbase
    ${SRC}/art/libdexfile
    ${SRC}/art/libartpalette/include
    ${SRC}/tinyxml2
    ${SRC}/fmtlib/include    
    ${SRC}/logging/liblog/include
    ${SRC}/libbase/include
    ${SRC}/libziparchive/include
    )

add_library(libartbase STATIC
    ${SRC}/art/libartbase/arch/instruction_set.cc
    ${SRC}/art/libartbase/base/allocator.cc
    ${SRC}/art/libartbase/base/arena_allocator.cc
    ${SRC}/art/libartbase/base/arena_bit_vector.cc
    ${SRC}/art/libartbase/base/bit_vector.cc
    ${SRC}/art/libartbase/base/compiler_filter.cc
    ${SRC}/art/libartbase/base/file_magic.cc
    ${SRC}/art/libartbase/base/file_utils.cc
    ${SRC}/art/libartbase/base/flags.cc
    ${SRC}/art/libartbase/base/hex_dump.cc
    ${SRC}/art/libartbase/base/logging.cc
    ${SRC}/art/libartbase/base/malloc_arena_pool.cc
    ${SRC}/art/libartbase/base/membarrier.cc
    ${SRC}/art/libartbase/base/memfd.cc
    ${SRC}/art/libartbase/base/memory_region.cc
    ${SRC}/art/libartbase/base/mem_map.cc
    ${SRC}/art/libartbase/base/metrics/metrics_common.cc
    ${SRC}/art/libartbase/base/os_linux.cc
    ${SRC}/art/libartbase/base/pointer_size.cc
    ${SRC}/art/libartbase/base/runtime_debug.cc
    ${SRC}/art/libartbase/base/scoped_arena_allocator.cc
    ${SRC}/art/libartbase/base/scoped_flock.cc
    ${SRC}/art/libartbase/base/socket_peer_is_trusted.cc
    ${SRC}/art/libartbase/base/time_utils.cc
    ${SRC}/art/libartbase/base/unix_file/fd_file.cc
    ${SRC}/art/libartbase/base/unix_file/random_access_file_utils.cc
    ${SRC}/art/libartbase/base/utils.cc
    ${SRC}/art/libartbase/base/zip_archive.cc
    ${SRC}/art/libartbase/base/globals_unix.cc
    ${SRC}/art/libartbase/base/mem_map_unix.cc
    )
target_include_directories(libartbase PRIVATE ${INCLUDES})

add_library(libartpalette STATIC
    ${SRC}/art/libartpalette/apex/palette.cc
    ${SRC}/art/libartpalette/system/palette_fake.cc
    )
target_include_directories(libartpalette PRIVATE ${INCLUDES})

add_library(libdexfile STATIC
    ${SRC}/art/libdexfile/dex/art_dex_file_loader.cc
    ${SRC}/art/libdexfile/dex/compact_dex_file.cc
    ${SRC}/art/libdexfile/dex/compact_offset_table.cc
    ${SRC}/art/libdexfile/dex/descriptors_names.cc
    ${SRC}/art/libdexfile/dex/dex_file.cc
    ${SRC}/art/libdexfile/dex/dex_file_exception_helpers.cc
    ${SRC}/art/libdexfile/dex/dex_file_layout.cc
    ${SRC}/art/libdexfile/dex/dex_file_loader.cc
    ${SRC}/art/libdexfile/dex/dex_file_tracking_registrar.cc
    ${SRC}/art/libdexfile/dex/dex_file_verifier.cc
    ${SRC}/art/libdexfile/dex/dex_instruction.cc
    ${SRC}/art/libdexfile/dex/modifiers.cc
    ${SRC}/art/libdexfile/dex/primitive.cc
    ${SRC}/art/libdexfile/dex/signature.cc
    ${SRC}/art/libdexfile/dex/standard_dex_file.cc
    ${SRC}/art/libdexfile/dex/type_lookup_table.cc
    ${SRC}/art/libdexfile/dex/utf.cc
    )
target_include_directories(libdexfile PRIVATE ${INCLUDES})

add_executable(dexdump
    ${SRC}/art/dexdump/dexdump_cfg.cc
    ${SRC}/art/dexdump/dexdump_main.cc
    ${SRC}/art/dexdump/dexdump.cc
    )
target_include_directories(dexdump PRIVATE ${INCLUDES})
target_link_libraries(dexdump
    libdexfile
    libartbase
    libartpalette
    libbase
    libziparchive
    liblog
    sdk_system_libs
    )
    