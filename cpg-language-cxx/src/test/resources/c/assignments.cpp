int assign_initializer_only() {
  int i;
  short s1 = 1;
  short s2 = i;

  return 0;
}

int assign_after_initializer() {
  int i;
  short s1 = 1;
  short s2 = 1;
  s2 = i;

  return 0;
}

int assign3() {
  int i;
  short s1;
  short s2 = 1;
  s1 = i;
  s1 = s2;

  return 0;
}

int assign_auto() {
  int i;
  auto s1 = i;
  auto s2 = s1;

  return 0;
}

int main() {
  assign_initializer_only();
  assign_after_initializer();
  assign3();
  assign_auto();

  return 0;
}
