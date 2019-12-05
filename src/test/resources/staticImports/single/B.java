import static A.test;

public class B {

  public static void main(String[] args) {
    test();
    int y = test; // single import can retrieve multiple things
  }
}