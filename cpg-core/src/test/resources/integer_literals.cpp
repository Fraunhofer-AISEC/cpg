void zero() {
  int i = 0;
  long l_with_suffix = 0l;
  long long l_long_long_with_suffix = 0ll;
  unsigned long long l_unsigned_long_long_with_suffix = 0ull;
}

void decimal() {
  int i = 42;
  int i_with_literal = 1'000;
  long l = 9223372036854775807; // still fits into long
  long l_with_suffix = 9223372036854775807L;
  long long l_long_long_with_suffix = 9223372036854775807LL;
  unsigned long l_unsigned_long_with_suffix = 9223372036854775809ul;
  unsigned long long l_long_long_implicit = 9223372036854775808; // too large for signed, so implicitly converted
  unsigned long long l_unsigned_long_long_with_suffix = 9223372036854775809ull;
}

void octal() {
  int i = 052;
  long l_with_suffix = 052L;
  unsigned long long l_unsigned_long_long_with_suffix = 052ull;
}

void hex() {
  int i = 0x2a;
  long l_with_suffix = 0x2aL;
  unsigned long long l_unsigned_long_long_with_suffix = 0x2aull;
}

void binary() {
  int i = 0b101010;
  long l_with_suffix = 0b101010L;
  unsigned long long l_unsigned_long_long_with_suffix = 0b101010ull;
}
