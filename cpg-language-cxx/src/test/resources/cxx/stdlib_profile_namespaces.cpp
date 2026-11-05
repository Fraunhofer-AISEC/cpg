// Functions that the code declares (without a body) with the names of stdlib functions, in its own
// namespace or as a method: they are different functions, so the stdlib profile does not apply.
namespace mylib {
void *memcpy(int &a, int &b);
}

struct Buf {
  unsigned long strlen(const char *s);
};

void own(int &a, int &b, Buf &buf, const char *s) {
  mylib::memcpy(a, b);
  buf.strlen(s);
}

// The stdlib functions: memcpy is declared in std, as <cstring> does, and strlen is inferred
// globally, since no header declares it.
namespace std {
void *memcpy(void *dest, const void *src, unsigned long n);
}

void stdlib(char *d, const char *s) {
  std::memcpy(d, s, 3);
  strlen(s);
}

// An unqualified call inside a namespace finds neither the code's own nor std's memcpy, so it infers
// a global memcpy, the C function.
namespace foo {
void unqualified(char *d, const char *s) { memcpy(d, s, 3); }
}
