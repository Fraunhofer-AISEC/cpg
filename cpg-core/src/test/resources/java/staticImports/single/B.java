package single;

import static single.A.test;
import single.A.C;

public class B {

  public static void main(String[] args) {
    test();
    int y = test; // single import can retrieve multiple things

    A.C c;
  }
}