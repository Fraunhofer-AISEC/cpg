void *malloc(unsigned long size);

static int take_pointer(int *p) { return 0; }
static int take_pointer_pointer(int **p) { return 0; }

int caller(void *v) {
    int a = take_pointer(malloc(4));
    int b = take_pointer(((void *)0));
    int c = take_pointer(v);
    // int* is an object type, so void* converts to int** as well
    int d = take_pointer_pointer(malloc(32));
    return a + b + c + d;
}
