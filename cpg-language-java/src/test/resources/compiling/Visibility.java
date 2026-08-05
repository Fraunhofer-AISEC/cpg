package compiling;

interface SomeInterface {

  // implicitly public static final
  int CONSTANT = 1;

  // implicitly public
  void doSomething();

  // implicitly public (and static as an interface member type)
  class Nested {
    // package-private in an ordinary class
    int nestedField;
  }
}

class SomeClass {

  // package-private (no access modifier)
  int packageField;

  // package-private (no access modifier)
  void packageMethod() {}
}
