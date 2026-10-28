#include <cstddef>
#include <cstdint>

void _mm_lfence();

class Victim {
public:
  unsigned int array1_size = 16;
  uint8_t array1[16];
  uint8_t array2[256 * 512];
  uint8_t temp = 0;

  void victim_function(size_t x) {
    if (x < array1_size) {
      temp &= array2[array1[x] * 512];
    }
  }

  void victim_function_split(size_t x) {
    if (x < array1_size) {
      uint8_t v = array1[x];
      temp &= array2[v * 512];
    }
  }

  void victim_function_fenced(size_t x) {
    if (x < array1_size) {
      _mm_lfence();
      temp &= array2[array1[x] * 512];
    }
  }

  void victim_function_alias(size_t x) {
    size_t *p = &x;
    if (*p < array1_size) {
      temp &= array2[array1[x] * 512];
    }
  }

  uint8_t safe_function(size_t x) {
    if (x < array1_size) {
      return array1[x];
    }
    return 0;
  }
};
