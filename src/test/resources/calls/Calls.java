public class Calls extends SuperClass {

  private void innerTarget() {}
  private void innerTarget(int param1, int param2) {}
  private void innerTarget(int param1, String param2) {}

  public static void main(String[] args) {
    innerTarget();
    innerTarget(1, 2);
    innerTarget(1, "2");
    // dummy
    innerTarget(1, 2, 3);

    superTarget();
    superTarget(1, 2);
    superTarget(1, "2");
    // dummy
    superTarget(1, 2, 3);

    External e = new External();
    e.externalTarget();
    e.externalTarget(1, 2);
    e.externalTarget(1, "2");
    // dummy
    e.externalTarget(1, 2, 3);

    e.superTarget();
    e.superTarget(1, 2);
    e.superTarget(1, "2");

    Unknown u = new Unknown();
    // don't create dummy for methods of unknown classes!
    u.unknownTarget();
  }
}