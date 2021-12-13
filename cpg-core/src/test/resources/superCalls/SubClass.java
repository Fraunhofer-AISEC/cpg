public class SubClass extends SuperClass implements Interface1, Interface2 {
  public int field;
  @Override
  public void target() {
    super.target();  // SuperClass.target()
    Interface1.super.target();  // Interface1.target()
    Interface2.super.target();  // Interface2.target()
  }

  public int getField() {
    return field;
  }

  public int getSuperField() {
    return super.field;  // SuperClass.field
  }

  private class Inner {
    public void inner() {
      SubClass.super.target();  // SuperClass.target()
    }
  }
}