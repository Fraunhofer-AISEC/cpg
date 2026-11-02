// Arguments whose type cannot be determined, because undeclared_var and undeclared_fn are
// declared in a header that is not available.
static int grow(int *s, long needed) { return needed > *s; }

static int forward_declared(int x);

int caller(int *s, long n) {
    int a = grow(s, undeclared_var + n);
    int b = grow(s, undeclared_fn() + n);
    int c = forward_declared(undeclared_var);
    return a + b + c;
}

static int forward_declared(int x) { return x; }
