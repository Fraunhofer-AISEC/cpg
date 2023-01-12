class MyClass {
  public:
    void target() {}
    void target(int param) {}
};

int main() {
  MyClass my;

  // declares a variable called no_param with a function pointer type and
  // assigns MyClass::target (without parameters)
  void (MyClass::*no_param) () = &MyClass::target;

  // declares a variable called single_param with a function pointer type and
  // assigns MyClass::target (with parameters)
  void (MyClass::*single_param) (int) = &MyClass::target;

  // calls the function pointer in no_param on the object my,
  // calling MyClass::target (without parameters)
  (my.*no_param)();

  // calls the function pointer in single_param on the object my,
  // calling MyClass::target (with parameters)
  (my.*single_param)(42);
}