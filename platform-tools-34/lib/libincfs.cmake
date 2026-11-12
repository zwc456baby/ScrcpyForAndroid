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

add_library(libincfs STATIC
    ${SRC}/incremental_delivery/incfs/incfs_ndk.c
    ${SRC}/incremental_delivery/incfs/incfs.cpp
    ${SRC}/incremental_delivery/incfs/MountRegistry.cpp
    ${SRC}/incremental_delivery/incfs/path.cpp
    ${SRC}/incremental_delivery/incfs/util/map_ptr.cpp
    ${SRC}/incremental_delivery/sysprop/IncrementalProperties.sysprop.cpp
    )
    
target_include_directories(libincfs PRIVATE
    ${SRC}/incremental_delivery/incfs/include 
    ${SRC}/incremental_delivery/incfs/util/include 
    ${SRC}/incremental_delivery/sysprop/include
    ${SRC}/incremental_delivery/incfs/kernel-headers
    ${SRC}/libbase/include
    ${SRC}/core/libutils/include
    ${SRC}/boringssl/include
    ${SRC}/selinux/libselinux/include
    ${SRC}/logging/liblog/include 
    )
