package cfg;

public class BreakContinue {

  public static void main(String[] args) {
    int i = 10;
    System.out.println();
    while (i > 0) {
      if (i < 8) continue;
      else if (i > 9) break;
      i--;
    }
    System.out.println();
    do {
      if (i > 9) break;
      if (i < 5) {
        i += 2;
        continue;
      }
      i++;
    } while (i < 10);
    System.out.println();
  }
}
