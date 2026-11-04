struct S { int x; };
int glob = 5;
static int h(struct S *s, int n) {
  s->x = n;
  return s->x + glob;
}
void k(void) {
  struct S s;
  s.x = 1;
  int r = h(&s, 2);
  do { r--; } while (0);
  r = r ? h(&s, 1) : 0;
}
