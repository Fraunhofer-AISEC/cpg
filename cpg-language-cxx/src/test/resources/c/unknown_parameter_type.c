// mystery_t is declared in a header that is not available, so we infer a struct for it, although
// it might as well be an integer typedef.
struct known { int x; };

static int take(mystery_t m) { return 0; }
static int take_pointer(mystery_t *m) { return 0; }
static int take_known(struct known *k) { return 0; }

int caller(mystery_t *p, int i, long *l) {
    int a = take(5);
    int b = take(*p);
    int c = take_pointer(&i);
    int d = take_pointer(p);
    // A long* is incompatible with a struct known*, but in C, the name alone determines the function
    int e = take_known(l);
    return a + b + c + d + e;
}
