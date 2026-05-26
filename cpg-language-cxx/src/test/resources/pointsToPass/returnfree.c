#include<stdio.h>
#include<stdlib.h>

char* returnfreed() {
  char* ret = malloc(16);
  
  if (1 == 2) {
	return NULL;
  }

  
  free(ret);
  
  return ret;
}

int main() {
  char* later_assigned_str = NULL;
  char* directly_assigned_str = returnfreed();

  printf("%s", later_assigned_str);
  later_assigned_str = returnfreed();
  printf("%s %c", later_assigned_str, *later_assigned_str);
  printf("%s", *directly_assigned_str);
}
