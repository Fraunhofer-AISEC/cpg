public class ControlFlowSensitiveDFGIfMerge {
  void func(int[] args) {
    int a = 1;

    if (args.length > 3) {
      a = 2;
    } else {
      System.out.println(a);
    }

    int b = a;
  }

  int bla;

  public static void main(String[] args) {
    ControlFlowSensitiveDFGIfMerge obj = new ControlFlowSensitiveDFGIfMerge();
    obj.bla = 3;
  }
}