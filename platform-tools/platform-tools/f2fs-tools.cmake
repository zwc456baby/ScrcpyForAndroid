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

set(DEFINITIONS
    -DF2FS_MAJOR_VERSION=1
    -DF2FS_MINOR_VERSION=15
    -DF2FS_TOOLS_VERSION="1.15.0"
    -DF2FS_TOOLS_DATE="2022-05-20"
    -DWITH_ANDROID
    )

set(INCLUDES
    ${SRC}/f2fs-tools/include
    ${SRC}/e2fsprogs/lib
    ${SRC}/e2fsprogs/lib/uuid
    ${SRC}/lz4/lib
    ${SRC}/core/libsparse/include
    ${SRC}/core/libcutils/include
    )

add_executable(make_f2fs 
    ${SRC}/f2fs-tools/lib/libf2fs.c
    ${SRC}/f2fs-tools/mkfs/f2fs_format.c
    ${SRC}/f2fs-tools/mkfs/f2fs_format_utils.c
    ${SRC}/f2fs-tools/lib/libf2fs_zoned.c
    ${SRC}/f2fs-tools/lib/nls_utf8.c
    ${SRC}/f2fs-tools/lib/libf2fs_io.c
    ${SRC}/f2fs-tools/mkfs/f2fs_format_main.c
    )
target_compile_definitions(make_f2fs PRIVATE 
    -DWITH_BLKDISCARD
    ${DEFINITIONS}
    )
target_include_directories(make_f2fs PRIVATE ${INCLUDES})
target_link_libraries(make_f2fs
    libext2_uuid
    libbase
    libsparse
    sdk_system_libs
    )
    
add_executable(make_f2fs_casefold 
    ${SRC}/f2fs-tools/lib/libf2fs.c
    ${SRC}/f2fs-tools/mkfs/f2fs_format.c
    ${SRC}/f2fs-tools/mkfs/f2fs_format_utils.c
    ${SRC}/f2fs-tools/lib/libf2fs_zoned.c
    ${SRC}/f2fs-tools/lib/nls_utf8.c
    ${SRC}/f2fs-tools/lib/libf2fs_io.c
    ${SRC}/f2fs-tools/mkfs/f2fs_format_main.c
    )
target_compile_definitions(make_f2fs_casefold PRIVATE 
    -DCONF_CASEFOLD
    -DCONF_PROJID
    ${DEFINITIONS}
    )
target_include_directories(make_f2fs_casefold PRIVATE ${INCLUDES})
target_link_libraries(make_f2fs_casefold
    libext2_uuid
    libbase
    libsparse
    sdk_system_libs
    )

add_executable(sload_f2fs
    ${SRC}/f2fs-tools/fsck/dir.c
    ${SRC}/f2fs-tools/fsck/dict.c
    ${SRC}/f2fs-tools/fsck/mkquota.c
    ${SRC}/f2fs-tools/fsck/quotaio.c
    ${SRC}/f2fs-tools/fsck/quotaio_tree.c
    ${SRC}/f2fs-tools/fsck/quotaio_v2.c
    ${SRC}/f2fs-tools/fsck/node.c
    ${SRC}/f2fs-tools/fsck/segment.c
    ${SRC}/f2fs-tools/fsck/xattr.c
    ${SRC}/f2fs-tools/fsck/main.c
    ${SRC}/f2fs-tools/fsck/mount.c
    ${SRC}/f2fs-tools/lib/libf2fs.c
    ${SRC}/f2fs-tools/lib/libf2fs_io.c
    ${SRC}/f2fs-tools/lib/libf2fs_zoned.c
    ${SRC}/f2fs-tools/lib/nls_utf8.c
    ${SRC}/f2fs-tools/fsck/dump.c
    ${SRC}/f2fs-tools/fsck/fsck.c
    ${SRC}/f2fs-tools/fsck/sload.c
    ${SRC}/f2fs-tools/fsck/compress.c
    )
target_compile_definitions(sload_f2fs PRIVATE 
    -DWITH_SLOAD
    ${DEFINITIONS}
    )
target_include_directories(sload_f2fs PRIVATE ${INCLUDES})
target_link_libraries(sload_f2fs
    libext2_uuid
    libselinux
    libsepol
    libpackagelistparser
    libsparse
    libbase
    libcutils
    liblog
    pcre2-8
    lz4_static
    sdk_system_libs
    )
    