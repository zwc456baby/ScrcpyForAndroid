# android-sdk-tools 34.0.0（API 24 兼容构建）

本目录是基于 [lzhiyong/android-sdk-tools](https://github.com/lzhiyong/android-sdk-tools)
的构建脚本，用来从 AOSP `platform-tools-34.0.0` 源码编译 `adb`，目标是
**Android 7.0（API 24）**，与 `app/src/main/jniLibs` 里现有的 `libadb.so` 保持一致。

- 上游仓库：https://github.com/lzhiyong/android-sdk-tools
- 上游同步提交：`fe637bf075f1b734b4b01d281f9f58899245ed92`（2026-10-16，"code refactoring"）
- AOSP 源码 tag：`platform-tools-34.0.0`
- 构建版本：`34.0.0`
- 目标 API：`24`（Android 7.0）
- NDK：`r26d` = `26.3.11579264`
- C++ 标准：C++20
- 许可证：Apache License 2.0，见 [LICENSE.txt](LICENSE.txt)

对应的 GitHub Actions 工作流：`.github/workflows/build-adb-34.yml`。

## 为什么单独做一个 34.0.0 目录

现有 `app/src/main/jniLibs/*/libadb.so` 是从 **platform-tools 34.0.0 / API 24**
构建的（ELF note 为 24，二进制内包含 `termux-android-tools/src-android-24` 路径）。
35.0.2/36.0.0 的 `libbase` 已经依赖 API 30 的日志接口，无法直接按 API 24 编译，
所以这里单独保留一套 34.0.0 的构建树，给 Android 7~10 使用。

## 与上游 / 35.0.2 构建脚本的差异

这个目录不是上游脚本的简单副本，已经针对 34.0.0 + API 24 做了适配：

1. 删除了上游的 `.github/` 目录，增加本说明文件；
2. `patches/misc/platform_tools_version.h` 改为 `34.0.0`；
3. `repository.json` 中 `src/boringssl` 固定到 `platform-tools-35.0.2`：
   34.0.0 的 boringssl 没有 `CMakeLists.txt`，35.0.2 的 boringssl 才带 CMake 构建文件；
4. 只构建 adb：`CMakeLists.txt` 关闭 `build-tools` / `others`，
   `platform-tools/CMakeLists.txt` 只保留 `adb.cmake`；
5. 按 34.0.0 的源码结构修正库的源文件列表：
   - `lib/libbase.cmake` 去掉 34 不存在的 `result.cpp`；
   - `lib/libutils.cmake` 使用 34 的顶层文件（没有 `binder/` 子目录）；
   - `lib/libandroidfw.cmake` 使用 34 的 Android.bp 源文件列表；
6. 新增 API 24 兼容补丁：
   - `fdsan_compat.patch`：libbase/libcutils 的 fdsan 弱符号声明与回退；
   - `logging_api30.patch`：去掉 `android/log.h` 里 API 30 函数的可用性限制，
     这些函数由本仓库编译的静态 liblog 提供；
   - `properties_api26.patch`：为 `__system_property_read_callback` /
     `__system_property_wait` 增加 API 24 的声明；
   - `adb_api24.patch`：`adb_getlogin_r` 在 API < 28 时回退到 `getlogin()`；
   - `adb_pairing_api24.patch`：去掉 adb pairing 头文件里的 API 30 可用性限制；
   - `libandroidfw_34_compat.patch`：修正 NDK libc++ 不接受的不完整类型 /
     `std::vector<const T>` 写法；
   - `incfs_path_include.patch`：补 `<memory>`；
   - `task_runner.h.patch`：openscreen 的 `packaged_task` 兼容；
   - `boringssl_cxx17.patch`：35.0.2 的 boringssl CMake 使用 C++17；
   - `protobuf_CMakeLists.txt.patch`：protobuf 的 Android 构建修正；
7. 新增 `lib/ndk_property_compat.cpp`：在 API < 26 时实现
   `__system_property_read_callback` / `__system_property_wait`，链接进 adb；
8. 新增 `lib/libsepol_reallocarray.c` / `.h`：API < 26 没有 `reallocarray()`；
9. `lib/libziparchive.cmake` 增加 `-Wno-c++11-narrowing`；
   `lib/libbuildversion.cmake` 强制包含 `lib/ndk_property_compat.h`；
10. `patches/misc/IncrementalProperties.sysprop.cpp` 增加
    `__system_property_read_callback` 的 API 24 声明。

## 构建方式

在本目录下执行：

```bash
# 1. 拉取 34.0.0 源码并应用补丁
python3 get_source.py --tags platform-tools-34.0.0

# 2. 构建 host protoc
python3 build.py --protoc-only --protoc-output host-tools/protoc

# 3. 编译 adb（API 24，NDK r26d）
ANDROID_NDK_HOME=/path/to/android-ndk-r26d python3 build.py \
  --toolchain ndk --arch aarch64 --link shared --api 24 \
  --protoc host-tools/protoc --build build/aarch64 --target adb
# 产物：build/aarch64/bin/platform-tools/adb
```

架构对应关系：`arm64-v8a` → `aarch64`、`armeabi-v7a` → `arm`、
`x86` → `i686`、`x86_64` → `x86_64`。

## 注意

- 这个目录是为 API 24 定制的，**不要直接用上游 `platform-tools` 覆盖**，
  否则会丢失上面的 API 24 补丁和 34.0.0 源码列表适配。
- 如果上游更新了构建脚本，需要把新脚本与本目录的差异重新合并。
- 下载的 AOSP 源码（`src/`）、构建目录（`build/`）和 host 工具（`host-tools/`）
  都已在本目录的 `.gitignore` 中忽略，不会提交到本仓库。
