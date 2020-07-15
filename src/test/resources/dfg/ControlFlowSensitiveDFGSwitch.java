public class ControlFlowSesitiveDFGSwitch {
  void func3() {
    int swithVal = 3;
    int a = 0;

    switch (swithVal) {
      case 1:
        a = 10;
        break;
      case 2:
        a = 11;
        break;
      case 3:
        a = 12; // Fall through
      default:
        System.out.println(a);
        break;
    }

    int b = a;
  }
}
