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

add_library(libopenscreen STATIC 
    ${SRC}/openscreen/discovery/dnssd/impl/conversion_layer.cc
    ${SRC}/openscreen/discovery/dnssd/impl/dns_data_graph.cc
    ${SRC}/openscreen/discovery/dnssd/impl/instance_key.cc
    ${SRC}/openscreen/discovery/dnssd/impl/network_interface_config.cc
    ${SRC}/openscreen/discovery/dnssd/impl/publisher_impl.cc
    ${SRC}/openscreen/discovery/dnssd/impl/querier_impl.cc
    ${SRC}/openscreen/discovery/dnssd/impl/service_dispatcher.cc
    ${SRC}/openscreen/discovery/dnssd/impl/service_instance.cc
    ${SRC}/openscreen/discovery/dnssd/impl/service_key.cc
    ${SRC}/openscreen/discovery/dnssd/public/dns_sd_instance.cc
    ${SRC}/openscreen/discovery/dnssd/public/dns_sd_instance_endpoint.cc
    ${SRC}/openscreen/discovery/dnssd/public/dns_sd_txt_record.cc
    ${SRC}/openscreen/discovery/mdns/mdns_probe.cc
    ${SRC}/openscreen/discovery/mdns/mdns_probe_manager.cc
    ${SRC}/openscreen/discovery/mdns/mdns_publisher.cc
    ${SRC}/openscreen/discovery/mdns/mdns_querier.cc
    ${SRC}/openscreen/discovery/mdns/mdns_reader.cc
    ${SRC}/openscreen/discovery/mdns/mdns_receiver.cc
    ${SRC}/openscreen/discovery/mdns/mdns_records.cc
    ${SRC}/openscreen/discovery/mdns/mdns_responder.cc
    ${SRC}/openscreen/discovery/mdns/mdns_sender.cc
    ${SRC}/openscreen/discovery/mdns/mdns_service_impl.cc
    ${SRC}/openscreen/discovery/mdns/mdns_trackers.cc
    ${SRC}/openscreen/discovery/mdns/mdns_writer.cc
    ${SRC}/openscreen/discovery/mdns/public/mdns_service.cc
    ${SRC}/openscreen/platform/base/error.cc
    ${SRC}/openscreen/platform/base/interface_info.cc
    ${SRC}/openscreen/platform/base/ip_address.cc
    ${SRC}/openscreen/platform/base/udp_packet.cc
    ${SRC}/openscreen/platform/api/udp_socket.cc
    ${SRC}/openscreen/util/alarm.cc
    ${SRC}/openscreen/util/big_endian.cc
    ${SRC}/openscreen/platform/impl/time.cc
    ${SRC}/openscreen/platform/impl/network_interface.cc
    ${SRC}/openscreen/platform/impl/network_interface_linux.cc
    )

target_compile_options(libopenscreen PRIVATE 
    -fno-strict-aliasing
    -Wno-address-of-packed-member
    -Wno-array-bounds
    -Wno-pointer-sign
    -Wno-unused
    -Wno-unused-but-set-variable
    -Wno-unused-parameter
    -Wno-missing-field-initializers
    -Werror=implicit-function-declaration
    )

target_compile_options(libopenscreen PRIVATE 
    -std=c++17
    -fno-exceptions 
    -fno-unwind-tables 
    -fno-asynchronous-unwind-tables
    )

target_include_directories(libopenscreen PRIVATE
    ${SRC}/openscreen
    ${SRC}/abseil-cpp
    )
    