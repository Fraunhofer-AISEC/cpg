public class Variables {
  private int field = 42;

  private int getField() {
    return field;
  }

  private int getLocal() {
    int local = 42;
    return local;
  }

  private int getShadow() {
    int field = 43;
    return field;
  }

  private int noShadow() {
    int field = 43;
    return this.field;
  }
}