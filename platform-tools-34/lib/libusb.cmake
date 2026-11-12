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

add_library(libusb STATIC
    ${SRC}/libusb/libusb/core.c
    ${SRC}/libusb/libusb/descriptor.c
    ${SRC}/libusb/libusb/hotplug.c
    ${SRC}/libusb/libusb/io.c
    ${SRC}/libusb/libusb/sync.c
    ${SRC}/libusb/libusb/strerror.c
    ${SRC}/libusb/libusb/os/linux_usbfs.c
    ${SRC}/libusb/libusb/os/events_posix.c
    ${SRC}/libusb/libusb/os/threads_posix.c
    ${SRC}/libusb/libusb/os/linux_netlink.c
    )
target_include_directories(libusb PRIVATE
    ${SRC}/libusb/libusb
    ${SRC}/libusb/libusb/os
    ${SRC}/libusb/linux
    )
target_compile_options(libusb PRIVATE
    -fvisibility=hidden 
    -pthread
    )