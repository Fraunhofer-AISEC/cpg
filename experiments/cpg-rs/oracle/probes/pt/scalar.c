int f(int c) {
  int a = 1;
  int b = a;
  a = 2;
  if (c) {
    a = 3;
  }
  b = a;
  while (c) {
    a = a + 1;
    c--;
  }
  return a + b;
}
