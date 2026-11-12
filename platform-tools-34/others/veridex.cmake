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

add_executable(veridex
    ${SRC}/art/tools/veridex/flow_analysis.cc
    ${SRC}/art/tools/veridex/hidden_api.cc
    ${SRC}/art/tools/veridex/hidden_api_finder.cc
    ${SRC}/art/tools/veridex/precise_hidden_api_finder.cc
    ${SRC}/art/tools/veridex/resolver.cc
    ${SRC}/art/tools/veridex/veridex.cc
    )

target_include_directories(veridex PRIVATE
    ${SRC}/art/libdexfile
    ${SRC}/art/libartbase
    ${SRC}/fmtlib/include
    ${SRC}/logging/liblog/include
    ${SRC}/libbase/include
    ${SRC}/libziparchive/include
    )

target_link_libraries(veridex
    libdexfile
    libartbase
    libartpalette
    libbase
    liblog
    libziparchive
    fmt::fmt
    sdk_system_libs
    )
