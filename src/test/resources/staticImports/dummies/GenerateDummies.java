import static a.b.c.SomeClass.someMethod;

public class GenerateDummies {
  public static void main(String[] args) {
    someMethod("hi"); // needs to map to the same method as the first one
    someMethod("many", "many", "words", 123);
    someMethod("second"); // same method as the first one
  }
}