import java.util.Arrays;
import java.util.List;

class ForEachStmt {
  public static void main(String[] args) {
    List<String> ls = Arrays.asList(args);
    int i, j = 0;

    for (i = 1, j = 1; i < ls.size(); i++) {
      System.out.println(ls.get(i));
    }
  }
}
