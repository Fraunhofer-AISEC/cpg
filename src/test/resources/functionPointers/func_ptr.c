void target() {}
void target(int param) {}

int main() {
  void (*no_param)() = &target;
  void (*single_param)(int) = &target;
  void (*no_param_unused)() = &target;
  void (*single_param_unused)(int) = &target;
  void (*no_param_unknown)() = &fun;
  void (*single_param_unknown)(int) = &fun;
  (*no_param)();
  (*single_param)(42);
  (*no_param_unknown)();
  (*no_param_unknown)();
  (*single_param_unknown)(42);
  (*single_param_unknown)(43);
}