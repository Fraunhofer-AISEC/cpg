public class ControlFlowSesitiveDFGSwitch {
  void func3() {
    int swithVal = 3;
    int a = 0;

    switch (swithVal) {
      case 1:
        a = 1;
        break;
      case 2:
        a = 2;
        break;
      case 3:
        a = 3;
        break;
      default:
        System.out.println(a);
        break;
    }

    int b = a;
  }
}
