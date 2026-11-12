# Build environment of android-sdk-tools: NDK + zig + cmake/ninja/bison/flex/go.
#
# The image only contains the toolchains, the project is mounted at /work:
#
#   docker build --platform linux/amd64 -t android-sdk-tools .
#   docker run --rm -it --platform linux/amd64 -u "$(id -u):$(id -g)" -v "$PWD:/work" android-sdk-tools \
#       sh -c "python3 get_source.py && python3 build.py --arch aarch64"
#
# Build arguments:
#   NDK_VERSION  android ndk version, empty to skip the NDK   (default r27c)
#   ZIG_VERSION  zig version, empty to skip zig               (default 0.13.0)
#
# The NDK only provides linux-x86_64 binaries, so build the image for linux/amd64.

FROM ubuntu:24.04

ARG NDK_VERSION=r29
ARG ZIG_VERSION=0.16.0

ENV DEBIAN_FRONTEND=noninteractive \
    LANG=C.UTF-8

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        bison \
        build-essential \
        ca-certificates \
        clang \
        cmake \
        curl \
        file \
        flex \
        git \
        golang-go \
        ninja-build \
        patch \
        python3 \
        unzip \
        xz-utils \
        zip \
    && rm -rf /var/lib/apt/lists/*

# android ndk
RUN if [ -n "$NDK_VERSION" ]; then \
        curl -fL --retry 3 -o /tmp/ndk.zip \
            "https://dl.google.com/android/repository/android-ndk-${NDK_VERSION}-linux.zip" \
        && unzip -q /tmp/ndk.zip -d /opt \
        && mv "/opt/android-ndk-${NDK_VERSION}" /opt/android-ndk \
        && rm -f /tmp/ndk.zip; \
    fi

# zig
RUN if [ -n "$ZIG_VERSION" ]; then \
        curl -fL --retry 3 -o /tmp/zig.tar.xz \
            "https://ziglang.org/download/${ZIG_VERSION}/zig-linux-x86_64-${ZIG_VERSION}.tar.xz" \
        && mkdir -p /opt/zig \
        && tar -xJf /tmp/zig.tar.xz -C /opt/zig --strip-components=1 \
        && ln -s /opt/zig/zig /usr/local/bin/zig \
        && rm -f /tmp/zig.tar.xz; \
    fi

# HOME and the caches point to /tmp, so they're writable with `-u $(id -u):$(id -g)`
ENV ANDROID_NDK_HOME=/opt/android-ndk \
    ANDROID_NDK_ROOT=/opt/android-ndk \
    CMAKE_POLICY_VERSION_MINIMUM=3.5 \
    HOME=/tmp \
    ZIG_GLOBAL_CACHE_DIR=/tmp/zig-cache \
    GOCACHE=/tmp/go-cache

# the mounted project may belong to another user
RUN git config --system --add safe.directory '*'

WORKDIR /work
CMD ["bash"]
