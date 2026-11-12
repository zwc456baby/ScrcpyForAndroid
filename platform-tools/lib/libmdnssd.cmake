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

add_library(libmdnssd STATIC
    ${SRC}/mdnsresponder/mDNSShared/dnssd_clientlib.c
    ${SRC}/mdnsresponder/mDNSShared/dnssd_clientstub.c
    ${SRC}/mdnsresponder/mDNSShared/dnssd_ipc.c
    )
target_compile_options(libmdnssd PRIVATE
    -fno-strict-aliasing
    -fwrapv
    )
target_compile_definitions(libmdnssd PRIVATE
    -D_GNU_SOURCE 
    -DHAVE_IPV6 
    -DNOT_HAVE_SA_LEN 
    -DPLATFORM_NO_RLIMIT 
    -DMDNS_UDS_SERVERPATH="/dev/socket/mdnsd"
    -DMDNS_USERNAME="mdnsr"
    -DMDNS_DEBUGMSGS=0
    )