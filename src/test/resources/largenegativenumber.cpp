int main() {
  int a = -1;                     // regular int
  int b = -2147483648;            // lowest int value, 2147483648 is too large too fit into int, so the literal itself is a long
  long c = -2147483649;           // long literal without explicit L notation
  long d = -9223372036854775808; // lowest long value, 9223372036854775808 is too large to fit into long, so the literal itself is an unsigned long
}
