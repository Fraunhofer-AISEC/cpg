class SomeClass {
private:
  void* field = 0;

public:
  void* method();

  // cannot parse inline correctly (issue #4)
  //inline void* inlineMethod() {
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
