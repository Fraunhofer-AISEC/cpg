// An argument whose type cannot be determined, because undeclared_var is declared in a header
// that is not available.
static int grow(int *s, int needed) { return needed; }
static int grow(int *s, long *needed) { return 1; }

int caller(int *s) {
    return grow(s, undeclared_var + 1);
}
