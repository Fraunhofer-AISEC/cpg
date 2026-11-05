struct S { int x; struct S *next; };

int f(int *p, int a[], struct S *s, struct S t, int i) {
  int x = 0, y;
  a[1] = 3;
  a[i] += 1;
  *p = 4;
  *p += 2;
  s->x = 5;
  s->next->x = 6;
  t.x = 7;
  t.next->next = s;
  x = y = 2;
  (*p)++;
  ++i;
  i--;
  *(p + 1) = 9;
  x = (int)i;
  *(int *)p = 8;
  return x + a[0];
}
