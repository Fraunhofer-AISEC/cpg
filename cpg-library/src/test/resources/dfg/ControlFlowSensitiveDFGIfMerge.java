public class ControlFlowSensitiveDFGIfMerge {
  void func() {
    int a = 1;
    if (args.length > 3) {
      a = 2;
    } else {
      System.out.println(a);
    }

    int b = a;
  }

}