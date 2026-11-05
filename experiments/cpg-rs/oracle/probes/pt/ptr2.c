int g(int *q) {
  int a = *q;
  *q = a + 1;
  return *q;
}
