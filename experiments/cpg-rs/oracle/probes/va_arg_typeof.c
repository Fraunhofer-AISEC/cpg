// Looks like the expansion of __builtin_va_arg, but is written in the source: a dereference of
// a pointer cast, not a va_arg.
int deref(void *p) {
  int v = *(__typeof__(int) *)p;
  return v;
}
