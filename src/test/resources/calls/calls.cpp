void functionTarget() {}
void functionTarget(int param1, int param2) {}
void functionTarget(int param1, const char* param2) {}

class SuperClass {
  public:
    void superTarget() {}
    void superTarget(int param1, int param2) {}
    void superTarget(int param1, const char* param2) {}
};

class External: public SuperClass {
  public:
    void externalTarget() {}
    void externalTarget(int param1, int param2) {}
    void externalTarget(int param1, const char* param2) {}
};

class Calls: SuperClass {
  private:
    void innerTarget() {}
    void innerTarget(int param1, int param2) {}
    void innerTarget(int param1, const char* param2) {}
  public:
    void main() {
      functionTarget();
      functionTarget(1, 2);
      functionTarget(1, "2");
      // dummy
      functionTarget(1, 2, 3);

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

      External e;
      e.externalTarget();
      e.externalTarget(1, 2);
      e.externalTarget(1, "2");
      // dummy
      e.externalTarget(1, 2, 3);

      e.superTarget();
      e.superTarget(1, 2);
      e.superTarget(1, "2");
      // dummy
      e.superTarget(1, 2, 3);

      Unknown u;
      // don't create dummy for methods of unknown classes!
      u.unknownTarget();
    }
};