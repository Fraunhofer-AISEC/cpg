struct myStruct {
  int field1;
};

void doSomething(int i) {}

int main() {
  struct myStruct s1;
  struct myStruct s2;

  s1.field1 = 1;
  s2.field1 = 1;

  doSomething(s1.field1);
  doSomething(s2.field1);
}