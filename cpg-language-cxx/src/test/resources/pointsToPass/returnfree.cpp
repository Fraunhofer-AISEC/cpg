char* returnfreed() {
  char* ret = malloc(16);
  
  free(ret);
  
  return ret;
}

int main() {
  char* str = NULL;

  printf("%s", str);
  str = returnfreed();
  printf("%s", str);
}
