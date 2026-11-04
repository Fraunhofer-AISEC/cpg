int g(int x, int *p) {
  int a = x > 0 ? 1 : 2;
  int b = (x && a) || !x;
  a += 3;
  a <<= 1;
  x++;
  --x;
  int c = sizeof(int) + sizeof(x);
  int d = -x + ~a;
  int arr[3] = {1, 2, 3};
  int e = arr[1] + *p;
  p = &x;
  a = (b, c);
  return (int)a;
}
