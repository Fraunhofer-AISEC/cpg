void ((*global_no_param_void))(void);
void (*global_no_param)();
void ((*global_one_param)(int));
int (*global_two_param)(int, unsigned long);

int main() {
  void ((*local_one_param)(int));
  int (*local_two_param)(int, unsigned long);
}
