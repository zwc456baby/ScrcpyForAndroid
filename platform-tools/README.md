# android-sdk-tools

English | [简体中文](README_cn.md)

Build the Android `platform-tools` and `build-tools` from the AOSP source with CMake,
such as `aapt aapt2 aidl zipalign adb fastboot` ... etc.

* **Static by default**: the executables are fully statically linked, no runtime dependencies
* **Multiple toolchains**: Android NDK, zig, the host compilers, or any CMake toolchain file
* **Multiple arch**: `aarch64`, `arm`, `x86_64`, `i686`
* **Prebuilt binaries**: built by GitHub Actions and published to [Releases](../../releases)

If you need other tools, please refer to the existing tools to add a CMake file.

## Tools

| directory | tools |
| --- | --- |
| `build-tools` | `aapt` `aapt2` `aidl` `zipalign` `dexdump` `split-select` |
| `platform-tools` | `adb` `fastboot` `sqlite3` `etc1tool` `hprof-conv` `e2fsdroid` `sload_f2fs` `mke2fs` `make_f2fs` `make_f2fs_casefold` |
| `others` | `veridex` |

## Toolchains

| toolchain | libc | output |
| --- | --- | --- |
| `ndk` (default) | bionic | Android executables |
| `zig` | musl (static) / glibc (shared) | Linux executables, the static ones run on Android as well |
| `host` | the build machine (`$CC`/`$CXX`, clang by default) | native executables |
| `custom` | any CMake toolchain file | cross gcc / clang / musl-cross ... |

The Android abi names `arm64-v8a`, `armeabi-v7a`, `x86` are accepted for `--arch` as well.

> The zig toolchain is experimental: the build system supports it, but some AOSP sources
> are only built for bionic / glibc upstream and may need extra patches for musl.

## Download

Download `android-sdk-tools-static-<arch>.zip` from [Releases](../../releases).
The release tag is the `PLATFORM_TOOLS_VERSION` of
[`patches/misc/platform_tools_version.h`](patches/misc/platform_tools_version.h), e.g. `36.0.0`.

## Build with Docker

The [`Dockerfile`](Dockerfile) provides a build environment with the NDK, zig and all the dependencies,
the project directory is mounted into the container.

```bash
docker build --platform linux/amd64 -t android-sdk-tools .

# fetch the sources and build with the NDK
docker run --rm -it --platform linux/amd64 -u "$(id -u):$(id -g)" -v "$PWD:/work" android-sdk-tools \
    sh -c "python3 get_source.py && python3 build.py --arch aarch64"

# build with zig
docker run --rm -it --platform linux/amd64 -u "$(id -u):$(id -g)" -v "$PWD:/work" android-sdk-tools \
    python3 build.py --toolchain zig --arch aarch64
```

Build arguments: `NDK_VERSION` (default `r29`) and `ZIG_VERSION` (default `0.16.0`),
set one to empty to skip it, e.g. `--build-arg ZIG_VERSION=`.

## Build manually

### 1. Dependencies

`git` `patch` `python3` `cmake (>= 3.19)` `ninja` `bison` `flex` `go`, a host C/C++ compiler,
and the toolchain you want to use (NDK or zig).

```bash
# Debian / Ubuntu
sudo apt install git patch python3 cmake ninja-build bison flex golang-go build-essential
```

### 2. Get the source

`get_source.py` clones all the repositories of [`repository.json`](repository.json) and applies the patches.
The default tag is `platform-tools-<PLATFORM_TOOLS_VERSION>`.

```bash
python get_source.py

# specific branches or tags
# see available tags https://android.googlesource.com/platform/frameworks/base/+refs
python get_source.py --tags platform-tools-36.0.0

# other options: --jobs (parallel clones), --retries, --skip-patch
python get_source.py --help
```

The source patches only apply to specific tags,
a patch which can't be applied to the selected tag is skipped with a message.

### 3. Build

`build.py` builds the host `protoc` (used to generate the sources of aapt2 and adb) automatically,
then compiles the tools and packages them as `android-sdk-tools-<static|shared>-<arch>.zip`.

```bash
# NDK (default toolchain), static link (default)
python build.py --ndk=/path/to/android-ndk-xxx --arch=aarch64

# zig, static musl executables
python build.py --toolchain=zig --arch=aarch64

# dynamic link
python build.py --ndk=/path/to/android-ndk-xxx --arch=aarch64 --link=shared

# the host compilers
python build.py --toolchain=host --arch=x86_64 --cc=clang --cxx=clang++

# any other toolchain with a CMake toolchain file
python build.py --toolchain=custom --toolchain-file=/path/to/toolchain.cmake --arch=aarch64

# use an existing host protoc
python build.py --ndk=/path/to/android-ndk-xxx --arch=aarch64 --protoc=/path/to/protoc

# build specified targets, recommended to locate errors
python build.py --ndk=/path/to/android-ndk-xxx --arch=aarch64 --target aapt2 adb

# extra cmake arguments after --
python build.py --toolchain=zig --arch=x86_64 -- -DSDK_STRIP=OFF

# all options
python build.py --help
```

The NDK path can also be set by `ANDROID_NDK_HOME` / `ANDROID_NDK_ROOT`.
The build directory is `build/<toolchain>-<link>-<arch>`, the executables are in its `bin` directory.

### Use CMake directly

| option | default | description |
| --- | --- | --- |
| `SDK_STATIC` | `ON` | link fully static executables |
| `SDK_STRIP` | `ON` | strip the executables at link time (Release) |
| `PROTOC_PATH` | | the host protoc |
| `ZIG_TARGET` | | target of [`cmake/toolchains/zig.cmake`](cmake/toolchains/zig.cmake), e.g. `aarch64-linux-musl` |
| `ZIG_CPU` | | optional `-mcpu` of zig, e.g. `cortex_a53` |

```bash
cmake -GNinja -B build/zig -DCMAKE_TOOLCHAIN_FILE=cmake/toolchains/zig.cmake \
    -DZIG_TARGET=aarch64-linux-musl -DPROTOC_PATH=/path/to/protoc
ninja -C build/zig
```

## Project layout

```
├── CMakeLists.txt            top-level project, third-party libraries
├── cmake/
│   ├── platform.cmake        static link, strip, system libraries (toolchain independent)
│   ├── zlib.cmake            bundled static zlib
│   ├── modules/FindZLIB.cmake  redirects find_package(ZLIB) to the bundled zlib
│   └── toolchains/zig.cmake  zig toolchain file
├── lib/                      AOSP libraries (libbase, libutils, liblog ...)
├── build-tools/              aapt, aapt2, aidl ...
├── platform-tools/           adb, fastboot ...
├── others/                   veridex
├── patches/                  source patches and generated files
├── repository.json           the source repositories
├── get_source.py             fetch the sources
├── build.py                  build and package
├── Dockerfile                build environment
└── .github/workflows/        CI
```

## GitHub Actions

[`.github/workflows/build.yml`](.github/workflows/build.yml) builds all the arch on every push
with the NDK (zig can be selected when running the workflow manually), then publishes a release:

* the tag is `PLATFORM_TOOLS_VERSION`, e.g. `36.0.0`, the title is `v36.0.0`
* the release notes are the latest commit message (replaced, never appended)
* when `PLATFORM_TOOLS_VERSION` doesn't change, no new release is created:
  the assets of the existing release are overwritten and the tag is moved to the latest commit
* the assets are split by arch: `android-sdk-tools-static-aarch64.zip`, `android-sdk-tools-static-arm.zip`,
  `android-sdk-tools-static-x86_64.zip`, `android-sdk-tools-static-i686.zip`

Pull requests are built without publishing.

## License

[Apache License 2.0](LICENSE.txt)
