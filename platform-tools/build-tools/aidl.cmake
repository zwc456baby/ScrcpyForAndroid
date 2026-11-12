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

find_package(BISON REQUIRED)
find_package(FLEX REQUIRED)

if(NOT BISON_FOUND)
    message(FATAL_ERROR "can't find the bison")
endif()

if(NOT FLEX_FOUND)
    message(FATAL_ERROR "can't find the flex")
endif()

# generate aidl_language_y.h and aidl_language_y.cpp
bison_target(
    LangParser ${SRC}/aidl/aidl_language_y.yy ${SRC}/aidl/aidl_language_y.cpp
    DEFINES_FILE ${SRC}/aidl/aidl_language_y.h
    )

# generate aidl_language_l.h and aidl_language_l.cpp
flex_target(
    LangScanner ${SRC}/aidl/aidl_language_l.ll ${SRC}/aidl/aidl_language_l.cpp
    DEFINES_FILE ${SRC}/aidl/aidl_language_l.h
    )

# add dependency
add_flex_bison_dependency(LangScanner LangParser)

message(STATUS ${FLEX_LangScanner_OUTPUTS})          
message(STATUS ${BISON_LangParser_OUTPUTS})

add_executable(aidl
    ${SRC}/aidl/aidl_checkapi.cpp
    ${SRC}/aidl/aidl_const_expressions.cpp
    ${SRC}/aidl/aidl_dumpapi.cpp
    ${SRC}/aidl/aidl_language_l.ll
    ${SRC}/aidl/aidl_language_y.yy
    ${SRC}/aidl/aidl_language.cpp
    ${SRC}/aidl/aidl_to_common.cpp
    ${SRC}/aidl/aidl_to_cpp_common.cpp
    ${SRC}/aidl/aidl_to_cpp.cpp
    ${SRC}/aidl/aidl_to_java.cpp
    ${SRC}/aidl/aidl_to_ndk.cpp
    ${SRC}/aidl/aidl_to_rust.cpp
    ${SRC}/aidl/aidl_typenames.cpp
    ${SRC}/aidl/aidl.cpp
    ${SRC}/aidl/ast_java.cpp
    ${SRC}/aidl/check_valid.cpp
    ${SRC}/aidl/code_writer.cpp
    ${SRC}/aidl/comments.cpp
    ${SRC}/aidl/diagnostics.cpp
    ${SRC}/aidl/generate_aidl_mappings.cpp
    ${SRC}/aidl/generate_cpp.cpp
    ${SRC}/aidl/generate_cpp_analyzer.cpp
    ${SRC}/aidl/generate_java_binder.cpp
    ${SRC}/aidl/generate_java.cpp
    ${SRC}/aidl/generate_ndk.cpp
    ${SRC}/aidl/generate_rust.cpp
    ${SRC}/aidl/import_resolver.cpp
    ${SRC}/aidl/io_delegate.cpp
    ${SRC}/aidl/location.cpp
    ${SRC}/aidl/logging.cpp
    ${SRC}/aidl/options.cpp
    ${SRC}/aidl/parser.cpp
    ${SRC}/aidl/permission.cpp
    ${SRC}/aidl/preprocess.cpp
    ${SRC}/aidl/main.cpp
    ${BISON_LangParser_OUTPUTS}
    ${FLEX_LangScanner_OUTPUTS}
    )
    
target_include_directories(aidl PRIVATE
    ${SRC}/libbase/include
    ${SRC}/fmtlib/include
    ${SRC}/googletest/googletest/include
    )
    
target_link_libraries(aidl
    libbase
    liblog
    gmock
    fmt::fmt
    sdk_system_libs
    )