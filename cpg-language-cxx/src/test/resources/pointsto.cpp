#include <stdio.h>

int main() {
  int i=0;
  int j=1;
  int* a=&i;
  int* b=a;
  printf("%d\n", i);
  i=2;
  printf("%d\n", i);
  i = i + 1;
  printf("%d\n", *a);
  i++;
  printf("%d\n", *a);
  a=&j;
  printf("%d\n", *a);
  *a=3;
  printf("%d\n", *b);
}

int conditions() {
  int i=0;                                                                                                                                            
  int j=1;                                                                      
  int* a;                                                                                               
  if ( 1 == 1 ) {
    a = &i;
    printf("%d\n", *a);
  } else {
    a = &j;
    printf("%d\n", *a);
  }
  printf("%d\n", *a);

  for (int x=0; x<10; x++) {
    i++;
  }
  printf("%d\n", *a);
}


/*int memcpy() {
  char a[3]="aa";
  char b[3]="bb";
  char c[3]="cc";

  char* pa=a;

  char* noalias1=b;
  char* noalias2=b;
  char* noalias3=b;
  char* noalias4=b;

  char* alias1=c;
  char* alias2=c; 
  char* alias3=c;
  char* alias4=c; 
  
//  printf("%s, %s, %s, %p, %p, %p\n", pa, pb, pc, pa, pb, pc);

  // No aliases
  memcpy(noalias1,pa,sizeof(pa));
  memcpy_verw_s(noalias2, sizeof(pa), pa, sizeof(pa));
  memcpy_s(noalias3, sizeof(pa), &pa, sizeof(pa));
  memcpy_verw(&noalias4, pa, sizeof(pa));

  // Create aliases
  memcpy(&alias1, &pa, sizeof(pa));
  printf("%s\n", pa);
  memcpy_verw_s(&alias2, sizeof(pa), &pa, sizeof(pa));
  memcpy_verw(&alias3, &pa, sizeof(pa));
  memcpy_s(&alias4, sizeof(pa), &pa, sizeof(pa));
  
  printf("%c %c %c %c\n", *alias1, *alias2, *alias3, *alias4);
}*/

int arrays() {
   int n[5];   
   int i, j;
          
   for(i = 0; i < 5; i++){
      n[i] = i + 100;
   }
   
   for(j = 0; j < 5; j++){
      printf("n[%d] = %d\n", j, n[j]);
   }
   return 0;

} 


/*int ghidra(undefined8 *param_1) {
  undefined8 uVar1;   
  undefined8 *local_10;
                                                                                                                                   
  uVar1 = DAT_0011b1c8;                                                                                                                                  
  *param_1 = CONCAT71(key._1_7_,(char)key);                                                                                                            
  param_1[1] = uVar1;
  
  *local_10[0] = key;   
  local_10[1] = uVar1;
}*/
