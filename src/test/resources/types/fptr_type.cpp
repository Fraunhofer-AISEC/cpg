void ((*global_one_param)(int));
int (*global_two_param)(int, unsigned long);

int main() {
  void ((*local_one_param)(int));
  int (*local_two_param)(int, unsigned long);
}
