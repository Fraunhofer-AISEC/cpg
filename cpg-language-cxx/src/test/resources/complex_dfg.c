#include<stdio.h>

int func1(int* p);
int func2(int* p);
int func3(int* p);

int func3(int *p){
  printf("%d\n", *p);
  (*p)++;
  func2(p);
}

int func2(int* p) {
  for (int j=0; j<10; j++) {
    func3(p);
  }
}

int func1(int* p) {
  (*p)++;
  func2(p);
}

int main() {
  int i=0;
  int* p=&i;
  func1(p);
  printf("%d\n", i);
  return 0;
}