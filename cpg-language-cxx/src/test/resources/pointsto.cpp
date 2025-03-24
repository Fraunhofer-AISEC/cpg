#include <stdio.h>

int basics() {
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

int testmemcpy() {
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
  memcpy((long *)caddr,pa,sizeof(a));

  // Copy the data at pa.memoryValue (AKA &a) to the address at &d.memoryValue (AKA d.memoryAddress) 
  memcpy(&d,pa,sizeof(a));
  
  // Copy the data at &a (AKA a.memoryAddress) to the address at pe.memoryValue (AKA e.memoryAddress) 
  memcpy(pe,&a,sizeof(a));

  // Copy the data at &pa (AKA pa.memoryAddress, which contains &a) to the address at &pf (which so far contained f.memoryAddress) 
  // Simplified: Set the value of pf to &a
  memcpy(&pf,&pa,sizeof(a));


  printf("a : %d, %p; b : %d, %p, c : %d, %p, d : %d, %p, e : %d, %p, f : %d, %p\n", a, &a, b, &b, c, &c, d, &d, e, &e, f, &f);
  printf("pa: %d, %p; pb: %d, %p, pc: %d, %p, pd: %d, %p, pe: %d, %p, pf: %d, %p\n", *pa, pa, *pb, pb, *pc, pc, *pd, pd, *pe, pe, *pf, pf);
i}

int pointertopointer(){

   int a = 10;
   int *b = &a;
   int **c = &b;

   printf("a: %d \nAddress of 'a': %d \nValue at a: %d\n\n", a, b, *b);
   printf("b: %d \nPointer to 'b' is c: %d \nValue at b: %d\n", b, c, *c);
   printf("Value of 'a' from 'c': %d", **c);

   return 0;
}

undefined4 sgx_ecall_key_to_out(long param_1)

{
  int iVar1;
  undefined4 uVar2;
  long local_30;
  void *local_28;
  long local_20;
  long local_18;
  size_t local_10;
  
  if ((param_1 == 0) || (iVar1 = sgx_is_outside_enclave(param_1,8), iVar1 == 0)) {
    return 2;
  }
  local_20 = param_1;
  iVar1 = memcpy_s(&local_30,8,param_1,8);
  if (iVar1 != 0) {
    return 1;
  }
  uVar2 = 0;
  local_18 = local_30;
  local_10 = 0x10;
  local_28 = (void *)0x0;
  if ((local_30 != 0) && (iVar1 = sgx_is_outside_enclave(local_30,0x10), iVar1 == 0)) {
    return 2;
  }
  if ((local_18 != 0) && (local_10 != 0)) {
  local_28 = dlmalloc(local_10);
    if (local_28 == (void *)0x0) {
      uVar2 = 3;
      goto LAB_001011ce;
    }
    memset(local_28,0,local_10);
  }
  printf("%d\n", *local_28);
  ecall_key_to_out(local_28);
  printf("%d %d\n", *local_28, local_28[1]);
  if ((local_28 != (void *)0x0) && /*printf("%d", *local_28) &&*/ 
     (iVar1 = memcpy_verw_s(local_18,local_10,local_28,local_10), iVar1 != 0)) {
    uVar2 = 1;
  }
LAB_001011ce:
  if (local_28 != (void *)0x0) {
    free(local_28);
  }
  printf("%d %d\n", *local_18, *param_1); return uVar2;
}

void ecall_key_to_out(/*undefined8*/ void *param_1)
{
  undefined8 uVar1;
  
  if ((char)key == '\0') {
    derive_secret_key();
  }
  uVar1 = DAT_0011b1c8;
  *param_1 = CONCAT71(key._1_7_,(char)key);
  param_1[1] = uVar1;
  return;
}

int inc(int i) {                                                                  
  i=i+1;
  return i;       
}  

void incp(int* p) {
  *p=*p+1;//(*p)++;
}

void changep(int* p1, int* p2) {
  *p1=*p2;
}

void changep2(int* p1, int* p2) {
  *p1=p2;
}

int testFunctionSummaries() {                                                      
  int i=0;                                                                                                                                                
  int j=3; 
  int* p=&i;                                                                                                                                             

  printf("i: %d j: %d *p: %d p: %p\n", i, j, *p, p);

  i=inc(i);
  printf("i: %d j: %d *p: %d p: %p\n", i, j, *p, p);

  incp(p);
  printf("i: %d j: %d *p: %d p: %p\n", i, j, *p, p);

  changep(p, &j);
  printf("i: %d j: %d *p: %d p: %p\n", i, j, *p, p);

  incp(p);
  printf("i: %d j: %d *p: %d p: %p\n", i, j, *p, p);

  i = unknownFunc(i, p);
}

void changepointer(int **p, int newint, int *newp) {
   *p = newp;
   **p = newint;
 //   *p = 3;
 //   **p = 4;
}

// Sample output: 
// p2p: 4adb5408, &p_oldval: 4adb5408, &p_newval: 4adb53f8, *p2p: 4adb5414, &oldval: 4adb5414, &newval: 4adb5404, **p2p: 1
// p2p: 4adb5408, &p_oldval: 4adb5408, &p_newval: 4adb53f8, *p2p: 4adb5404, &oldval: 4adb5414, &newval: 4adb5404, **p2p: 2
int testFunctionSummaries2() {
   int oldval = 1;                                                                                                                                    
   int* p_oldval = &oldval;
   int** p2p = &p_oldval;
   int newval = 2;
   int* p_newval = &newval;

   printf("p2p: %x, &p_oldval: %x, &p_newval: %x, *p2p: %x, &oldval: %x, &newval: %x, **p2p: %d\n", p2p, &p_oldval, &p_newval, *p2p, &oldval, &newval, **p2p);
   changepointer(p2p, newval, p_newval);
   printf("p2p: %x, &p_oldval: %x, &p_newval: %x, *p2p: %x, &oldval: %x, &newval: %x, **p2p: %d\n", p2p, &p_oldval, &p_newval, *p2p, &oldval, &newval, **p2p);
}

char key[16]; // 0x1b1c0
/* WARNING: Unknown calling convention -- yet parameter storage is locked */
/* derive_secret_key() */

undefined8 derive_secret_key(void)

{
  int iVar1;
  undefined8 uVar2;
  long in_FS_OFFSET;
  int local_3d0;
  undefined8 local_3c8;
  undefined8 local_3c0;
  undefined2 local_2c6;
  undefined2 local_2c4;
  undefined2 local_218;
  undefined2 local_216;
  undefined2 local_214;
  undefined8 local_210;
  undefined8 local_208;
  undefined8 local_200;
  undefined8 local_1f8;
  undefined8 local_1f0;
  undefined8 local_1e8;
  undefined8 local_1e0;
  undefined8 local_1d8;
  undefined4 local_1d0;
  undefined2 local_1cc;
  long local_10;
  
  local_10 = *(long *)(in_FS_OFFSET + 0x28);
  memset(&local_218,0,0x200);
  iVar1 = sgx_create_report(0,0,&local_3c8);
  if (iVar1 == 0) {
    local_218 = 4;
    local_216 = 2;
    local_210 = local_3c8;
    local_208 = local_3c0;
    local_214 = local_2c6;
    local_200 = 0xff0000000000000b;
    local_1f8 = 0;
    local_1d0 = 0xf0000000;
    local_1cc = local_2c4;
    local_1f0 = key_id;
    local_1e8 = DAT_0011b1e8;
    local_1e0 = DAT_0011b1f0;
    local_1d8 = DAT_0011b1f8;
    iVar1 = sgx_get_key(&local_218,&key);
    if ( iVar1 == 0 ) {
      printf("[DEBUG] Fetched key: ");
      for (local_3d0 = 0; local_3d0 < 0x10; local_3d0 = local_3d0 + 1) {
        printf("%02x",(ulong)*(byte *)((long)&key + (long)local_3d0));
      }
      printf("\n");
      uVar2 = 0;
    }
    else {
      printf("Failed to fetch key");
      uVar2 = 0xffffffff;
    }
  }
  else {
    uVar2 = 0xffffffff;
  }
  if (local_10 != *(long *)(in_FS_OFFSET + 0x28)) {
                    /* WARNING: Subroutine does not return */
    __stack_chk_fail();
  }
  return uVar2;
}


void ecall_key_to_usercheck(undefined8 *param_1)

{
  undefined8 uVar1;
  
  if ((char)key == '\0') {
    derive_secret_key();
  }
  uVar1 = DAT_0011b1c8;
  *param_1 = CONCAT71(key._1_7_,(char)key);
  param_1[1] = uVar1;
  return;
}


undefined4 sgx_ecall_key_to_usercheck(long pms)

{
  int iVar1;
  undefined8 _tmp_outptr;
  long local_18;
  undefined8 local_10;
  
  if ((pms != 0) && (iVar1 = sgx_is_outside_enclave(pms,8), iVar1 != 0)) {
    local_18 = pms;
    iVar1 = memcpy_s(&_tmp_outptr,8,pms,8);
    if (iVar1 != 0) {
      return 1;
    }
    local_10 = _tmp_outptr;
    ecall_key_to_usercheck(_tmp_outptr);
    return 0;
  }
  return 2;
}

int testCallingContexts() {
    int i=0;
    int j=1;
    int* p=&j;

    i=inc(i);

    printf("%d\n", i);

    i=inc(i);

    incp(p);

    printf("%d %d\n", j, *p);
  
    incp(p);

    j = 2;

    printf("%d %d\n", j, *p);

}


int testUnaryOp() {
  int i = 1;
  
  if ( 1 == 1 ) {
    i = -i;
  }
  printf("%d\n", i);

  i++;

  printf("%d\n", i);
}

int add(int* a, int b) {
  *a = *a + b;
}

int testShortFS() {
  int i=1;
  int j=2;
  int* pi = &i;
  int* pj = &j;

  add(pi, 0);

  printf("%d %d\n", i, j);

  add(pi, j);

  printf("%d %d\n", i, j);

  add(pi, *pj);

  printf("%d %d\n", i, j);

  set(pi);

  printf("%d %d\n", i, j);
}

int set(int* p) {
  *p = 3;
}
