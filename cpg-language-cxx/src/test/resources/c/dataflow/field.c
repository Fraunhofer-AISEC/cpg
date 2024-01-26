struct myStruct {
  int field1;
};

void doSomething(int i) {}

int main() {
  struct myStruct s;
  s.field1 = 1;

  doSomething(s.field1);
}