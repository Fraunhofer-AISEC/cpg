namespace ABC {
  // The FQN of this struct should be ABC:A
  struct A {};

  void foo();
  void bar(ABC::A a);
};

// This is a function (not a method!) within the namespace ABC
void ABC::foo() {
  // This "A" type is actually ABC:A because the default scope of this function is the namespace ABC
  A a;
  bar(a);
}

void ABC::bar(ABC::A a) {
}

int main() {
  ABC::foo();
  return 0;
}