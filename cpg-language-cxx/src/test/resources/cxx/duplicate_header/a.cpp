#include "common.h"

int use_a() {
  Shared s;
  my_uint32 v = 0;
  return shared_helper(s.value) + (int)v;
}
