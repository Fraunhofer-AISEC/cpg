struct inner {
  int field;
};

struct outer {
  struct inner i;
};

void doSomething(int i) {}

int main() {
  struct outer o;
  o.i.field = 1;

  doSomething(o.i.field);
}