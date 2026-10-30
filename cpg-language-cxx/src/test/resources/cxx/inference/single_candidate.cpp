// undeclared_var, mystery_t and Bar are declared in a header that is not available, so we infer a
// struct for mystery_t and Bar, although mystery_t might as well be an integer typedef and Bar might
// derive from Foo or convert to it.
struct Foo {};

int grow(int *s, int needed) { return needed; }
int take(mystery_t m) { return 0; }
int take_foo(Foo &foo) { return 0; }
int take_pp(Foo **foo) { return 0; }
int with_default(int a, int b = 2) { return a + b; }
int vararg(const char *format, ...) { return 0; }
void log(const char *message) {}
int two(int a, int b) { return a + b; }

int caller(int *s, Bar *bar) {
    // An unknown type may be convertible, so the only candidate is the target
    int a = grow(s, undeclared_var + 1);
    // The same for a parameter of an unknown type
    int c = take(5);
    // The same for a class we do not know the definition of, also behind a pointer level both types
    // have, and with a default argument or variadic parameters
    int d = take_foo(*bar);
    int e = take_pp(&bar);
    int f = with_default(undeclared_var);
    int g = vararg(undeclared_var, 1, 2);
    // A double cannot be converted to a const char*, so this is a log we do not know, e.g. from
    // <cmath>
    log(3.0);
    // The number of arguments does not fit, so this is a two we do not know
    int b = two(1);
    return a + b + c + d + e + f + g;
}
