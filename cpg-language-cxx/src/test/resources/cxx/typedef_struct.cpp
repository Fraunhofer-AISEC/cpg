typedef struct test {
  int a;
  int b;
} S;

int structs() {
  S s;
  S t;
  S* p=&s;
  s.a=1;
  s.b=2;
  printf("%d %d\n", s.a, s.b);
 }

 long typedef bla;