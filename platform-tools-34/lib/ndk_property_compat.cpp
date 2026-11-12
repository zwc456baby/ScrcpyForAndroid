/*
 * API < 26 compatibility shims for building AOSP platform-tools with the NDK.
 *
 * __system_property_read_callback() and __system_property_wait() are hidden
 * from the NDK headers below API 26, but libbase/libcutils use them.  The
 * fallbacks below are built into the adb executable and use the old
 * __system_property_read()/__system_property_serial() interfaces.
 */
#include <stdint.h>
#include <time.h>
#include <unistd.h>

#include <sys/system_properties.h>

#if __ANDROID_API__ < 26

extern "C" {

// Not declared by the NDK headers, but exported by bionic since early releases.
uint32_t __system_property_area_serial();
uint32_t __system_property_serial(const prop_info* pi);

void __system_property_read_callback(
    const prop_info* pi,
    void (*callback)(void* cookie, const char* name, const char* value, uint32_t serial),
    void* cookie) {
  char name[PROP_NAME_MAX];
  char value[PROP_VALUE_MAX];
  if (__system_property_read(pi, name, value) >= 0) {
    callback(cookie, name, value, __system_property_serial(pi));
  }
}

bool __system_property_wait(const prop_info* pi, uint32_t old_serial, uint32_t* new_serial,
                            const struct timespec* relative_timeout) {
  struct timespec start;
  clock_gettime(CLOCK_MONOTONIC, &start);
  const int64_t timeout_ns =
      relative_timeout ? static_cast<int64_t>(relative_timeout->tv_sec) * 1000000000LL +
                             relative_timeout->tv_nsec
                       : -1;
  for (;;) {
    const uint32_t serial =
        pi ? __system_property_serial(pi) : __system_property_area_serial();
    if (serial != old_serial) {
      *new_serial = serial;
      return true;
    }
    if (timeout_ns >= 0) {
      struct timespec now;
      clock_gettime(CLOCK_MONOTONIC, &now);
      const int64_t elapsed_ns =
          static_cast<int64_t>(now.tv_sec - start.tv_sec) * 1000000000LL +
          (now.tv_nsec - start.tv_nsec);
      if (elapsed_ns >= timeout_ns) return false;
    }
    usleep(1000);
  }
}

}  // extern "C"

#endif  // __ANDROID_API__ < 26
