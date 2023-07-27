int assign1() {
  int i;
  short s1 = 1;
  short s2 = i;

  return 0;
}

int assign2() {
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
  assign1();
  assign2();
  assign3();
  assign_auto();

  return 0;
}
