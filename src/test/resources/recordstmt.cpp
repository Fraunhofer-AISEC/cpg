class SomeClass {
private:
  void* field = 0;

public:
  void* method();
  void* method(int a);

  void* inlineMethod() {
    return 0;
  }

  SomeClass() {

  }

  SomeClass(int a);
};

void* SomeClass::method() {
  return 0;
}

void* SomeClass::method(int a) {
  return 0;
}

SomeClass::SomeClass(int a) {

}

int main() {
  SomeClass s;
  s.method();
}
