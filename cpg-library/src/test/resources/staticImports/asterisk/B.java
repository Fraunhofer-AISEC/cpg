import static A.*;

public class B {

  public static void main(String[] args) {
    a();
    b();
    b(true);
    nonStatic(); // must not be resolved to A.nonStatic but rather an inferred node in B
    int y = staticField;
    int z = nonStaticField; // must not be resolved to A.nonStaticField but rather an inferred node in B
  }
}