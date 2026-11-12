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

include(libbase.cmake)
include(liblog.cmake)
include(libcutils.cmake)
include(libutils.cmake)
include(libandroidfw.cmake)
include(libincfs.cmake)
include(libselinux.cmake)
include(libsepol.cmake)
include(libsparse.cmake)
include(libziparchive.cmake)
include(libbuildversion.cmake)
include(libpackagelistparser.cmake)
# libprocessgroup is only used by adbd, not by the adb client; its source layout
# changed in Android 16, so it is not configured here (the adb build doesn't need it)
#include(libprocessgroup.cmake)
include(libusb.cmake)
include(libdiagnoseusb.cmake)
include(libmdnssd.cmake)
include(libopenscreen.cmake)