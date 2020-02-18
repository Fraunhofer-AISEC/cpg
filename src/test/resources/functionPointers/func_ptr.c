void target() {}
void target(int param) {}

int main() {
  void (*no_param)() = &target;
  void (*single_param)(int) = &target;
  void (*no_param_unused)() = &target;
  void (*single_param_unused)(int) = &target;
  (*no_param)();
  (*single_param)(42);
}