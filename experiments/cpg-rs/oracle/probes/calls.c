int f(int x);
int f(int x) { return x + 1; }
int c(void) {
  return f(1) + f(f(2));
}
int d(int *p, int n) {
  int t = 0;
  while (n--)
    t += *p++;
  if (!p || n < 0) return -1;
  else if (t > 10) return 1;
  return t ? t : 0;
}
