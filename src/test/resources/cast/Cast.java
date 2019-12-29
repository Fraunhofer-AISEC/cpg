package cast;

public class Cast {
  public static void main(String[] args) {
    // these classes do not exist but we want to test parsing partial stuff
    ExtendedClass e = new ExtendedClass();
    BaseClass b = (BaseClass) e;
  }
}