package compiling;

public class FieldAccess {

  private int[] array;

  public static void main(String[] args) {
    FieldAccess f = new FieldAccess();
    int l = f.array.length;
  }

}