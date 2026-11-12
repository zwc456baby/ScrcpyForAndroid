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

add_library(libabsl_base STATIC
    ${SRC}/abseil-cpp/absl/base/internal/cycleclock.cc
    ${SRC}/abseil-cpp/absl/base/internal/exponential_biased.cc
    ${SRC}/abseil-cpp/absl/base/internal/low_level_alloc.cc
    ${SRC}/abseil-cpp/absl/base/internal/periodic_sampler.cc
    ${SRC}/abseil-cpp/absl/base/internal/raw_logging.cc
    ${SRC}/abseil-cpp/absl/base/internal/spinlock.cc
    ${SRC}/abseil-cpp/absl/base/internal/spinlock_wait.cc
    ${SRC}/abseil-cpp/absl/base/internal/strerror.cc
    ${SRC}/abseil-cpp/absl/base/internal/sysinfo.cc
    ${SRC}/abseil-cpp/absl/base/internal/thread_identity.cc
    ${SRC}/abseil-cpp/absl/base/internal/throw_delegate.cc
    ${SRC}/abseil-cpp/absl/base/internal/unscaledcycleclock.cc
    )
target_include_directories(libabsl_base PUBLIC
    ${SRC}/abseil-cpp
    )

add_library(libabsl_strings STATIC
    ${SRC}/abseil-cpp/absl/strings/ascii.cc
    ${SRC}/abseil-cpp/absl/strings/charconv.cc
    ${SRC}/abseil-cpp/absl/strings/cord.cc
    ${SRC}/abseil-cpp/absl/strings/escaping.cc
    ${SRC}/abseil-cpp/absl/strings/internal/charconv_bigint.cc
    ${SRC}/abseil-cpp/absl/strings/internal/charconv_parse.cc
    ${SRC}/abseil-cpp/absl/strings/internal/cord_internal.cc
    ${SRC}/abseil-cpp/absl/strings/internal/cord_rep_ring.cc
    ${SRC}/abseil-cpp/absl/strings/internal/escaping.cc
    ${SRC}/abseil-cpp/absl/strings/internal/memutil.cc
    ${SRC}/abseil-cpp/absl/strings/internal/ostringstream.cc
    ${SRC}/abseil-cpp/absl/strings/internal/str_format/arg.cc
    ${SRC}/abseil-cpp/absl/strings/internal/str_format/bind.cc
    ${SRC}/abseil-cpp/absl/strings/internal/str_format/extension.cc
    ${SRC}/abseil-cpp/absl/strings/internal/str_format/float_conversion.cc
    ${SRC}/abseil-cpp/absl/strings/internal/str_format/output.cc
    ${SRC}/abseil-cpp/absl/strings/internal/str_format/parser.cc
    ${SRC}/abseil-cpp/absl/strings/internal/utf8.cc
    ${SRC}/abseil-cpp/absl/strings/match.cc
    ${SRC}/abseil-cpp/absl/strings/numbers.cc
    ${SRC}/abseil-cpp/absl/strings/str_cat.cc
    ${SRC}/abseil-cpp/absl/strings/str_replace.cc
    ${SRC}/abseil-cpp/absl/strings/str_split.cc
    ${SRC}/abseil-cpp/absl/strings/string_view.cc
    ${SRC}/abseil-cpp/absl/strings/substitute.cc
    )
target_include_directories(libabsl_strings PUBLIC
    ${SRC}/abseil-cpp
    )
    