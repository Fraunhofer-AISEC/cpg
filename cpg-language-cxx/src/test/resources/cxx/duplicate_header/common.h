#ifndef COMMON_H
#define COMMON_H

typedef unsigned int my_uint32;

struct Shared {
  int value;
  int get() const;
};

inline int shared_helper(int x) { return x + 1; }

#endif
