static int take_pointer(int *p) { return 0; }
static int take_other_pointer(int *p) { return 0; }

int caller(void) {
    int a = take_pointer(0);
    int b = take_pointer(0L);
    int c = take_pointer(0U);
    int d = take_pointer(0UL);
    // Not null pointer constants, so they cannot be converted to a pointer. In C, the name alone
    // determines the function, so the calls still go to take_other_pointer.
    int e = take_other_pointer(1);
    int f = take_other_pointer(0.0);
    return a + b + c + d + e + f;
}
