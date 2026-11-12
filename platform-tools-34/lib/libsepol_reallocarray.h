#pragma once

#include <stddef.h>

#if __ANDROID_API__ < 26
void* reallocarray(void* ptr, size_t nmemb, size_t size);
#endif
