import static A.*;

public class B {

  public static void main(String[] args) {
    a();
    b();
    b(true);
    nonStatic(); // must not be resolved to A.nonStatic but rather a dummy in B
    int y = staticField;
    int z = nonStaticField; // must not be resolved to A.nonStaticField but rather a dummy in B
  }
}