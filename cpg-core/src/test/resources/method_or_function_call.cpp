struct A {
    void foo(int i) {
    }
};

struct B {
    void (*bar)(int);
};

void bar(int i) {
}

int main() {
    A a;
    B b;
    b.bar = &bar;

    // foo is a method
    (a.foo)(1);
    a.foo(2);

    // bar is a function
    (b.bar)(3);
    (*b.bar)(3);

    return 0;
}
