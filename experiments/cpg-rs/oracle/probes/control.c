int f(int a, int b) {
  int r = 0;
  if (a > b) { r = 1; } else { r = 2; }
  if (a) r++;
  while (a < 10) {
    a++;
    if (a == 5) continue;
    if (a == 8) break;
  }
  do { b--; } while (b > 0);
  for (int i = 0; i < 3; i++) { r += i; }
  for (;;) { break; }
  switch (a) {
    case 1: r = 1; break;
    case 2: r = 2;
    default: r = 3;
  }
  goto end;
  r = 99;
end:
  return r;
}
