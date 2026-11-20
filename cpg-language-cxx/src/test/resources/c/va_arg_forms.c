typedef long long i64;

double forms(void *ap) {
  *va_arg(ap, int *) = 1;
  i64 big = va_arg(ap, i64);
  unsigned long n = va_arg(ap, unsigned long);
  double d = va_arg(ap, double);
  return d;
}
