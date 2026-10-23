int global_result = 0;

void f() {
    // Local is not declared yet at this point - this qualified reference must NOT resolve to the
    // local class below, since it is only in scope from its own declaration point onward, just
    // like the unqualified case.
    global_result = Local::bar();
    struct Local {
        static int bar() { return 1; }
    };
    // Now Local is in scope.
    global_result = Local::bar();
}
