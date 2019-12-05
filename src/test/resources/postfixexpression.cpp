#include <stdio.h>
#include <iostream>

int main(int a) {
  printf("text"); // function call

  a++; // unary operation
  a--; // unary operation

  std::string test = "a"; // just here to make it compile in clang

  test.c_str(); // member call
}
