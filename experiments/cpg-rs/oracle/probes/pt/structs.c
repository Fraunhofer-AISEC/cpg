struct I {
  int a;
};
struct O {
  struct I in;
  int z;
};

int h(int c) {
  struct O o;
  o.in.a = 1;
  o.z = 2;
  int u = o.in.a;
  struct O o2;
  o2 = o;
  int v = o2.z;
  int w = o2.in.a;
  return u + v + w;
}
