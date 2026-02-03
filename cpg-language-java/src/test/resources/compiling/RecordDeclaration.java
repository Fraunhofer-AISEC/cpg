package compiling;

class SimpleClass {

  private int field;

  private int field1 = 1, field2 = 2;

  SimpleClass() {
    // constructor
  }

  Integer method() {
    System.out.println("Hello world");
    int x = 0;
    if (System.currentTimeMillis() > 0) {
      x = x + 1;
    } else {
      x = x -1;
    }
    return x;
  }
}
