package de.fraunhofer.aisec.cpg.frontends.java;

public class StaticContextThis {

  public String instanceMember = "";

  public static void main(String[] args) {
    String local = new StaticContextThis().instanceMember;
  }
}
