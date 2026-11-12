# android-sdk-tools（内置副本）

本目录是 [lzhiyong/android-sdk-tools](https://github.com/lzhiyong/android-sdk-tools) 的完整副本，
用于在本仓库的 GitHub Actions 中交叉编译 `adb`（输出为 `libadb.so`）。

## 为什么是内置副本，而不是 submodule / 动态引用

上游仓库曾经用 `git push --force` 改写历史，导致本仓库 submodule 中记录的那次提交
（`5071328`）在上游彻底消失，`actions/checkout` 拉取 submodule 时直接失败，
打包工作流也随之失效。

为了不再受上游改写历史的影响，这里把构建所需的文件整套复制进本仓库：
打包时只读取本地文件，不再访问上游仓库，因此上游无论怎么 force-push、
删除分支或重打 tag，都不会影响本仓库的构建。

- 上游仓库：https://github.com/lzhiyong/android-sdk-tools
- 同步来源提交：`fe637bf075f1b734b4b01d281f9f58899245ed92`（2026-10-16，"code refactoring"）
- 构建版本：`platform-tools 36.0.0`（对应 AOSP `android-16.0.0_r1`）
- 许可证：Apache License 2.0，见 [LICENSE.txt](LICENSE.txt)

> 上游内置的 `patches/misc/platform_tools_version.h` 仍是 `35.0.2`，本仓库已改为 `36.0.0`。
> AOSP 各仓库并没有 `platform-tools-36.0.0` 这个 tag，所以工作流用
> `get_source.py --tags android-16.0.0_r1` 取 Android 16 的源码
> （`android-16.0.0_r1` 的 `sdk/plat_tools_source.prop_template` 中 `Pkg.Revision=36.0.0`）。

## 与上游的差异

只做了少量与上游代码无冲突的调整，其余文件保持原样：

1. 删除了上游的 `.github/` 目录（避免把上游的工作流带进本仓库）；
2. 增加了本说明文件；
3. `patches/misc/platform_tools_version.h` 由 `35.0.2` 改为 `36.0.0`；
4. `lib/libutils.cmake` 补上了 `${SRC}/core/libsystem/include`；
5. `platform-tools/adb.cmake` 按 36.0.0 的源码列表做了同步：
   - 新增 `apacket_reader.cpp`、`client/detach.cpp`、`client/usb_libusb_device.cpp`、
     `client/usb_libusb_hotplug.cpp`、`client/usb_libusb_inhouse_hotplug.cpp`、
     `client/transport_emulator.cpp`；
   - 移除 36.0.0 中已删除的 `client/fastdeploycallbacks.cpp`、
     `client/openscreen/mdns_service_watcher.cpp`、`client/transport_local.cpp`；
   - `libusb` 头文件目录由不存在的 `${SRC}/libusb/include` 改为 `${SRC}/libusb`，
     36.0.0 的头文件是 `<libusb/libusb.h>`（位于 `${SRC}/libusb/libusb/libusb.h`）；
   - `adb` 目标补上 `${SRC}/openscreen`、`${SRC}/abseil-cpp`、`${SRC}/libusb` 头文件目录；
   - 用 `set_target_properties(adb ... RUNTIME_OUTPUT_DIRECTORY .../bin/platform-tools)`
     固定输出路径（jsoncpp 会把 `CMAKE_RUNTIME_OUTPUT_DIRECTORY` 写成全局 cache，
     否则 adb 会跑到 `bin/adb`）；
6. `lib/CMakeLists.txt` 不再配置 `libprocessgroup`：只有 adbd 需要它，adb 客户端不链接，
   且 36.0.0 中它的目录结构发生了变化；`lib/libandroidfw.cmake` 补上了 36.0.0 新增的
   `LocaleDataLookup.cpp`（`getMaxAncestorTreeDepth()` 的定义在这里）；
7. 新增 `patches/boringssl_cxx17.patch`：36.0.0 的 boringssl 使用了 C++17 的
   `std::is_convertible_v` 等特性，但它自动生成的 CMakeLists 仍强制 C++14，
   这个补丁把它改为 C++17；
8. 新增 `patches/ndk_system_property_serial.patch`：bionic 导出了
   `__system_property_area_serial()` / `__system_property_serial()`，但 NDK 头文件
   没有声明，`libbase`、`liblog`、`libcutils` 都会用到它们，补丁在三个文件里补上声明；
9. 新增 `patches/liblog_ndk_compat.patch`：`liblog/logger_write.cpp` 用到了 API 37 才有的
   `__android_log_logd_logger_with_timestamp()`（该函数由本仓库自己编译的 liblog 提供），
   补丁去掉它头文件里的 `__INTRODUCED_IN(37)`，以便用 API 30 编译；
10. 新增 `patches/combined_iterator_swap.patch`：`libandroidfw` 的 `CombinedIterator`
    代理引用（`RefPair`）在 NDK 的 libc++ 下无法被 `std::inplace_merge` 交换，
    补丁为 `RefPair` 增加右值 `swap` 重载；
11. 新增 `patches/locale_data_constexpr.patch`：`LocaleDataLookup.h` 的 `packLocale` /
    `packScript` 在 constexpr 函数里用了 `reinterpret_cast`，新版 clang 会直接报错，
    补丁去掉这两个函数的 `constexpr`（它们本来就无法用于常量表达式）；
12. `CMakeLists.txt` 的 `CMAKE_CXX_STANDARD` 由 20 改为 23：36.0.0 用到了
    `std::string::resize_and_overwrite` 等 C++23 特性，因此 NDK 也必须用 r29
    （r27c 的 libc++ 不支持这些特性）；
13. 新增 `patches/incfs_path_include.patch`：`incfs/include/path.h` 使用了
    `std::unique_ptr` 却没有包含 `<memory>`，新版 libc++ 不再间接包含它；
14. 更新了上游的 `patches/task_runner.h.patch`：36.0.0 的 openscreen 文件少了空行，
    旧补丁的上下文对不上，无法应用；新补丁直接把 `Task` 固定为
    `std::packaged_task<void()>`（NDK 的 libc++ 不提供 `noexcept` 版本）。

第 4 点是 `platform-tools` 35/36 的适配：这些版本把 `<system/graphics.h>`、
`<system/thread_defs.h>` 移到了 `core/libsystem/include`，而
`core/libutils/include/utils/ThreadDefs.h` 仍然直接包含它们，不加这个头文件目录
会编译失败。上游更新后如果已经自行修复，可以直接采用上游版本。

## 如何更新

上游更新后，不要用 submodule，直接把新版本的文件覆盖到本目录即可：

```bash
git clone --depth 1 https://github.com/lzhiyong/android-sdk-tools /tmp/android-sdk-tools
rsync -a --delete \
  --exclude '.git/' --exclude '.github/' \
  --exclude 'VENDOR.md' \
  --exclude 'boringssl_cxx17.patch' \
  --exclude 'liblog_ndk_compat.patch' \
  --exclude 'ndk_system_property_serial.patch' \
  --exclude 'combined_iterator_swap.patch' \
  --exclude 'locale_data_constexpr.patch' \
  --exclude 'incfs_path_include.patch' \
  /tmp/android-sdk-tools/ platform-tools/
```

`rsync --delete` 不会删除本说明文件和本仓库新增的补丁。
覆盖后记得检查 `lib/libutils.cmake`（第 4 点）、`platform-tools/adb.cmake`（第 5 点）、
`lib/CMakeLists.txt`（第 6 点）、`patches/` 下本仓库新增或修改的补丁（第 7-11、13、14 点）、
`CMakeLists.txt` 的 C++23（第 12 点）是否还需要本地改动，然后重新执行一次
`.github/workflows/build-adb.yml`，确认 4 个架构都能编译通过。

如果上游改动了 `get_source.py` / `build.py` 的参数，需要同步调整该工作流。

## 构建方式（工作流中执行的命令）

所有命令都在本目录下执行：

```bash
# 1. 克隆 Android 16 的 AOSP 源码并打补丁
#    （AOSP 没有 platform-tools-36.0.0 tag，不能省略 --tags）
python3 get_source.py --tags android-16.0.0_r1

# 2. 构建 host 版 protoc（只做一次，4 个架构共用）
python3 build.py --protoc-only --protoc-output host-tools/protoc

# 3. 交叉编译 adb（36.0.0 的 libbase 依赖 API 29+ 的 fdsan，这里用 API 30）
#    NDK 必须用 r29（libc++ 21 才支持 36.0.0 需要的 C++23 特性）
ANDROID_NDK_HOME=/path/to/android-ndk-r29 python3 build.py \
  --toolchain ndk --arch aarch64 --link shared --api 30 \
  --protoc host-tools/protoc --build build/aarch64 --target adb
# 产物：build/aarch64/bin/platform-tools/adb
```

架构对应关系：`arm64-v8a` → `aarch64`、`armeabi-v7a` → `arm`、
`x86` → `i686`、`x86_64` → `x86_64`。

> 产物是按 **API 30** 编译的，运行时要求 Android 11（API 30）及以上。
> 如果要支持更低的 Android 版本，需要另行为 libbase/liblog 等补 fdsan 兼容层，
> 不能只改 `--api`。

下载的 AOSP 源码（`src/`）、构建目录（`build/`）和 host 工具（`host-tools/`）
都已在本目录的 `.gitignore` 中忽略，不会提交到本仓库。
