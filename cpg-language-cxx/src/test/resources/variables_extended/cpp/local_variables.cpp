class Test {
  public:
  int call(int a) {
    return a + 1;
  }
};

/*
C++ 17, the variable can be declared by providing values through parenthesis too. The difference between
constructor initialization and the old normal way of initialization is that it will always return last
value in the parenthesis no matter what it’s magnitude or sign is.
In this example, foo will be assigned the value 42.
*/
int main() {
  int foo = 1;
  foo = (1,2,3,4,42);
  Test t;
  t.call(foo);
}

int testExpressionInExpressionList() {
  int x = 23;
  x = 42;
  int foo = 1;
  foo = (1,2,3,4,x);
  Test t;
  t.call(foo);
}