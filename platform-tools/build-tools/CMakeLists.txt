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

project(build-tools)
# executable files output path
set(EXECUTABLE_OUTPUT_PATH ${CMAKE_BINARY_DIR}/bin/${PROJECT_NAME})

include(aapt.cmake)
include(aapt2.cmake)
include(aidl.cmake)
include(zipalign.cmake)
include(split-select.cmake)
include(dexdump.cmake)