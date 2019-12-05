import static A.*;

public class B {

  public static void main(String[] args) {
    a();
    b();
    b(true);
    nonStatic(); // needs to stay unresolved
    int y = staticField;
    int z = nonStaticField; // needs to stay unresolved
  }
}