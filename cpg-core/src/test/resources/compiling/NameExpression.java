package compiling;
class NameExpression {
  private static String TEST = "test";

  private static String TEST2 = NameExpression.TEST;

  public NameExpression() {
    String test3 = NameExpression.TEST2;
  }
}