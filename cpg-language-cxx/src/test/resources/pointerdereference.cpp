int main() {
  int a;

  a++;
  --a;

  int len = sizeof(a);

  bool b = !false;

  int* ptr = 0;

  b = *ptr;

  int* c;
  *c = 7;
  *ptr = *c;
}
