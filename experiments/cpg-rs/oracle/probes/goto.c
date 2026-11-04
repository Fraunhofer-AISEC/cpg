int a(int x) {
  if (x) goto done;
  if (x > 1) goto done;
  x = 3;
done:
  return x;
}
int b(int n) {
again:
  n--;
  if (n > 0) goto again;
  return n;
}
