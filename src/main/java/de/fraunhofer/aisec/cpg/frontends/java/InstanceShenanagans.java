package de.fraunhofer.aisec.cpg.frontends.java;

public class InstanceShenanagans {

  public static class SomeInnerObject {
    public String member = "member";
  }

  public static void main(String[] args) {
    SomeObject o = new SomeObject();
    SomeObject p = new SomeObject();

    // These two are not distinguished
    System.out.println(o.member + p.member);
    p = o;
    System.out.println(o.member + p.member);

    // These two are distinguided only by the code property but lead to the same access of member
    // variables
    // System.out.println(o.member + p.member);
    // System.out.println(o.member + o.member);
  }
}
