// To reproduce this example, run the following commands from the parent directory:
// clang -shared libexample/lib.c -o libexample.so
// clang main/load.c -o load
// ./load
// Expected output: a = 3

#include <dlfcn.h>
#include <stdio.h>

int main() {
    void* lib = dlopen("libexample.so", RTLD_LAZY);

    int (*b)(int);
    int *c;

    // does not work yet because of wrong DFG edges
    //*(void **) (&b) = dlsym(lib, "myfunc");
    // but the following works and is also a valid syntax
    b = dlsym(lib, "myfunc");
    c = dlsym(lib, "myvar"); // c = 2

    int a = b(*c);

    // a = 3
    printf("a = %d\n", a);

    dlclose(lib);
}
