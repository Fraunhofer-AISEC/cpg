static int take_pointer(int *p) { return 0; }

static int overloaded(int i) { return 1; }
static int overloaded(int *p) { return 2; }

int caller() {
    int a = take_pointer(0);
    // An exact match for int wins over the null pointer conversion
    int b = overloaded(0);
    return a + b;
}
