void functionTarget() {}
void functionTarget(int param1, int param2) {}
void functionTarget(int param1, const char* param2) {}

class SuperClass {
  public:
    void superTarget() {}
    void superTarget(int param1, int param2) {}
    void superTarget(int param1, const char* param2) {}
    virtual void overridingTarget() {}
};

class External: public SuperClass {
  public:
    void externalTarget() {}
    void externalTarget(int param1, int param2) {}
    void externalTarget(int param1, const char* param2) {}
    void overridingTarget() override {}
};

class Invocation {
  public:
    void invoke() {}
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

      External e;
      e.externalTarget();
      e.externalTarget(1, 2);
      e.externalTarget(1, "2");
      // inferred
      e.externalTarget(1, 2, 3);

      e.superTarget();
      e.superTarget(1, 2);
      e.superTarget(1, "2");

      SuperClass *s = new External();
      s->overridingTarget();

      Unknown u;
      // don't create inference for methods of unknown classes!
      u.unknownTarget();
    }
};

void main() {
  // Invocation of method from function (main function not in a class)
  Invocation i;
  i.invoke();
  // inferred
  functionTarget(1, 2, 3);
}