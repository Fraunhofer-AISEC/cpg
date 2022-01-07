class A {
public:
    void foo(int a);
};

class B {
public:
    void foo(int a);
};

void B::foo(int a) {
}

int main() {
    A a;
    B b;

    a.foo(1);
    b.foo(2);

    foo();

    C c;
    c.foo(3);
}