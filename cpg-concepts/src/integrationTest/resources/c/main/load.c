#include <dlfcn.h>

int main() {
    void* lib = dlopen("libexample.so", RTLD_LAZY);

    int (*myfunc)(int);

    *(void**)(&myfunc) = dlsym(lib, "myfunc");
    int a = myfunc(1);

    dlclose(lib);
}
