// The headers are just there to make it compile with clang, but we will not parse headers.
// You can use `clang++ -std=c++20 inference.cpp` to check, if it will compile.
#include "inference.h"

// To make it a little bit easier for our inference engine, we forward declare "constants" as a
// namespace, because otherwise we would not know whether this is a class or a namespace.
namespace constants {};

int doSomething() {
  if(somethingGlobal == 4) {
    return 1;
  }

  double a = constants::pi;

  return 0;
}

int main() {
  doSomething();
}