class Test {
public:
  int call(int a) {
    return a + 1;
  }
};

int main() {
  int foo = 42;
  foo = 3;
  Test t;
  t.call(foo);

  // note, this is not a construct expression, but a function declaration.
  // however this is still just a regular declaration statement and the EOG should continue beyond it
  Test t2();
}