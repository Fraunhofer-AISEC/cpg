package de.fraunhofer.aisec.cpg.frontends.java;

public class ClassReferences {

  public static String staticString = "staticLiteral";
  public String instanceString = "instanceLiteral";

  public static void staticFunction() {}

  public void instanceFunction() {}

  public static void main(String[] args) {
    Class<ClassReferences> classReference = ClassReferences.class;
    ClassReferences instanceReference = new ClassReferences();

    classReference.getSimpleName();
    String sInstance = instanceReference.instanceString;
    String sClass = ClassReferences.staticString;
    instanceReference.instanceFunction();
    ClassReferences.staticFunction();
  }
}
