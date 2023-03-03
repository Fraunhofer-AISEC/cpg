import java.util.Arrays;
import java.util.List;

class ForEachStmt {
  public static void main(String[] args) {
    List<String> ls = Arrays.asList(args);
    for (String s : ls) System.out.println(s);
  }
}
