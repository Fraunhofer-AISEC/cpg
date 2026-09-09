int printstuff(void** s){
  void* str = *s;
  printf("%s", *str);
}

int testPointerDerefDFs() {
  void* p=malloc();
  
  *p = 1;
  foo(p);

  free(p);
  printf("%d", *p);

  bar(p);

  printstuff(&p);
}

