#include <stdio.h>

int third(float b) {
  printf("An overloaded function which is never called.\n");
  return 1;
}

int third(int a, int b) {
  printf("third\n");

  return 1;
}

int first() {
  printf("first\n");

  return 2;
}

int second() {
  printf("second\n");

  return 3;
}

int fourth() {
  printf("fourth\n");

  return 4;
}

int main() {
  third(first(), second());

  if(true) {
    fourth();
  }
}