# android-sdk-tools

[English](README.md) | 简体中文

使用 CMake 从 AOSP 源码编译 Android 的 `platform-tools` 和 `build-tools`，例如 `aapt aapt2 aidl zipalign adb fastboot` 等。

* **默认静态编译**：可执行文件完全静态链接，没有运行时依赖
* **多工具链**：Android NDK、zig、本机编译器，或任意 CMake 工具链文件
* **多架构**：`aarch64`、`arm`、`x86_64`、`i686`
* **预编译产物**：由 GitHub Actions 编译并发布到 [Releases](../../releases)

如果需要其他工具，可以参考现有工具的写法添加 CMake 文件。

## 工具列表

| 目录 | 工具 |
| --- | --- |
| `build-tools` | `aapt` `aapt2` `aidl` `zipalign` `dexdump` `split-select` |
| `platform-tools` | `adb` `fastboot` `sqlite3` `etc1tool` `hprof-conv` `e2fsdroid` `sload_f2fs` `mke2fs` `make_f2fs` `make_f2fs_casefold` |
| `others` | `veridex` |

## 工具链

| 工具链 | libc | 产物 |
| --- | --- | --- |
| `ndk`（默认） | bionic | Android 可执行文件 |
| `zig` | musl（静态）/ glibc（动态） | Linux 可执行文件，静态版本也可以在 Android 上运行 |
| `host` | 本机（`$CC`/`$CXX`，默认 clang） | 本机可执行文件 |
| `custom` | 任意 CMake 工具链文件 | 交叉 gcc / clang / musl-cross 等 |

`--arch` 也接受 Android ABI 名称：`arm64-v8a`、`armeabi-v7a`、`x86`。

> zig 工具链目前属于实验性支持：构建系统已经支持，但部分 AOSP 源码上游只在 bionic / glibc 下编译过，
> 在 musl 下可能还需要额外的补丁。

## 下载

从 [Releases](../../releases) 下载 `android-sdk-tools-static-<架构>.zip`。
Release 的 tag 来自 [`patches/misc/platform_tools_version.h`](patches/misc/platform_tools_version.h)
中的 `PLATFORM_TOOLS_VERSION`，例如 `36.0.0`。

## 使用 Docker 编译

[`Dockerfile`](Dockerfile) 提供了包含 NDK、zig 及全部依赖的编译环境，项目目录以挂载方式进入容器。

```bash
docker build --platform linux/amd64 -t android-sdk-tools .

# 获取源码并使用 NDK 编译
docker run --rm -it --platform linux/amd64 -u "$(id -u):$(id -g)" -v "$PWD:/work" android-sdk-tools \
    sh -c "python3 get_source.py && python3 build.py --arch aarch64"

# 使用 zig 编译
docker run --rm -it --platform linux/amd64 -u "$(id -u):$(id -g)" -v "$PWD:/work" android-sdk-tools \
    python3 build.py --toolchain zig --arch aarch64
```

构建参数：`NDK_VERSION`（默认 `r29`）和 `ZIG_VERSION`（默认 `0.16.0`），
设为空即可跳过安装，例如 `--build-arg ZIG_VERSION=`。

## 手动编译

### 1. 依赖

`git` `patch` `python3` `cmake (>= 3.19)` `ninja` `bison` `flex` `go`、本机 C/C++ 编译器，
以及要使用的工具链（NDK 或 zig）。

```bash
# Debian / Ubuntu
sudo apt install git patch python3 cmake ninja-build bison flex golang-go build-essential
```

### 2. 获取源码

`get_source.py` 会克隆 [`repository.json`](repository.json) 中的所有仓库并应用补丁，
默认 tag 为 `platform-tools-<PLATFORM_TOOLS_VERSION>`。

```bash
python get_source.py

# 指定分支或 tag
# 可用的 tag 见 https://android.googlesource.com/platform/frameworks/base/+refs
python get_source.py --tags platform-tools-36.0.0

# 其他选项：--jobs（并行克隆数）、--retries（重试次数）、--skip-patch（不打补丁）
python get_source.py --help
```

源码补丁只适用于特定的 tag，无法应用到当前 tag 的补丁会被跳过并给出提示。

### 3. 编译

`build.py` 会自动编译本机的 `protoc`（用于生成 aapt2 和 adb 的源码），
然后编译所有工具，并打包为 `android-sdk-tools-<static|shared>-<架构>.zip`。

```bash
# NDK（默认工具链），静态链接（默认）
python build.py --ndk=/path/to/android-ndk-xxx --arch=aarch64

# zig，静态 musl 可执行文件
python build.py --toolchain=zig --arch=aarch64

# 动态链接
python build.py --ndk=/path/to/android-ndk-xxx --arch=aarch64 --link=shared

# 本机编译器
python build.py --toolchain=host --arch=x86_64 --cc=clang --cxx=clang++

# 其他工具链，使用 CMake 工具链文件
python build.py --toolchain=custom --toolchain-file=/path/to/toolchain.cmake --arch=aarch64

# 使用已有的本机 protoc
python build.py --ndk=/path/to/android-ndk-xxx --arch=aarch64 --protoc=/path/to/protoc

# 只编译指定目标，便于定位错误
python build.py --ndk=/path/to/android-ndk-xxx --arch=aarch64 --target aapt2 adb

# 在 -- 之后传入额外的 cmake 参数
python build.py --toolchain=zig --arch=x86_64 -- -DSDK_STRIP=OFF

# 查看全部选项
python build.py --help
```

NDK 路径也可以通过环境变量 `ANDROID_NDK_HOME` / `ANDROID_NDK_ROOT` 指定。
编译目录为 `build/<工具链>-<链接方式>-<架构>`，可执行文件位于其中的 `bin` 目录。

### 直接使用 CMake

| 选项 | 默认值 | 说明 |
| --- | --- | --- |
| `SDK_STATIC` | `ON` | 完全静态链接 |
| `SDK_STRIP` | `ON` | 链接时去除符号（Release） |
| `PROTOC_PATH` | | 本机 protoc 路径 |
| `ZIG_TARGET` | | [`cmake/toolchains/zig.cmake`](cmake/toolchains/zig.cmake) 的目标，例如 `aarch64-linux-musl` |
| `ZIG_CPU` | | 可选，zig 的 `-mcpu`，例如 `cortex_a53` |

```bash
cmake -GNinja -B build/zig -DCMAKE_TOOLCHAIN_FILE=cmake/toolchains/zig.cmake \
    -DZIG_TARGET=aarch64-linux-musl -DPROTOC_PATH=/path/to/protoc
ninja -C build/zig
```

## 项目结构

```
├── CMakeLists.txt            顶层工程、第三方库
├── cmake/
│   ├── platform.cmake        静态链接、strip、系统库（与工具链无关）
│   ├── zlib.cmake            内置的静态 zlib
│   ├── modules/FindZLIB.cmake  将 find_package(ZLIB) 重定向到内置 zlib
│   └── toolchains/zig.cmake  zig 工具链文件
├── lib/                      AOSP 基础库（libbase、libutils、liblog 等）
├── build-tools/              aapt、aapt2、aidl 等
├── platform-tools/           adb、fastboot 等
├── others/                   veridex
├── patches/                  源码补丁和预生成文件
├── repository.json           源码仓库列表
├── get_source.py             获取源码
├── build.py                  编译与打包
├── Dockerfile                编译环境
└── .github/workflows/        CI
```

## GitHub Actions

[`.github/workflows/build.yml`](.github/workflows/build.yml) 在每次 push 时使用 NDK 编译所有架构
（手动运行时可以选择 zig），然后发布 release：

* tag 为 `PLATFORM_TOOLS_VERSION`，例如 `36.0.0`，标题为 `v36.0.0`
* release 描述为最新的 commit 信息（每次替换，不会追加）
* `PLATFORM_TOOLS_VERSION` 没有变化时不创建新的 release，
  而是覆盖上传已有 release 的文件，并把 tag 移动到最新的 commit
* 压缩包按架构区分：`android-sdk-tools-static-aarch64.zip`、`android-sdk-tools-static-arm.zip`、
  `android-sdk-tools-static-x86_64.zip`、`android-sdk-tools-static-i686.zip`

Pull request 只编译，不发布。

## 许可证

[Apache License 2.0](LICENSE.txt)
