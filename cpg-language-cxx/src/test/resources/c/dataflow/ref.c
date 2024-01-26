#include <stdio.h>

void doSomething(int c) {}

int main() {
  int var = 1;

  doSomething(var);

  var = 3;

  doSomething(var);
}