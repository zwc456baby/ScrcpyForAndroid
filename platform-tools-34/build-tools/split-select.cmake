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

add_executable(split-select
    ${SRC}/base/tools/split-select/Abi.cpp
    ${SRC}/base/tools/split-select/Grouper.cpp
    ${SRC}/base/tools/split-select/Rule.cpp
    ${SRC}/base/tools/split-select/RuleGenerator.cpp
    ${SRC}/base/tools/split-select/SplitDescription.cpp
    ${SRC}/base/tools/split-select/SplitSelector.cpp
    ${SRC}/base/tools/split-select/Main.cpp
    )

target_compile_definitions(split-select PRIVATE 
    -D_DARWIN_UNLIMITED_STREAMS
    )
target_include_directories(split-select PRIVATE
    ${SRC}/base/libs/androidfw/include
    ${SRC}/base/libs/androidfw/include_pathutils
    ${SRC}/core/libutils/include
    ${SRC}/logging/liblog/include
    ${SRC}/core/libsystem/include
    ${SRC}/libbase/include
    ${SRC}/fmtlib/include
    ${SRC}/base/tools
    ${SRC}/incremental_delivery/incfs/util/include 
    )
target_link_libraries(split-select
    libaapt
    libandroidfw
    libselinux
    libsepol
    libutils
    libcutils
    libincfs
    libbase
    libziparchive
    libpackagelistparser
    libprocessgroup
    liblog
    expat
    crypto
    pcre2-8
    jsoncpp_static
    png_static
    sdk_system_libs
    )
    