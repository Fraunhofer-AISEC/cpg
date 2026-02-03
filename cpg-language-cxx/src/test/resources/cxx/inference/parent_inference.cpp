/*
namespace ABC {
struct A {
    A();
    void foo();
};
}
*/

struct A : ABC::A {
    A() {
        foo();
        bar();
    }
    void bar() {

    }
};
