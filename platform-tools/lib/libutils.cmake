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

set(LIBUTILS_BINDER_SRC
    ${SRC}/core/libutils/binder/Errors.cpp
    ${SRC}/core/libutils/binder/RefBase.cpp
    ${SRC}/core/libutils/binder/SharedBuffer.cpp
    ${SRC}/core/libutils/binder/String16.cpp
    ${SRC}/core/libutils/binder/String8.cpp
    ${SRC}/core/libutils/binder/StrongPointer.cpp
    ${SRC}/core/libutils/binder/Unicode.cpp
    ${SRC}/core/libutils/binder/VectorImpl.cpp
    )

add_library(libutils STATIC
    ${SRC}/core/libutils/FileMap.cpp
    ${SRC}/core/libutils/JenkinsHash.cpp
    ${SRC}/core/libutils/LightRefBase.cpp
    ${SRC}/core/libutils/NativeHandle.cpp
    ${SRC}/core/libutils/Printer.cpp
    ${SRC}/core/libutils/StopWatch.cpp
    ${SRC}/core/libutils/SystemClock.cpp
    ${SRC}/core/libutils/Threads.cpp
    ${SRC}/core/libutils/Timers.cpp
    ${SRC}/core/libutils/Tokenizer.cpp
    ${SRC}/core/libutils/misc.cpp
    ${SRC}/core/libutils/Looper.cpp
    ${LIBUTILS_BINDER_SRC}
    )

# see core/libutils/Android.bp, Trace.cpp is only for android
if(SDK_TARGET_ANDROID)
    target_sources(libutils PRIVATE ${SRC}/core/libutils/Trace.cpp)
endif()

target_include_directories(libutils PRIVATE
    ${SRC}/core/include
    ${SRC}/core/libutils/include
    # platform-tools-35.0.2 moved <system/graphics.h> and <system/thread_defs.h>
    # into core/libsystem/include, which utils/ThreadDefs.h needs
    ${SRC}/core/libsystem/include
    ${SRC}/core/libprocessgroup/include
    ${SRC}/core/libvndksupport/include
    ${SRC}/logging/liblog/include 
    ${SRC}/libbase/include
    ${SRC}/unwinding/libbacktrace/include
    )
    
    