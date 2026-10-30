// undeclared_var and mystery_t are declared in a header that is not available, so we infer a struct
// for mystery_t, although it might as well be an integer typedef.
int grow(int *s, int needed) { return needed; }
int take(mystery_t m) { return 0; }
void log(const char *message) {}
int two(int a, int b) { return a + b; }

int caller(int *s) {
    // An unknown type may be convertible, so the only candidate is the target
    int a = grow(s, undeclared_var + 1);
    // The same for a parameter of an unknown type
    int c = take(5);
    // A double cannot be converted to a const char*, so this is a log we do not know, e.g. from
    // <cmath>
    log(3.0);
    // The number of arguments does not fit, so this is a two we do not know
    int b = two(1);
    return a + b + c;
}
