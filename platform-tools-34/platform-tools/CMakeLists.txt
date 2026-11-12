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

project(platform-tools)
# executable files output path
set(EXECUTABLE_OUTPUT_PATH ${CMAKE_BINARY_DIR}/bin/${PROJECT_NAME})

# this tool has removed by google
#include(dmtracedump.cmake)

# include(hprof-conv.cmake)  # not needed for adb-only build
# include(sqlite3.cmake)  # not needed for adb-only build
# include(etc1tool.cmake)  # not needed for adb-only build
# include(e2fsprogs.cmake)  # not needed for adb-only build
# include(f2fs-tools.cmake)  # not needed for adb-only build
# include(fastboot.cmake)  # not needed for adb-only build
include(adb.cmake)