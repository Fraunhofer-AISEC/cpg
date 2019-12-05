package de.fraunhofer.aisec.cpgtest;

public class Switch {

  public static void main(String[] args) {
    int i = 5;
    System.out.println();
    switch (i) {
      case 0:
      case 1:
        i = 10;
        break;
      case 2:
        i = 20;
      case 3:
        i *= 2;
        break;
      default:
        i = 4;
    }
    System.out.println();
  }

  public void whileswitch(int i) {
    System.out.println();
    label:
    while (i < 10) {
      switch (i) {
        case 0:
          i += 2;
        case 9:
          break label;
        default:
          i++;
      }
      System.out.println();
    }
    System.out.println();
  }

  public void switchwhile(int i) {
    System.out.println();
    label:
    switch (i) {
      case 0:
        i += 2;
      case 1:
        while (true) {
          if (i > 5) break label;
          i++;
        }
        System.out.println();
      default:
        i++;
    }
    System.out.println();
  }
}
