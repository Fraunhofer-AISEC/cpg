public class ExceptionTest {
  public static void main(String[] args) {
    try {
        throw new IllegalArgumentException();
    } catch(Exception e) {
        System.out.println("Oops");
    }
  }
}
