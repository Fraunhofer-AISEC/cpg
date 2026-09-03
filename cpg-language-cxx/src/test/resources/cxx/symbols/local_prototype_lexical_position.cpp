void m() {
    // f is not declared yet at this point - this must NOT resolve to the local prototype below,
    // since a locally-declared function prototype is only in scope from its own declaration point
    // onward, just like an ordinary local variable.
    f(1);
    void f(int);
    // Now f is in scope.
    f(2);
}
