// undeclared_var is declared in a header that is not available. Calls to function templates and
// constructors are resolved before or apart from the fallback for unknown argument types.
template <class T> void tmpl(T t, int i) {}

struct Box {
    Box(int i) {}
};

int caller() {
    tmpl(undeclared_var, 1);
    Box b(undeclared_var);
    return 0;
}
