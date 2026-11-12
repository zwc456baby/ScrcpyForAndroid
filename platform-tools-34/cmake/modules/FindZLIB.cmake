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
# Redirect find_package(ZLIB) of the third-party projects (libpng, protobuf ...)
# to the bundled zlib (see cmake/zlib.cmake), so that every toolchain links
# the same static zlib. Falls back to the stock module otherwise.

if(TARGET sdk_zlib)
    set(ZLIB_FOUND TRUE)
    set(ZLIB_INCLUDE_DIR ${SDK_ZLIB_INCLUDE_DIR})
    set(ZLIB_INCLUDE_DIRS ${SDK_ZLIB_INCLUDE_DIR})
    set(ZLIB_LIBRARY ZLIB::ZLIB)
    set(ZLIB_LIBRARIES ZLIB::ZLIB)
    set(ZLIB_VERSION ${SDK_ZLIB_VERSION})
    set(ZLIB_VERSION_STRING ${SDK_ZLIB_VERSION})
    if(NOT ZLIB_FIND_QUIETLY)
        message(STATUS "Found ZLIB: bundled sdk_zlib (version ${SDK_ZLIB_VERSION})")
    endif()
else()
    include(${CMAKE_ROOT}/Modules/FindZLIB.cmake)
endif()
