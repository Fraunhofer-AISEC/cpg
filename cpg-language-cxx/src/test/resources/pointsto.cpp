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


typedef struct test {
  int a;
  int b;
} S;

int structs() {
  S s;
  S t;
  S* p=&s;

  s.a=1;
  s.b=2;
  printf("%d %d\n", s.a, s.b);

  printf("%d %d\n", p->a, p->b);
  p->a=3;
  p->b=4;

  printf("%d %d\n", p->a, p->b);
}

int arrays() {
   int n[5];   
   int i, j;
   
   printf("%d\n", n[0]);
   n[0] = 1;
   printf("%d\n", n[0]);
        
   for(i = 0; i < 5; i++){
      n[i] = i + 100;
   }
   
   for(j = 0; j < 5; j++){
      printf("n[%d] = %d\n", j, n[j]);
   }
   return 0;

} 




/*long memcpy_s(long *dst, int dstlen, long *src, int srclen){ 
  memcpy(src,dst,dstlen);
}*/

int main() {
  long a=0;
  long b=1;
  long c=2;
  long caddr=(long)&c;
  long d=3;
  long e=4;
  long f=5;
  long g=6;
  long h=7;

  long* pa=&a;
  long* pb=&b;
  long* pc=&c;
  long* pd=&d;
  long* pe=&e;
  long* pf=&f;
  long* pg=&g;
  long* ph=&h;

  printf("a : %d, %p; b : %d, %p, c : %d, %p, d : %d, %p, e : %d, %p, f : %d, %p\n", a, &a, b, &b, c, &c, d, &d, e, &e, f, &f);
  printf("pa: %d, %p; pb: %d, %p, pc: %d, %p, pd: %d, %p, pe: %d, %p, pf: %d, %p\n", *pa, pa, *pb, pb, *pc, pc, *pd, pd, *pe, pe, *pf, pf);

  // Copy the data at pa.memoryValue (AKA &a) to the address at pb.memoryValue (AKA &b)
  memcpy(pb,pa,sizeof(a));

  // Copy the data at pa.memoryValue (AKA &a) to the address at caddr.memoryValue (AKA &c)
  memcpy((void *)caddr,pa,sizeof(pa));

  // Copy the data at pa.memoryValue (AKA &a) to the address at &d.memoryValue (AKA d.memoryAddress) 
  memcpy(&d,pa,sizeof(a));
  
  // Copy the data at &a (AKA a.memoryAddress) to the address at pe.memoryValue (AKA e.memoryAddress) 
  memcpy(pe,&a,sizeof(a));

  // Copy the data at &pa (AKA pa.memoryAddress, which contains &a) to the address at &pf (which so far contained f.memoryAddress) 
  // Simplified: Set the value of pf to &a
  memcpy(&pf,&pa,sizeof(a));


  printf("a : %d, %p; b : %d, %p, c : %d, %p, d : %d, %p, e : %d, %p, f : %d, %p\n", a, &a, b, &b, c, &c, d, &d, e, &e, f, &f);
  printf("pa: %d, %p; pb: %d, %p, pc: %d, %p, pd: %d, %p, pe: %d, %p, pf: %d, %p\n", *pa, pa, *pb, pb, *pc, pc, *pd, pd, *pe, pe, *pf, pf);
}
