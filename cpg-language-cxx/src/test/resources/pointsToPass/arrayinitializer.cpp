typedef struct test {                                                                                                                                   
  int a;
  int b;
} S;

int test_array_initializer() {
  int numbers[] = {1, 2, 3};                                                                                                                              
  const foo_t unknown_assigns[] = {
      .one = 1,
      .two = 2
  }
  S known_assigns = { .b = 4, .a = 3 }
  
  printf("%d %d %d\n", numbers[0], unknown_assigns[0], known_assigns[0]);
}
