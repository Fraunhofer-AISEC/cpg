char* returnfreed() {
  char* ret = malloc(16);
  
  if (1 == 2) {
	return NULL;
  }

  
  free(ret);
  
  return ret;
}

int main() {
  char* str = NULL;

  printf("%s", str);
  str = returnfreed();
  printf("%s %c", str, *str);
}
