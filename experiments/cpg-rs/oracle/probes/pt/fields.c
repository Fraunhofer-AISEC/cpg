struct P {
  int x;
  int y;
};

int g(int c) {
  struct P p;
  p.x = 1;
  p.y = 2;
  int a = p.x;
  if (c) {
    p.x = 3;
  }
  int b = p.x;
  int d = p.y;
  return a + b + d;
}
