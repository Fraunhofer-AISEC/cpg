int g1 = 1;
int g2 = g1 + 2;
int n(int a, int b) {
  int s = 0;
  for (int i = 0; i < a; i++) {
    for (int j = 0; j < b; j++) {
      if (j == 1) continue;
      if (i + j > 5) break;
      switch (j) {
        case 0: s++; break;
        case 1:
        case 2: s += 2; continue;
        default: s--;
      }
    }
  }
  do {
    s--;
    if (s == 7) continue;
    if (s == 9) break;
  } while (s > 0);
  return s > 0 && b || a;
}
