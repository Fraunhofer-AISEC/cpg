void target() {}

int main() {
  // Declares a function pointer with an initializer set to a function
  void (*no_param)() = &target;
  // Declares a function pointer without an initial value
  void (*no_param_uninitialized) ();

  // Sets the second function pointer to the function
  no_param_uninitialized = &target;

  // The following syntax is the "normal" syntax that is usually
  // used by de-referencing the pointer and calling it
  (*no_param)();
  (*no_param_uninitialized)();

  // However, C/C++ also allows us to directly call the function pointer
  // with a syntax that is indistinguishable from a regular function call
  no_param();
  no_param_uninitialized();

  // We can of course also just directly call our target function
  target();
}
