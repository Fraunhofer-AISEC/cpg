public class ContextSensitiveDFG {

  void test(){
  }

  void func() {
    test();
    int a = 1;
    if (args.length > 3) {
      a = 2;
    } else {
      System.out.println(a);
    }

    int b = a;

  }
}
