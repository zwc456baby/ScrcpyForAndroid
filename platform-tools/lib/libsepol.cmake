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

add_library(libsepol STATIC
    ${SRC}/selinux/libsepol/src/assertion.c
    ${SRC}/selinux/libsepol/src/avrule_block.c
    ${SRC}/selinux/libsepol/src/avtab.c
    ${SRC}/selinux/libsepol/src/boolean_record.c
    ${SRC}/selinux/libsepol/src/booleans.c
    ${SRC}/selinux/libsepol/src/conditional.c
    ${SRC}/selinux/libsepol/src/constraint.c
    ${SRC}/selinux/libsepol/src/context.c
    ${SRC}/selinux/libsepol/src/context_record.c
    ${SRC}/selinux/libsepol/src/debug.c
    ${SRC}/selinux/libsepol/src/ebitmap.c
    ${SRC}/selinux/libsepol/src/expand.c
    ${SRC}/selinux/libsepol/src/handle.c
    ${SRC}/selinux/libsepol/src/hashtab.c
    ${SRC}/selinux/libsepol/src/hierarchy.c
    ${SRC}/selinux/libsepol/src/iface_record.c
    ${SRC}/selinux/libsepol/src/interfaces.c
    ${SRC}/selinux/libsepol/src/kernel_to_cil.c
    ${SRC}/selinux/libsepol/src/kernel_to_common.c
    ${SRC}/selinux/libsepol/src/kernel_to_conf.c
    ${SRC}/selinux/libsepol/src/link.c
    ${SRC}/selinux/libsepol/src/mls.c
    ${SRC}/selinux/libsepol/src/module.c
    ${SRC}/selinux/libsepol/src/module_to_cil.c
    ${SRC}/selinux/libsepol/src/node_record.c
    ${SRC}/selinux/libsepol/src/nodes.c
    ${SRC}/selinux/libsepol/src/optimize.c
    ${SRC}/selinux/libsepol/src/polcaps.c
    ${SRC}/selinux/libsepol/src/policydb.c
    ${SRC}/selinux/libsepol/src/policydb_convert.c
    ${SRC}/selinux/libsepol/src/policydb_public.c
    ${SRC}/selinux/libsepol/src/policydb_validate.c
    ${SRC}/selinux/libsepol/src/port_record.c
    ${SRC}/selinux/libsepol/src/ports.c
    ${SRC}/selinux/libsepol/src/services.c
    ${SRC}/selinux/libsepol/src/sidtab.c
    ${SRC}/selinux/libsepol/src/symtab.c
    ${SRC}/selinux/libsepol/src/user_record.c
    ${SRC}/selinux/libsepol/src/users.c
    ${SRC}/selinux/libsepol/src/util.c
    ${SRC}/selinux/libsepol/src/write.c
    ${SRC}/selinux/libsepol/cil/src/android.c
    ${SRC}/selinux/libsepol/cil/src/cil_binary.c
    ${SRC}/selinux/libsepol/cil/src/cil_build_ast.c
    ${SRC}/selinux/libsepol/cil/src/cil.c
    ${SRC}/selinux/libsepol/cil/src/cil_copy_ast.c
    ${SRC}/selinux/libsepol/cil/src/cil_find.c
    ${SRC}/selinux/libsepol/cil/src/cil_fqn.c
    ${SRC}/selinux/libsepol/cil/src/cil_lexer.l
    ${SRC}/selinux/libsepol/cil/src/cil_list.c
    ${SRC}/selinux/libsepol/cil/src/cil_log.c
    ${SRC}/selinux/libsepol/cil/src/cil_mem.c
    ${SRC}/selinux/libsepol/cil/src/cil_parser.c
    ${SRC}/selinux/libsepol/cil/src/cil_policy.c
    ${SRC}/selinux/libsepol/cil/src/cil_post.c
    ${SRC}/selinux/libsepol/cil/src/cil_reset_ast.c
    ${SRC}/selinux/libsepol/cil/src/cil_resolve_ast.c
    ${SRC}/selinux/libsepol/cil/src/cil_stack.c
    ${SRC}/selinux/libsepol/cil/src/cil_strpool.c
    ${SRC}/selinux/libsepol/cil/src/cil_symtab.c
    ${SRC}/selinux/libsepol/cil/src/cil_tree.c
    ${SRC}/selinux/libsepol/cil/src/cil_verify.c
    ${SRC}/selinux/libsepol/cil/src/cil_write_ast.c
    )

target_compile_definitions(libsepol PRIVATE
    -DHAVE_REALLOCARRAY
    -D_GNU_SOURCE
    )
target_include_directories(libsepol PUBLIC
    ${SRC}/selinux/libselinux/include 
    ${SRC}/selinux/libsepol/include
    )
target_include_directories(libsepol PRIVATE
    ${SRC}/selinux/libsepol/src
    ${SRC}/selinux/libsepol/cil/include
    )