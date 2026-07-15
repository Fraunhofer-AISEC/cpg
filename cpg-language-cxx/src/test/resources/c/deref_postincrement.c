/*
 * Regression fixture for the ExpressionHandler unary-operator fix.
 *
 * Before the fix, `*_p++` collapsed into a PointerDereference whose *name* was
 * copied from the inner postfix-increment (whose name is "++"), tricking the
 * SymbolResolver into inferring a phantom global variable named "++". The idiom
 * originates in Apple's _stdio.h (`__sputc`), which is where the bug first
 * surfaced against the macOS system-header chain.
 */
int deref_postinc(int *_p, int _c) {
    return (*_p++ = _c);
}

int addr_of_local(void) {
    int x = 0;
    /* Simple `&x` — the outer PointerReference should still carry the name "x"
       so plain address-of on a Reference keeps working. */
    int *p = &x;
    return *p;
}
