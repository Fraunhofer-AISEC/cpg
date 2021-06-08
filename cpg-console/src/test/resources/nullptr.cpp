int main() {
    SomeClass* a = new SomeClass();
    a->doSomething();

    a = nullptr;

    a->doSomethingElse();
}