// In C, a file-scope `static` gives the variable/function internal linkage (TU-local). The
// non-static counterparts have external linkage.
static int internalGlobal = 1;
int exportedGlobal = 2;

static int internalFunction() { return internalGlobal; }

int exportedFunction() { return exportedGlobal; }

int useLocalStatic() {
    // Inside a function, `static` only affects storage duration, not linkage.
    static int localStatic = 0;
    return localStatic;
}
