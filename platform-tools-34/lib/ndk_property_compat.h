#pragma once

// API < 26 compatibility declaration.  The implementation lives in
// lib/ndk_property_compat.cpp and is linked into the adb executable.
struct prop_info;

#if __ANDROID_API__ < 26
extern "C" void __system_property_read_callback(
    const prop_info* pi,
    void (*callback)(void* cookie, const char* name, const char* value, unsigned serial),
    void* cookie);
#endif
