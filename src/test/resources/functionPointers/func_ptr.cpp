class A {
  public:
    void target() {}
    void target(int param) {}
};

int main() {
  A a;

  void (A::* no_param) () = &A::target;
  void (A::* single_param) (int) = &A::target;
  void (A::* no_param_unused) () = &A::target;
  void (A::* single_param_unused) (int) = &A::target;
  void (A::* no_param_unknown) () = &A::fun;
  void (A::* single_param_unknown) (int) = &A::fun;
  (a.*no_param)();
  (a.*single_param)(42);
  (a.*no_param_unknown)();
  (a.*no_param_unknown)();
  (a.*single_param_unknown)(42);
  (a.*single_param_unknown)(43);
}