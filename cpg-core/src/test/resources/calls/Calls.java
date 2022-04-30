public class Calls extends SuperClass {

  private void innerTarget() {}
  private void innerTarget(int param1, int param2) {}
  private void innerTarget(int param1, String param2) {}

  public void someFunction() {
    innerTarget();
    innerTarget(1, 2);
    innerTarget(1, "2");
    // inferred
    innerTarget(1, 2, 3);

    superTarget();
    superTarget(1, 2);
    superTarget(1, "2");
    // inferred
    superTarget(1, 2, 3);

    External e = new External();
    e.externalTarget();
    e.externalTarget(1, 2);
    e.externalTarget(1, "2");
    // inferred
    e.externalTarget(1, 2, 3);

    e.superTarget();
    e.superTarget(1, 2);
    e.superTarget(1, "2");

    SuperClass s = new External();
    s.overridingTarget();

    Unknown u = new Unknown();
    // don't create inference for methods of unknown classes!
    u.unknownTarget();
  }
}