void *malloc(unsigned long size);

static int take_pointer(int *p) { return 0; }

int caller(void *v) {
    int a = take_pointer(malloc(4));
    int b = take_pointer(((void *)0));
    int c = take_pointer(v);
    return a + b + c;
}
