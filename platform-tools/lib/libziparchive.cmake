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

add_library(libziparchive STATIC
    ${SRC}/libziparchive/zip_archive.cc
    ${SRC}/libziparchive/zip_archive_stream_entry.cc
    ${SRC}/libziparchive/zip_cd_entry_map.cc
    ${SRC}/libziparchive/zip_writer.cc
    ${SRC}/libziparchive/zip_error.cpp
    ${SRC}/libziparchive/incfs_support/signal_handling.cpp
    )

target_compile_definitions(libziparchive PRIVATE 
    -DZLIB_CONST
    -D_FILE_OFFSET_BITS=64
    -DZIPARCHIVE_DISABLE_CALLBACK_API=1
    )
    
target_include_directories(libziparchive PRIVATE
    ${SRC}/libziparchive/include 
    ${SRC}/libziparchive/incfs_support/include
    ${SRC}/libbase/include 
    ${SRC}/logging/liblog/include
    ${SRC}/googletest/googletest/include
    )