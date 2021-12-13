void target() {}
void target(int param) {}

int main() {
  void (*no_param)() = &target;
  void (*single_param)(int) = &target;
  void (*no_param_unused)() = &target;
  void (*single_param_unused)(int) = &target;
  void (*no_param_unknown)() = &fun;
  void (*single_param_unknown)(int) = &fun;

  void (*no_param_uninitialized) ();
  void (*single_param_uninitialized) (int);
  void (*no_param_unused_uninitialized) ();
  void (*single_param_unused_uninitialized) (int);
  void (*no_param_unknown_uninitialized) ();
  void (*single_param_unknown_uninitialized) (int);

  no_param_uninitialized = &target;
  single_param_uninitialized = &target;
  no_param_unused_uninitialized = &target;
  single_param_unused_uninitialized = &target;
  no_param_unknown_uninitialized = &fun;
  single_param_unknown_uninitialized = &fun;

  // normal pointers
  (*no_param)();
  (*single_param)(42);
  (*no_param_unknown)();
  (*no_param_unknown)();
  (*single_param_unknown)(42);
  (*single_param_unknown)(43);

  // normal pointers but initialized later on
  (*no_param_uninitialized)();
  (*single_param_uninitialized)(42);
  (*no_param_unknown_uninitialized)();
  (*no_param_unknown_uninitialized)();
  (*single_param_unknown_uninitialized)(42);
  (*single_param_unknown_uninitialized)(43);

  // calls without dedicated function pointer syntax
  no_param();
  single_param(42);
  no_param_unknown();
  no_param_unknown();
  single_param_unknown(42);
  single_param_unknown(43);
  no_param_uninitialized();
  single_param_uninitialized(42);
  no_param_unknown_uninitialized();
  no_param_unknown_uninitialized();
  single_param_unknown_uninitialized(42);
  single_param_unknown_uninitialized(43);
}