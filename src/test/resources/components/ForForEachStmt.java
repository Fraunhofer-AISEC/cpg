import java.util.Arrays;
import java.util.List;

class ForForEachStmt {

  public static void main(String[] args) {
    List<String> ls = Arrays.asList(args);
    for (int i = 0; i < args.length; i++) System.out.println(args[i]);
    for (String s : ls) System.out.println(s);
  }
}
