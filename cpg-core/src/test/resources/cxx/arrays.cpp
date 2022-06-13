int main() {
  int x[] = { 1, 2, 3 };

  x[0]; // yes this will produce a warning but is still valid and easier to parse for the test

  char (*a)[4]; // this is a pointer to an array of 4 chars. The type is char (*)[4]
  char* b[4]; // this is an array of 4 pointers to a char. The type is char *[4]
}