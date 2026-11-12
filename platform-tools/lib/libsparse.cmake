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

add_library(libsparse STATIC
    ${SRC}/core/libsparse/backed_block.cpp
    ${SRC}/core/libsparse/output_file.cpp
    ${SRC}/core/libsparse/sparse.cpp
    ${SRC}/core/libsparse/sparse_crc32.cpp
    ${SRC}/core/libsparse/sparse_err.cpp
    ${SRC}/core/libsparse/sparse_read.cpp
    )

target_include_directories(libsparse PRIVATE
    ${SRC}/core/libsparse/include 
    ${SRC}/libbase/include
    )
