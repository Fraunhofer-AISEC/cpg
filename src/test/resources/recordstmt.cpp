class SomeClass {
private:
  void* field = 0;

public:
  void* method();

  void* inlineMethod() {
    return 0;
  }

  SomeClass() {

  }
};

void* SomeClass::method() {
  return 0;
}

int main() {
  SomeClass s;
  s.method();
}
