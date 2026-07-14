#include<stdio.h>

void incp(int* p) {
  (*p)++;
}

void exec_func_ptr(void (*func) (int *), int* p){
  func(p);
}

int main() {
   int i=0;
   int* p=&i;
   void (*funcPtr) (int *) = incp;
     
   printf("%d\n", i);
   funcPtr(p);
   printf("%d\n", i);
   exec_func_ptr(funcPtr, p);
   printf("%d\n", i);
}
