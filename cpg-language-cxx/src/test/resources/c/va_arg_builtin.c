int next(void *ap) {
  int v = __builtin_va_arg(ap, int);
  char *s = __builtin_va_arg(ap, char *);
  return v;
}
