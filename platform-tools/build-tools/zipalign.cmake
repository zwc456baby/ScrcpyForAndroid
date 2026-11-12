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

add_executable(zipalign
    ${SRC}/build/tools/zipalign/ZipAlign.cpp
    ${SRC}/build/tools/zipalign/ZipEntry.cpp
    ${SRC}/build/tools/zipalign/ZipFile.cpp
    ${SRC}/build/tools/zipalign/ZipAlignMain.cpp
    )

target_include_directories(zipalign PRIVATE
    ${SRC}/build/tools/zipalign/include
    ${SRC}/core/libutils/include
    ${SRC}/logging/liblog/include
    ${SRC}/zopfli/src
    ${SRC}/libbase/include
    ${SRC}/libziparchive/include
    )

target_link_libraries(zipalign
    libutils
    libbase
    libziparchive
    libzopfli
    liblog
    sdk_system_libs
    )
