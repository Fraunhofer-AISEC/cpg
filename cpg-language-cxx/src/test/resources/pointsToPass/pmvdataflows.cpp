undefined8 test_PMV_flows(void *param_1) {
  long lVar3;
  long vVar7;
  void *pvVar7 = &vVar7;
  //lVar3 = *(long *)(param_1 + 2); TODO: this only works in the testServer as there it is treated as pointer expression
  lVar3 = *(long *)param_1;

  int i = (void *)memcpy(pvVar7,lVar3,__size);

  printf("%d\n", vVar7);
}
