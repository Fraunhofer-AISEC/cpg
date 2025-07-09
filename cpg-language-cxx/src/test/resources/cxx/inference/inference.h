#include <numbers>

int somethingGlobal = 0;

namespace constants {
  double pi = std::numbers::pi;
}

namespace util {
  class SomeClass {
  public:
    void doSomething() {};
  };
}