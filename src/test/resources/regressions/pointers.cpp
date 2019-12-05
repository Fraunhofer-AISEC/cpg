#include <cstdio>

int main() {
  int ***x;
  int *y = **x;
  int **z = &y;
}