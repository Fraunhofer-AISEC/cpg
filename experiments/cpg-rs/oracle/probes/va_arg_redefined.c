// What <stdarg.h> of GCC and Clang does: it defines the same macro as the stdlib profile.
#define va_arg(ap, type) __builtin_va_arg(ap, type)

int next(void *ap) {
  int v = va_arg(ap, int);
  char *s = va_arg(ap, char *);
  return v;
}
