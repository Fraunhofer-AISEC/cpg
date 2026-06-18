#include<stdio.h>

int func1();

int func3(int *p){
  printf("%d\n", *p);
  func1(p);
}

int func2(int* p) {
  for (int j=0; j<10; j++) {
    (*p)++;
  }
  func3(p);
}

int func1(int* p) {
  if (*p < 50) {
    (*p)++;
    func2(p);
  }
}

int main() {
  int i=0;
  int* p=&i;
  func1(p);
  return 0;
}
