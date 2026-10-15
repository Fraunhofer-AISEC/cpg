// A file-scope `static` gives the variable/function internal linkage (TU-local), *not* a static
// member. Their non-static counterparts have external linkage.
static int internalGlobal = 1;
int exportedGlobal = 2;

static int internalFunction() { return internalGlobal; }
int exportedFunction() { return exportedGlobal; }

class MyClass {
  public:
    // Here `static` means a class-level member, bound to the class rather than to an instance.
    static int staticField;
    int instanceField;

    static int staticMethod();
    int instanceMethod();
};

// Out-of-line definition of the static data member (no `static` keyword allowed here).
int MyClass::staticField = 0;

int useLocalStatic() {
    // Inside a function, `static` only affects storage duration, which is irrelevant to
    // resolution: this is neither internal linkage nor a static member.
    static int localStatic = 0;
    return localStatic;
}
