// In C, the name alone determines the function, also if the number of arguments does not fit.
// old_style has no prototype, so a call can pass any arguments.
int old_style();
static int two(int a, int b) { return a + b; }

int caller(void) {
    int a = old_style(1, 2);
    int b = two(1);
    return a + b;
}
