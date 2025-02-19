#include <dlfcn.h>

int main() {
    void* lib = dlopen("libexample.so", RTLD_LAZY);

    int (*b)(int);

    // does not work yet because of wrong DFG edges
    //*(void **) (&b) = dlsym(lib, "myfunc");
    // but the following works and is also a valid syntax
    b = dlsym(lib, "myfunc");
    int a = b(1);

    dlclose(lib);
}
