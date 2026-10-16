#include "common.h"

int use_b() {
  Shared s;
  my_uint32 v = 1;
  return shared_helper(s.value) + (int)v;
}
