#include<stdio.h>

void incp(int* p) {
  (*p)++;
}

int main() {
   int i=0;
   int* p=&i;
   void (*funcPtr) (int *) = incp;
     
   printf("%d\n", i);
   funcPtr(p);
   printf("%d\n", i);
}
