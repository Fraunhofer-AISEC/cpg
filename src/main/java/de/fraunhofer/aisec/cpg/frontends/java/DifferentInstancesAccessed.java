package de.fraunhofer.aisec.cpg.frontends.java;

public class DifferentInstancesAccessed {

  public static class OtherClass {
    // The bug problem should not be restricted to inner classes we only need it to avoid another
    // bug that hides the
    // bahviour
    public String name = "name";
  }

  public static void main(String[] args) {
    OtherClass inst1 = new OtherClass();
    OtherClass inst2 = new OtherClass();
    String different = inst1.name + inst2.name;
    String same = inst1.name + inst1.name;
  }
}
