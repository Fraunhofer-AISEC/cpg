int next(void *ap) {
  int v = va_arg(ap, int);
  char *s = va_arg(ap, char *);
  return v;
}
