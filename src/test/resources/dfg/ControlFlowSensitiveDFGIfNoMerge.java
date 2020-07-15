public class ControlFlowSensitiveDFGIfNoMerge {
  void func2() {
    int a = 1;
    if (args.length > 3) {
      a = 2;
    } else {
      a = 3;
      int b = a;
    }
  }
}
