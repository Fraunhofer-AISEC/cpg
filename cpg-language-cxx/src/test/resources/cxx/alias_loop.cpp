namespace ABC {
    struct A {};
}

namespace ABC {
    // not sure why this is even possible, but somehow we can define this
    // type as itself in this partial namespace
    using A = ABC::A;

    void test() {
        A a;
    }
}