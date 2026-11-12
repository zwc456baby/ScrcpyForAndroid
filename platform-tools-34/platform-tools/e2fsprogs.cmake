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
    ${SRC}/e2fsprogs/lib
    ${SRC}/core/libsparse/include
    ${SRC}/e2fsprogs/lib
    ${SRC}/e2fsprogs/misc
    ${SRC}/e2fsprogs/lib/ext2fs
    ${SRC}/selinux/libselinux/include
    ${SRC}/core/libcutils/include
    )

# libext2_blkid
add_library(libext2_blkid STATIC
    ${SRC}/e2fsprogs/lib/blkid/cache.c
    ${SRC}/e2fsprogs/lib/blkid/dev.c
    ${SRC}/e2fsprogs/lib/blkid/devname.c
    ${SRC}/e2fsprogs/lib/blkid/devno.c
    ${SRC}/e2fsprogs/lib/blkid/getsize.c
    ${SRC}/e2fsprogs/lib/blkid/llseek.c
    ${SRC}/e2fsprogs/lib/blkid/probe.c
    ${SRC}/e2fsprogs/lib/blkid/read.c
    ${SRC}/e2fsprogs/lib/blkid/resolve.c
    ${SRC}/e2fsprogs/lib/blkid/save.c
    ${SRC}/e2fsprogs/lib/blkid/tag.c
    ${SRC}/e2fsprogs/lib/blkid/version.c
    )
target_include_directories(libext2_blkid PRIVATE ${INCLUDES})

# libext2_com_err
add_library(libext2_com_err STATIC
    ${SRC}/e2fsprogs/lib/et/error_message.c
    ${SRC}/e2fsprogs/lib/et/et_name.c
    ${SRC}/e2fsprogs/lib/et/init_et.c
    ${SRC}/e2fsprogs/lib/et/com_err.c
    ${SRC}/e2fsprogs/lib/et/com_right.c
    )
target_include_directories(libext2_com_err PRIVATE ${INCLUDES})

# libext2_e2p
add_library(libext2_e2p STATIC
    ${SRC}/e2fsprogs/lib/e2p/encoding.c
    ${SRC}/e2fsprogs/lib/e2p/feature.c
    ${SRC}/e2fsprogs/lib/e2p/fgetflags.c
    ${SRC}/e2fsprogs/lib/e2p/fsetflags.c
    ${SRC}/e2fsprogs/lib/e2p/fgetproject.c
    ${SRC}/e2fsprogs/lib/e2p/fsetproject.c
    ${SRC}/e2fsprogs/lib/e2p/fgetversion.c
    ${SRC}/e2fsprogs/lib/e2p/fsetversion.c
    ${SRC}/e2fsprogs/lib/e2p/getflags.c
    ${SRC}/e2fsprogs/lib/e2p/getversion.c
    ${SRC}/e2fsprogs/lib/e2p/hashstr.c
    ${SRC}/e2fsprogs/lib/e2p/iod.c
    ${SRC}/e2fsprogs/lib/e2p/ljs.c
    ${SRC}/e2fsprogs/lib/e2p/ls.c
    ${SRC}/e2fsprogs/lib/e2p/mntopts.c
    ${SRC}/e2fsprogs/lib/e2p/parse_num.c
    ${SRC}/e2fsprogs/lib/e2p/pe.c
    ${SRC}/e2fsprogs/lib/e2p/pf.c
    ${SRC}/e2fsprogs/lib/e2p/ps.c
    ${SRC}/e2fsprogs/lib/e2p/setflags.c
    ${SRC}/e2fsprogs/lib/e2p/setversion.c
    ${SRC}/e2fsprogs/lib/e2p/uuid.c
    ${SRC}/e2fsprogs/lib/e2p/ostype.c
    ${SRC}/e2fsprogs/lib/e2p/percent.c
    )
target_include_directories(libext2_e2p PRIVATE ${INCLUDES})

# libext2_quota
add_library(libext2_quota STATIC
    ${SRC}/e2fsprogs/lib/support/devname.c
    ${SRC}/e2fsprogs/lib/support/dict.c
    ${SRC}/e2fsprogs/lib/support/mkquota.c
    ${SRC}/e2fsprogs/lib/support/parse_qtype.c
    ${SRC}/e2fsprogs/lib/support/plausible.c
    ${SRC}/e2fsprogs/lib/support/profile.c
    ${SRC}/e2fsprogs/lib/support/profile_helpers.c
    ${SRC}/e2fsprogs/lib/support/prof_err.c
    ${SRC}/e2fsprogs/lib/support/quotaio.c
    ${SRC}/e2fsprogs/lib/support/quotaio_tree.c
    ${SRC}/e2fsprogs/lib/support/quotaio_v2.c
    )
target_include_directories(libext2_quota PRIVATE ${INCLUDES})

# libext2_uuid
add_library(libext2_uuid STATIC
    ${SRC}/e2fsprogs/lib/uuid/clear.c
    ${SRC}/e2fsprogs/lib/uuid/compare.c
    ${SRC}/e2fsprogs/lib/uuid/copy.c
    ${SRC}/e2fsprogs/lib/uuid/gen_uuid.c
    ${SRC}/e2fsprogs/lib/uuid/isnull.c
    ${SRC}/e2fsprogs/lib/uuid/pack.c
    ${SRC}/e2fsprogs/lib/uuid/parse.c
    ${SRC}/e2fsprogs/lib/uuid/unpack.c
    ${SRC}/e2fsprogs/lib/uuid/unparse.c
    ${SRC}/e2fsprogs/lib/uuid/uuid_time.c
    )
target_include_directories(libext2_uuid PRIVATE ${INCLUDES})

# libext2fs
add_library(libext2fs STATIC
    ${SRC}/e2fsprogs/lib/ext2fs/ext2_err.c
    ${SRC}/e2fsprogs/lib/ext2fs/alloc.c
    ${SRC}/e2fsprogs/lib/ext2fs/alloc_sb.c
    ${SRC}/e2fsprogs/lib/ext2fs/alloc_stats.c
    ${SRC}/e2fsprogs/lib/ext2fs/alloc_tables.c
    ${SRC}/e2fsprogs/lib/ext2fs/atexit.c
    ${SRC}/e2fsprogs/lib/ext2fs/badblocks.c
    ${SRC}/e2fsprogs/lib/ext2fs/bb_inode.c
    ${SRC}/e2fsprogs/lib/ext2fs/bitmaps.c
    ${SRC}/e2fsprogs/lib/ext2fs/bitops.c
    ${SRC}/e2fsprogs/lib/ext2fs/blkmap64_ba.c
    ${SRC}/e2fsprogs/lib/ext2fs/blkmap64_rb.c
    ${SRC}/e2fsprogs/lib/ext2fs/blknum.c
    ${SRC}/e2fsprogs/lib/ext2fs/block.c
    ${SRC}/e2fsprogs/lib/ext2fs/bmap.c
    ${SRC}/e2fsprogs/lib/ext2fs/check_desc.c
    ${SRC}/e2fsprogs/lib/ext2fs/crc16.c
    ${SRC}/e2fsprogs/lib/ext2fs/crc32c.c
    ${SRC}/e2fsprogs/lib/ext2fs/csum.c
    ${SRC}/e2fsprogs/lib/ext2fs/closefs.c
    ${SRC}/e2fsprogs/lib/ext2fs/dblist.c
    ${SRC}/e2fsprogs/lib/ext2fs/dblist_dir.c
    ${SRC}/e2fsprogs/lib/ext2fs/digest_encode.c
    ${SRC}/e2fsprogs/lib/ext2fs/dirblock.c
    ${SRC}/e2fsprogs/lib/ext2fs/dirhash.c
    ${SRC}/e2fsprogs/lib/ext2fs/dir_iterate.c
    ${SRC}/e2fsprogs/lib/ext2fs/dupfs.c
    ${SRC}/e2fsprogs/lib/ext2fs/expanddir.c
    ${SRC}/e2fsprogs/lib/ext2fs/ext_attr.c
    ${SRC}/e2fsprogs/lib/ext2fs/extent.c
    ${SRC}/e2fsprogs/lib/ext2fs/fallocate.c
    ${SRC}/e2fsprogs/lib/ext2fs/fileio.c
    ${SRC}/e2fsprogs/lib/ext2fs/finddev.c
    ${SRC}/e2fsprogs/lib/ext2fs/flushb.c
    ${SRC}/e2fsprogs/lib/ext2fs/freefs.c
    ${SRC}/e2fsprogs/lib/ext2fs/gen_bitmap.c
    ${SRC}/e2fsprogs/lib/ext2fs/gen_bitmap64.c
    ${SRC}/e2fsprogs/lib/ext2fs/get_num_dirs.c
    ${SRC}/e2fsprogs/lib/ext2fs/get_pathname.c
    ${SRC}/e2fsprogs/lib/ext2fs/getsize.c
    ${SRC}/e2fsprogs/lib/ext2fs/getsectsize.c
    ${SRC}/e2fsprogs/lib/ext2fs/hashmap.c
    ${SRC}/e2fsprogs/lib/ext2fs/i_block.c
    ${SRC}/e2fsprogs/lib/ext2fs/icount.c
    ${SRC}/e2fsprogs/lib/ext2fs/imager.c
    ${SRC}/e2fsprogs/lib/ext2fs/ind_block.c
    ${SRC}/e2fsprogs/lib/ext2fs/initialize.c
    ${SRC}/e2fsprogs/lib/ext2fs/inline.c
    ${SRC}/e2fsprogs/lib/ext2fs/inline_data.c
    ${SRC}/e2fsprogs/lib/ext2fs/inode.c
    ${SRC}/e2fsprogs/lib/ext2fs/io_manager.c
    ${SRC}/e2fsprogs/lib/ext2fs/ismounted.c
    ${SRC}/e2fsprogs/lib/ext2fs/link.c
    ${SRC}/e2fsprogs/lib/ext2fs/llseek.c
    ${SRC}/e2fsprogs/lib/ext2fs/lookup.c
    ${SRC}/e2fsprogs/lib/ext2fs/mmp.c
    ${SRC}/e2fsprogs/lib/ext2fs/mkdir.c
    ${SRC}/e2fsprogs/lib/ext2fs/mkjournal.c
    ${SRC}/e2fsprogs/lib/ext2fs/namei.c
    ${SRC}/e2fsprogs/lib/ext2fs/native.c
    ${SRC}/e2fsprogs/lib/ext2fs/newdir.c
    ${SRC}/e2fsprogs/lib/ext2fs/nls_utf8.c
    ${SRC}/e2fsprogs/lib/ext2fs/openfs.c
    ${SRC}/e2fsprogs/lib/ext2fs/progress.c
    ${SRC}/e2fsprogs/lib/ext2fs/punch.c
    ${SRC}/e2fsprogs/lib/ext2fs/qcow2.c
    ${SRC}/e2fsprogs/lib/ext2fs/rbtree.c
    ${SRC}/e2fsprogs/lib/ext2fs/read_bb.c
    ${SRC}/e2fsprogs/lib/ext2fs/read_bb_file.c
    ${SRC}/e2fsprogs/lib/ext2fs/res_gdt.c
    ${SRC}/e2fsprogs/lib/ext2fs/rw_bitmaps.c
    ${SRC}/e2fsprogs/lib/ext2fs/sha256.c
    ${SRC}/e2fsprogs/lib/ext2fs/sha512.c
    ${SRC}/e2fsprogs/lib/ext2fs/swapfs.c
    ${SRC}/e2fsprogs/lib/ext2fs/symlink.c
    ${SRC}/e2fsprogs/lib/ext2fs/undo_io.c
    ${SRC}/e2fsprogs/lib/ext2fs/unix_io.c
    ${SRC}/e2fsprogs/lib/ext2fs/sparse_io.c
    ${SRC}/e2fsprogs/lib/ext2fs/unlink.c
    ${SRC}/e2fsprogs/lib/ext2fs/valid_blk.c
    ${SRC}/e2fsprogs/lib/ext2fs/version.c
    ${SRC}/e2fsprogs/lib/ext2fs/test_io.c
    )
target_include_directories(libext2fs PRIVATE ${INCLUDES})

# libext2_misc
add_library(libext2_misc STATIC 
    ${SRC}/e2fsprogs/misc/create_inode.c
    )
target_include_directories(libext2_misc PRIVATE ${INCLUDES})

# mke2fs
add_executable(mke2fs
    ${SRC}/e2fsprogs/misc/mke2fs.c
    ${SRC}/e2fsprogs/misc/util.c
    ${SRC}/e2fsprogs/misc/mk_hugefiles.c
    ${SRC}/e2fsprogs/misc/default_profile.c
    )
target_include_directories(mke2fs PRIVATE ${INCLUDES})
target_link_libraries(mke2fs
    libext2_misc
    libext2_blkid
    libext2fs
    libext2_uuid
    libext2_e2p
    libext2_com_err
    libext2_quota
    libsparse
    libbase
    sdk_system_libs
    )

# e2fsdroid
add_executable(e2fsdroid
    ${SRC}/e2fsprogs/contrib/android/e2fsdroid.c
    ${SRC}/e2fsprogs/contrib/android/block_range.c
    ${SRC}/e2fsprogs/contrib/android/fsmap.c
    ${SRC}/e2fsprogs/contrib/android/block_list.c
    ${SRC}/e2fsprogs/contrib/android/base_fs.c
    ${SRC}/e2fsprogs/contrib/android/perms.c
    ${SRC}/e2fsprogs/contrib/android/basefs_allocator.c
    )
target_include_directories(e2fsdroid PRIVATE ${INCLUDES})
target_link_libraries(e2fsdroid
    libext2_misc
    libext2_com_err
    libext2fs
    libselinux
    libsepol
    libpackagelistparser
    libsparse
    libcutils
    libbase
    liblog
    pcre2-8
    sdk_system_libs
    )