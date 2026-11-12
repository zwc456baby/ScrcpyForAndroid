#!/usr/bin/env python3
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
# pylint: disable=not-callable, line-too-long, no-else-return

import argparse
import json
import re
import shutil
import subprocess
import sys
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

ROOT = Path(__file__).resolve().parent
VERSION_HEADER = ROOT / 'patches/misc/platform_tools_version.h'


def tools_version():
    """read PLATFORM_TOOLS_VERSION from patches/misc/platform_tools_version.h"""
    text = VERSION_HEADER.read_text()
    match = re.search(r'^\s*#define\s+PLATFORM_TOOLS_VERSION\s+"([^"]+)"', text, re.M)
    if match is None:
        sys.exit('can not find PLATFORM_TOOLS_VERSION in {}'.format(VERSION_HEADER))
    return match.group(1)


def check(command):
    if shutil.which(command) is None:
        sys.exit('please install the {} package'.format(command))


def clone(repo, default_branch, retries):
    path = ROOT / repo['path']
    if path.exists():
        return '{} already exists, skip'.format(repo['path'])
    # a repo can pin its own branch/tag (e.g. zlib), otherwise use the AOSP tag
    branch = repo.get('branch', default_branch)
    command = ['git', 'clone', '-q', '-c', 'advice.detachedHead=false',
               '--depth', '1', '--branch', branch, repo['url'], str(path)]
    for attempt in range(1, retries + 1):
        result = subprocess.run(command, capture_output=True, text=True)
        if result.returncode == 0:
            return 'cloned {} ({})'.format(repo['path'], branch)
        shutil.rmtree(path, ignore_errors=True)
        if attempt < retries:
            time.sleep(5 * attempt)
    raise RuntimeError('failed to clone {} ({}):\n{}'.format(repo['url'], branch, result.stderr))


def replace_in_file(path, old, new):
    path = ROOT / path
    if path.exists():
        text = path.read_text()
        if old in text:
            path.write_text(text.replace(old, new))


def apply_patch(patch):
    """apply a patch file if it is applicable, the patch files of this project
    use different path prefixes, so try both the project root and src/"""
    for workdir in (ROOT, ROOT / 'src'):
        base = ['patch', '-p1', '-s', '-f', '-d', str(workdir), '-i', str(patch)]
        if subprocess.run(base + ['-N', '--dry-run'], capture_output=True).returncode == 0:
            subprocess.run(base + ['-N'], check=True)
            return 'applied'
        if subprocess.run(base + ['-R', '--dry-run'], capture_output=True).returncode == 0:
            return 'already applied'
    return 'not applicable for this version, skipped'


def patches():
    inc = ROOT / 'src/incremental_delivery/sysprop/include'
    inc.mkdir(parents=True, exist_ok=True)
    shutil.copy2(ROOT / 'patches/misc/IncrementalProperties.sysprop.h', inc)
    shutil.copy2(ROOT / 'patches/misc/IncrementalProperties.sysprop.cpp', inc.parent)

    shutil.copy2(ROOT / 'patches/misc/deployagent.inc', ROOT / 'src/adb/fastdeploy/deployagent')
    shutil.copy2(ROOT / 'patches/misc/deployagentscript.inc', ROOT / 'src/adb/fastdeploy/deployagent')

    shutil.copy2(VERSION_HEADER, ROOT / 'src/soong/cc/libbuildversion/include')

    aapt2 = 'src/base/tools/aapt2/'
    replace_in_file(aapt2 + 'ApkInfo.proto', 'frameworks/base/tools/aapt2/Resources.proto', 'Resources.proto')
    replace_in_file(aapt2 + 'Resources.proto', 'frameworks/base/tools/aapt2/Configuration.proto', 'Configuration.proto')
    replace_in_file(aapt2 + 'ResourcesInternal.proto', 'frameworks/base/tools/aapt2/Configuration.proto', 'Configuration.proto')
    replace_in_file(aapt2 + 'ResourcesInternal.proto', 'frameworks/base/tools/aapt2/Resources.proto', 'Resources.proto')

    replace_in_file('src/abseil-cpp/CMakeLists.txt', '/usr/src/googletest', '${CMAKE_SOURCE_DIR}/src/googletest')

    # symlink googletest to boringssl/**/googletest
    dest = ROOT / 'src/boringssl/src/third_party/googletest'
    if dest.parent.exists() and not dest.exists():
        dest.symlink_to(ROOT / 'src/googletest')

    # the source patches only apply to specific versions, apply them when possible
    for patch in sorted((ROOT / 'patches').rglob('*.patch')):
        print('patch {}: {}'.format(patch.relative_to(ROOT), apply_patch(patch)))


def main():
    version = tools_version()
    parser = argparse.ArgumentParser()
    parser.add_argument('--tags', default='platform-tools-{}'.format(version),
                        help='git tag or branch of the AOSP repositories, default is platform-tools-{} '
                             '(from patches/misc/platform_tools_version.h)'.format(version))
    parser.add_argument('--jobs', type=int, default=8, help='clone N repositories in parallel')
    parser.add_argument('--retries', type=int, default=3, help='retry times of a failed clone')
    parser.add_argument('--skip-patch', action='store_true', help='only clone, do not patch the sources')
    args = parser.parse_args()

    # check necessary packages
    for command in ('git', 'patch', 'go', 'bison', 'flex'):
        check(command)

    repos = json.loads((ROOT / 'repository.json').read_text())
    print('fetching sources: {}'.format(args.tags))
    errors = []
    with ThreadPoolExecutor(max_workers=max(1, args.jobs)) as pool:
        futures = [pool.submit(clone, repo, args.tags, args.retries) for repo in repos]
        for future in as_completed(futures):
            try:
                print(future.result(), flush=True)
            except RuntimeError as e:
                errors.append(str(e))
    if errors:
        sys.exit('\n'.join(errors))

    if not args.skip_patch:
        patches()

    print('download success!!')


if __name__ == '__main__':
    main()
