static int take_pointer(int *p) { return 0; }

int caller(void) {
    int a = take_pointer(0);
    int b = take_pointer(0L);
    // Not a null pointer constant
    int c = take_pointer(1);
    return a + b + c;
}
