/*
 * API < 26 compatibility shim: bionic does not provide reallocarray().
 * libsepol uses it unconditionally, so provide the standard implementation.
 */
#include <errno.h>
#include <stdint.h>
#include <stdlib.h>

#include "libsepol_reallocarray.h"

#if __ANDROID_API__ < 26
void* reallocarray(void* ptr, size_t nmemb, size_t size) {
  if (size != 0 && nmemb > SIZE_MAX / size) {
    errno = ENOMEM;
    return NULL;
  }
  return realloc(ptr, nmemb * size);
}
#endif
