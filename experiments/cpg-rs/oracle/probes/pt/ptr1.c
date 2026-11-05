int f(int c) {
  int x = 1;
  int *p = &x;
  *p = 2;
  int y = *p;
  int z = x;
  return y + z;
}
