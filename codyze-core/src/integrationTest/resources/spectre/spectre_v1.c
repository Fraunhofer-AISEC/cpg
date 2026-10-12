#include <stddef.h>
#include <stdint.h>

unsigned int array1_size = 16;
uint8_t array1[16];
uint8_t array2[256 * 512];
uint8_t temp = 0;

void _mm_lfence(void);

// Classic Spectre v1 gadget (Kocher et al.): bounds check on attacker-controlled
// x, speculative out-of-bounds load array1[x], secret-dependent second load.
void victim_function(size_t x) {
  if (x < array1_size) {
    temp &= array2[array1[x] * 512];
  }
}

// Same gadget, but split over a local variable.
void victim_function_split(size_t x) {
  if (x < array1_size) {
    uint8_t v = array1[x];
    temp &= array2[v * 512];
  }
}

// Mitigated: speculation barrier between the bounds check and the load.
void victim_function_fenced(size_t x) {
  if (x < array1_size) {
    _mm_lfence();
    temp &= array2[array1[x] * 512];
  }
}

// Same gadget, but the bounds check reads the index through an alias.
void victim_function_alias(size_t x) {
  size_t *p = &x;
  if (*p < array1_size) {
    temp &= array2[array1[x] * 512];
  }
}

// Not a gadget: bounds-checked load, but no secret-dependent second access.
uint8_t safe_function(size_t x) {
  if (x < array1_size) {
    return array1[x];
  }
  return 0;
}
