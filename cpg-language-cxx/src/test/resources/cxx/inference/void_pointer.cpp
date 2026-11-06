static int take_pointer(int *p) { return 0; }

int caller(void *v) {
    // Not valid C++, a void* needs an explicit cast to int*
    return take_pointer(v);
}
