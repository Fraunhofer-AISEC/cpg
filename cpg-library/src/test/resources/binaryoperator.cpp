#include <iostream>

int main() {
  int a;
  int b;

  a = b * 2;
  a = 1 * 1;

  std::string* notMultiplication = 0 * 0; // just to check if the parser correctly deduces that this is a variable decl with a pointer

  a = 2 >> 2;

  return a;
}