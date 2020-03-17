class A {
  public:
    void target() {}
    void target(int param) {}
    void (A::* no_param_field) () = &A::target;
    void (A::* single_param_field) (int) = &A::target;
    void (A::* no_param_unused_field) () = &A::target;
    void (A::* single_param_unused_field) (int) = &A::target;
    void (A::* no_param_unknown_field) () = &A::fun;
    void (A::* single_param_unknown_field) (int) = &A::fun;

    void (A::* no_param_field_uninitialized) ();
    void (A::* single_param_field_uninitialized) (int);
    void (A::* no_param_unused_field_uninitialized) ();
    void (A::* single_param_unused_field_uninitialized) (int);
    void (A::* no_param_unknown_field_uninitialized) ();
    void (A::* single_param_unknown_field_uninitialized) (int);
};

int main() {
  A a;

  void (A::* no_param) () = &A::target;
  void (A::* single_param) (int) = &A::target;
  void (A::* no_param_unused) () = &A::target;
  void (A::* single_param_unused) (int) = &A::target;
  void (A::* no_param_unknown) () = &A::fun;
  void (A::* single_param_unknown) (int) = &A::fun;

  void (A::* no_param_uninitialized) ();
  void (A::* single_param_uninitialized) (int);
  void (A::* no_param_unused_uninitialized) ();
  void (A::* single_param_unused_uninitialized) (int);
  void (A::* no_param_unknown_uninitialized) ();
  void (A::* single_param_unknown_uninitialized) (int);

  no_param_uninitialized = &A::target;
  single_param_uninitialized = &A::target;
  no_param_unused_uninitialized = &A::target;
  single_param_unused_uninitialized = &A::target;
  no_param_unknown_uninitialized = &A::fun;
  single_param_unknown_uninitialized = &A::fun;

  a.no_param_field_uninitialized = &A::target;
  a.single_param_field_uninitialized = &A::target;
  a.no_param_unused_field_uninitialized = &A::target;
  a.single_param_unused_field_uninitialized = &A::target;
  a.no_param_unknown_field_uninitialized = &A::fun;
  a.single_param_unknown_field_uninitialized = &A::fun;

  // normal pointers
  (a.*no_param)();
  (a.*single_param)(42);
  (a.*no_param_unknown)();
  (a.*no_param_unknown)();
  (a.*single_param_unknown)(42);
  (a.*single_param_unknown)(43);

  // normal pointers but initialized later on
  (a.*no_param_uninitialized)();
  (a.*single_param_uninitialized)(42);
  (a.*no_param_unknown_uninitialized)();
  (a.*no_param_unknown_uninitialized)();
  (a.*single_param_unknown_uninitialized)(42);
  (a.*single_param_unknown_uninitialized)(43);

  // pointers stored as fields
  (a.*a.no_param_field)();
  (a.*a.single_param_field)(42);
  (a.*a.no_param_unknown_field)();
  (a.*a.no_param_unknown_field)();
  (a.*a.single_param_unknown_field)(42);
  (a.*a.single_param_unknown_field)(43);

  // pointers stored as fields but initialized later on
  (a.*a.no_param_field_uninitialized)();
  (a.*a.single_param_field_uninitialized)(42);
  (a.*a.no_param_unknown_field_uninitialized)();
  (a.*a.no_param_unknown_field_uninitialized)();
  (a.*a.single_param_unknown_field_uninitialized)(42);
  (a.*a.single_param_unknown_field_uninitialized)(43);
}