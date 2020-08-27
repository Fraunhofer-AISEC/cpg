// declaration only
void function0(int arg0);

// declaration and definition
int function1(int arg0, std::string arg1, SomeType* arg2, AnotherType &arg3) {

}

// body for the function declared earlier. should connect the body to the original declaration
void function0(int arg0) {
  callSomething();
  // no explicit return
}

void* function2() {
  return NULL;
}

class UnknownType* function3();

#define SOME_MACRO(x) x
void function4(int a = SOME_MACRO(1));

// void parameter without name
void function5(void);
