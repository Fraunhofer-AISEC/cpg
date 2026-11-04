int m(int n) {
  int s = 0;
  for (int i = 0; i < n; i++) {
    if (i == 2) continue;
    s += i;
  }
  while (n) {
    n--;
    if (n == 3) break;
  }
  return s;
}
