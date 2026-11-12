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

add_library(liblog STATIC
    ${SRC}/logging/liblog/log_event_list.cpp
    ${SRC}/logging/liblog/log_event_write.cpp
    ${SRC}/logging/liblog/logger_name.cpp
    ${SRC}/logging/liblog/logger_read.cpp
    ${SRC}/logging/liblog/logger_write.cpp
    ${SRC}/logging/liblog/properties.cpp
    ${SRC}/logging/liblog/logprint.cpp
    ${SRC}/logging/liblog/event_tag_map.cpp
    ${SRC}/logging/liblog/log_time.cpp
    ${SRC}/logging/liblog/pmsg_reader.cpp
    ${SRC}/logging/liblog/pmsg_writer.cpp
    ${SRC}/logging/liblog/logd_reader.cpp
    ${SRC}/logging/liblog/logd_writer.cpp
    )

target_compile_definitions(liblog PRIVATE
    -DLIBLOG_LOG_TAG=1006 
    -D_XOPEN_SOURCE=700 
    -DFAKE_LOG_DEVICE=1
    -DSNET_EVENT_LOG_TAG=1397638686
    -DANDROID_DEBUGGABLE=0
    )
    
target_include_directories(liblog PRIVATE
    ${SRC}/core/include
    ${SRC}/logging/liblog/include
    ${SRC}/core/libcutils/include
    ${SRC}/libbase/include
    )
    