int someFunction(int c) {
  return 2;
}

int main() {
  int a;

  a = 2;

  int b;

  a = b;

  a = someFunction(b);
}
