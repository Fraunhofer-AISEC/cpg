#include "include.h"
#include "another-include.h"

int main() {
  SomeClass* c = new SomeClass();
}

SomeClass::SomeClass() {
}

int SomeClass::DoSomething() { return 1; }
