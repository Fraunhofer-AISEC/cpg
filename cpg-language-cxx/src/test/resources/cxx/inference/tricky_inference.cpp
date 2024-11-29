// The headers are just there to make it compile with clang, but we will not parse headers.
// You can use `clang++ -std=c++20 tricky_inference.cpp` to check, if it will compile.
#include "tricky_inference.h"

// We do not know "some::json", but this is a typedef to it.
// One could argue that "using some::json" might be more appropriate,
// but this is inspired by actual real-world code. But, this actually
// gives us the advantage, that we know that "json" is a type and not
// a namespace, so we need to use this information.
using json = some::json;

// Next, we are creating some kind of class that only uses our "json"
// object, but does not actually call any functions on it. We could
// probably conform at this point, that we are dealing with a record
// and could infer it (since we do not know it).
class wrapper {
public:
    json* get() {
        log("get");
        int i(j.size());
        return &j;
    }

private:
    json j;
};

// For some more complexity, let's refer to a sub-class of it
void loop(json* j) {
    log("loop");

    for(json::iterator* it = j->begin(); it != j->end(); it = it->next()) {
        if(!it->isValid()) {
            // do something
        }
    }
}

// And lastly, finally call a method on it, so we can know it's
// a class.
void* get_data(json* j) {
    log("get_data");
    return j->data;
}