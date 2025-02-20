// The headers are just there to make it compile with clang, but we will not parse headers.
// You can use `clang++ -std=c++20 tricky_inference.cpp` to check, if it will compile.
#include "superclass.h"

class F : A {

};

class G : N::B {

};

namespace O {
    class H : E {
        class I : M::C::D {
        };
    };

}
