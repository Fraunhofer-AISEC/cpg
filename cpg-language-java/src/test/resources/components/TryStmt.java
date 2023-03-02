import some.ImportedException;

class TryStmt {

  public static void main(String[] args) {
    try {
      System.out.println(Integer.parseInt(args[0]));
      throw new RuntimeException("Reached");
    } catch (NumberFormatException p) {
      System.out.println("NumberFormatException");
    } catch (NotResolvableTypeException o) {
      System.out.println("NotResolvableTypeException");
    } catch (ImportedException o) {
      System.out.println("ImportedException");
    } finally {
      System.out.println("Finished");
    }
  }
}
